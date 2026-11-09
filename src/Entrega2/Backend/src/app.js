const express = require('express');
const path = require('node:path');
const { randomBytes, createHash } = require('node:crypto');
const QRCode = require('qrcode');
const { createClient: supabaseClient } = require('@supabase/supabase-js');

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
const ERRORS = {
  INVALID_INPUT: [400, 'Confira os dados enviados.'], INVALID_QR: [400, 'QR Code inválido.'],
  INVALID_ANSWERS: [400, 'Responda todas as perguntas com alternativas válidas.'],
  UNAUTHENTICATED: [401, 'Entre novamente na sua conta.'], FORBIDDEN: [403, 'Acesso não autorizado.'],
  NOT_ENROLLED: [403, 'Você não está inscrito neste encontro.'], NOT_FOUND: [404, 'Registro não encontrado.'],
  QR_NOT_ACTIVE: [409, 'O período de presença ainda não começou.'], QR_EXPIRED: [410, 'O QR Code expirou.'],
  COURSE_IMMUTABLE: [409, 'O curso de um encontro não pode ser alterado. Crie outro encontro.'],
  COURSE_FULL: [409, 'Não há vagas disponíveis.'], SERVICE_UNAVAILABLE: [503, 'Serviço indisponível. Tente novamente.'],
};
class ApiError extends Error { constructor(code) { super(code); this.code = code; } }
function invalid(condition) { if (condition) throw new ApiError('INVALID_INPUT'); }
function id(value) { invalid(typeof value !== 'string' || !UUID.test(value)); return value; }
function text(value, max, min = 1) { invalid(typeof value !== 'string' || value.trim().length < min || value.trim().length > max); return value.trim(); }
function object(value) { invalid(!value || typeof value !== 'object' || Array.isArray(value)); return value; }
function related(value) { return Array.isArray(value) ? value[0] : value; }
async function data(query) {
  const result = await query;
  if (result.error) throw new ApiError(ERRORS[result.error.message] ? result.error.message : 'SERVICE_UNAVAILABLE');
  return result.data;
}
function productionClient(token) {
  const url = process.env.SUPABASE_URL;
  const key = process.env.SUPABASE_ANON_KEY;
  if (!url || !key) throw new ApiError('SERVICE_UNAVAILABLE');
  return supabaseClient(url, key, {
    auth: { persistSession: false, autoRefreshToken: false, detectSessionInUrl: false },
    global: { ...(token ? { headers: { Authorization: `Bearer ${token}` } } : {}),
      fetch: (url, options) => fetch(url, { ...options, signal: AbortSignal.timeout(12000) }) },
  });
}
function sessionPayload(result) {
  if (result.error) throw new ApiError([400, 401, 403, 422].includes(result.error.status) ? 'UNAUTHENTICATED' : 'SERVICE_UNAVAILABLE');
  if (!result.data?.session || !result.data?.user) throw new ApiError('UNAUTHENTICATED');
  const { session, user } = result.data;
  return { access_token: session.access_token, refresh_token: session.refresh_token,
    expires_in: session.expires_in, expires_at: session.expires_at, user: { id: user.id } };
}

// Construção da aplicação separada do listener para testes HTTP locais.
function createApp({ createClient = productionClient } = {}) {
  const app = express();
  app.disable('x-powered-by');
  app.use((req, res, next) => {
    res.set('X-Content-Type-Options', 'nosniff');
    res.set('X-Frame-Options', 'DENY');
    res.set('Referrer-Policy', 'no-referrer');
    res.set('Cache-Control', 'no-store');
    res.set('Content-Security-Policy', "default-src 'self'; img-src 'self' data:; style-src 'self'; script-src 'self'; connect-src 'self'; frame-ancestors 'none'; base-uri 'self'; form-action 'self'");
    next();
  });
  app.use(express.json({ limit: '32kb' }));
  app.get('/health', (req, res) => res.json({ status: 'ok' }));
  app.get(/^\/admin$/, (req, res) => res.redirect(302, '/admin/'));
  app.use(express.static(path.join(__dirname, '../public'), { index: 'index.html', maxAge: 0 }));
  app.post('/api/auth/login', async (req, res) => {
    const b = object(req.body); const email = text(b.email, 254);
    invalid(typeof b.senha !== 'string' || b.senha.length < 1 || b.senha.length > 1024);
    const result = await createClient().auth.signInWithPassword({ email, password: b.senha });
    res.json({ ...sessionPayload(result), message: 'Login realizado com sucesso.' });
  });
  app.post('/api/auth/refresh', async (req, res) => {
    const token = text(object(req.body).refresh_token, 4096);
    res.json(sessionPayload(await createClient().auth.refreshSession({ refresh_token: token })));
  });
  app.post('/api/auth/cadastro', async (req, res) => {
    const b = object(req.body); const email = text(b.email, 254); const nome = text(b.nome, 120);
    invalid(typeof b.senha !== 'string' || b.senha.length < 8 || b.senha.length > 1024);
    const result = await createClient().auth.signUp({ email, password: b.senha, options: { data: { full_name: nome } } });
    if (result.error) throw new ApiError([400, 422].includes(result.error.status) ? 'INVALID_INPUT' : 'SERVICE_UNAVAILABLE');
    res.status(201).json({ message: 'Cadastro solicitado. Verifique seu e-mail se houver confirmação pendente.', user: result.data.user ? { id: result.data.user.id } : null });
  });
  app.use('/api', async (req, res, next) => {
    const header = req.headers.authorization;
    if (typeof header !== 'string' || !header.startsWith('Bearer ') || header.length > 8192) throw new ApiError('UNAUTHENTICATED');
    const token = header.slice(7).trim(); if (!token) throw new ApiError('UNAUTHENTICATED');
    const client = createClient(token); const result = await client.auth.getUser(token);
    if (result.error) throw new ApiError([400, 401, 403, 422].includes(result.error.status) ? 'UNAUTHENTICATED' : 'SERVICE_UNAVAILABLE');
    if (!result.data?.user?.id) throw new ApiError('UNAUTHENTICATED');
    req.user = result.data.user; req.userId = req.user.id; req.db = client; next();
  });
  app.get('/api/perfil', async (req, res) => {
    const p = await data(req.db.from('profiles').select('full_name,school').eq('id', req.userId).maybeSingle());
    res.json({ nome: p?.full_name || req.user.user_metadata?.full_name || 'Aluno', escola: p?.school || '', email: req.user.email || '' });
  });
  app.get('/api/card', async (req, res) => {
    const p = await data(req.db.from('profiles').select('full_name,school').eq('id', req.userId).maybeSingle());
    res.json({ id: req.userId, nome: p?.full_name || req.user.user_metadata?.full_name || 'Aluno', escola: p?.school || '', numero: `PE-${req.userId.slice(0, 8).toUpperCase()}` });
  });
  app.get('/api/inicio', async (req, res) => {
    const p = await data(req.db.from('profiles').select('full_name').eq('id', req.userId).maybeSingle());
    const e = await data(req.db.from('enrollments').select('courses(title)').eq('user_id', req.userId).eq('status', 'enrolled').limit(1));
    res.json({ nome: p?.full_name || 'Aluno', curso: related(e?.[0]?.courses)?.title || null });
  });
  const courseFields = 'id,title,description,location,workload,meetings_count,course_date,is_active,total_spots';
  app.get('/api/cursos', async (req, res) => {
    const courses = await data(req.db.from('courses').select(courseFields).eq('is_active', true).order('course_date').limit(500));
    const enrolled = await data(req.db.from('enrollments').select('course_id,status').eq('user_id', req.userId));
    const statuses = new Map((enrolled || []).map(e => [e.course_id, e.status]));
    res.json((courses || []).map(c => ({ ...c, status: statuses.get(c.id) === 'attended' ? 'Concluído' : statuses.get(c.id) === 'enrolled' ? 'Inscrito' : 'Disponível' })));
  });
  app.get('/api/cursos/:id', async (req, res) => {
    const c = await data(req.db.from('courses').select(courseFields).eq('id', id(req.params.id)).maybeSingle());
    if (!c) throw new ApiError('NOT_FOUND'); res.json(c);
  });
  app.post('/api/cursos/:id/inscricao', async (req, res) => {
    await data(req.db.rpc('e2_enroll', { p_course_id: id(req.params.id) }));
    res.json({ message: 'Inscrição confirmada.' });
  });
  app.get('/api/agenda', async (req, res) => {
    if (req.query.from !== undefined) invalid(typeof req.query.from !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(req.query.from) || !Number.isFinite(Date.parse(req.query.from)));
    const enrollments = await data(req.db.from('enrollments').select('course_id').eq('user_id', req.userId).in('status', ['enrolled', 'attended']));
    const courseIds = [...new Set((enrollments || []).map(e => e.course_id))];
    if (!courseIds.length) return res.json([]);
    let query = req.db.from('activities').select('id,course_id,title,activity_date,courses(title,location)').in('course_id', courseIds).eq('is_cancelled', false).order('activity_date').limit(1000);
    if (req.query.from) query = query.gte('activity_date', `${req.query.from}T00:00:00-03:00`);
    const activities = await data(query);
    const attendance = await data(req.db.from('attendances').select('activity_id').eq('user_id', req.userId));
    const attended = new Set((attendance || []).map(a => a.activity_id));
    res.json((activities || []).map(a => ({ id: a.id, activity_id: a.id, course_id: a.course_id,
      activity_date: new Intl.DateTimeFormat('en-CA', { timeZone: 'America/Sao_Paulo' }).format(new Date(a.activity_date)),
      start_time: new Intl.DateTimeFormat('en-GB', { timeZone: 'America/Sao_Paulo', hour: '2-digit', minute: '2-digit', second: '2-digit', hour12: false }).format(new Date(a.activity_date)),
      end_time: null, course_title: related(a.courses)?.title || '', title: a.title,
      location: related(a.courses)?.location || '', attended: attended.has(a.id) })));
  });
  app.post('/api/presencas/check-in', async (req, res) => {
    const b = object(req.body); id(b.activityId);
    invalid(typeof b.qrToken !== 'string' || !/^[A-Za-z0-9_-]{32,256}$/.test(b.qrToken));
    const result = await data(req.db.rpc('e2_check_in', { p_token: b.qrToken, p_activity_id: b.activityId }));
    res.status(result.created ? 201 : 200).json(result);
  });
  app.get('/api/presencas', async (req, res) => {
    const rows = await data(req.db.from('attendances').select('id,activity_id,scanned_at,activities(title,courses(title))').eq('user_id', req.userId).order('scanned_at', { ascending: false }).limit(1000));
    res.json((rows || []).map(r => ({ id: r.id, activity_id: r.activity_id, scanned_at: r.scanned_at, title: related(r.activities)?.title || '', course_title: related(related(r.activities)?.courses)?.title || '' })));
  });
  app.get('/api/certificados', async (req, res) => {
    const rows = await data(req.db.from('certificates').select('id,certificate_url,issued_at,courses(title,workload)').eq('user_id', req.userId).order('issued_at', { ascending: false }).limit(500));
    res.json((rows || []).map(r => ({ id: r.id, certificate_url: /^https:\/\//i.test(r.certificate_url || '') ? r.certificate_url : null,
      issued_at: r.issued_at, title: related(r.courses)?.title || 'Certificado', workload: related(r.courses)?.workload || 0 })));
  });
  app.get('/api/teste-perfil', async (req, res) => {
    const rows = await data(req.db.from('e2_profile_questions').select('id,enunciado,alternatives').order('position'));
    res.json({ titulo: 'Explore seus interesses', descricao: 'Questionário acadêmico de orientação. Não é diagnóstico psicológico.',
      perguntas: (rows || []).map(q => ({ id: q.id, enunciado: q.enunciado, alternativas: q.alternatives.map(a => ({ id: a.id, texto: a.texto })) })) });
  });
  app.get('/api/teste-perfil/resultados', async (req, res) => res.json(await data(req.db.from('e2_profile_results').select('id,classificacao,resumo,created_at').eq('user_id', req.userId).order('created_at', { ascending: false }).limit(100)) || []));
  app.post('/api/teste-perfil/resultados', async (req, res) => {
    const b = object(req.body); invalid(!Array.isArray(b.answers) || b.answers.length < 1 || b.answers.length > 100);
    for (const answer of b.answers) { object(answer); id(answer.questionId); text(answer.alternativeId, 50); }
    res.status(201).json(await data(req.db.rpc('e2_submit_profile', { p_answers: b.answers })));
  });
  app.get('/api/comunicados', async (req, res) => res.json(await data(req.db.from('e2_announcements').select('id,titulo,conteudo,created_at').order('created_at', { ascending: false }).limit(100)) || []));
  app.get('/api/mensagens', async (req, res) => {
    const rows = await data(req.db.from('e2_messages').select('id,user_id,remetente,conteudo,created_at').eq('user_id', req.userId).order('created_at', { ascending: false }).limit(500));
    res.json((rows || []).reverse());
  });
  app.post('/api/mensagens', async (req, res) => res.status(201).json(await data(req.db.rpc('e2_send_message', { p_content: text(object(req.body).conteudo, 2000), p_user_id: null }))));
  app.get('/api/notificacoes', async (req, res) => {
    const rows = await data(req.db.from('e2_notifications').select('id,titulo,conteudo,created_at').order('created_at', { ascending: false }).limit(100));
    const reads = await data(req.db.from('e2_notification_reads').select('notification_id').eq('user_id', req.userId));
    const ids = new Set((reads || []).map(r => r.notification_id)); res.json((rows || []).map(r => ({ ...r, lida: ids.has(r.id) })));
  });
  app.patch('/api/notificacoes/:id/lida', async (req, res) => res.json(await data(req.db.rpc('e2_mark_notification_read', { p_id: id(req.params.id) }))));

  app.use('/api/admin', async (req, res, next) => {
    if (await data(req.db.rpc('e2_is_admin')) !== true) throw new ApiError('FORBIDDEN'); next();
  });
  app.get('/api/admin/me', (req, res) => res.json({ admin: true }));
  app.get('/api/admin/alunos', async (req, res) => res.json(await data(req.db.from('profiles').select('id,full_name,school').order('full_name').limit(500)) || []));
  app.get('/api/admin/cursos', async (req, res) => res.json(await data(req.db.from('courses').select(courseFields).order('course_date').limit(500)) || []));
  app.get('/api/admin/encontros', async (req, res) => {
    const rows = await data(req.db.from('activities').select('id,title,activity_date,course_id,is_cancelled,courses(title)').order('activity_date').limit(1000));
    res.json((rows || []).map(({ courses, ...r }) => ({ ...r, course_title: related(courses)?.title || '' })));
  });
  function courseBody(body, partial) {
    const b = object(body); const out = {};
    for (const [key, max] of [['title', 160], ['description', 4000], ['location', 300]]) if (b[key] !== undefined || !partial) out[key] = text(b[key] ?? '', max, key === 'title' ? 1 : 0);
    for (const [key, max] of [['workload', 10000], ['meetings_count', 500], ['total_spots', 10000]]) if (b[key] !== undefined || !partial) { const value = b[key] ?? (key === 'total_spots' ? 30 : 1); invalid(!Number.isInteger(value) || value < 1 || value > max); out[key] = value; }
    if (b.course_date !== undefined || !partial) { invalid(typeof b.course_date !== 'string' || !Number.isFinite(Date.parse(b.course_date))); out.course_date = new Date(b.course_date).toISOString(); }
    if (b.is_active !== undefined) { invalid(typeof b.is_active !== 'boolean'); out.is_active = b.is_active; }
    invalid(!Object.keys(out).length); return out;
  }
  function activityBody(body, partial) {
    const b = object(body); const out = {};
    if (b.title !== undefined || !partial) out.title = text(b.title, 160);
    if (b.course_id !== undefined || !partial) out.course_id = id(b.course_id);
    if (b.activity_date !== undefined || !partial) { invalid(typeof b.activity_date !== 'string' || !Number.isFinite(Date.parse(b.activity_date))); out.activity_date = new Date(b.activity_date).toISOString(); }
    if (b.is_cancelled !== undefined) { invalid(typeof b.is_cancelled !== 'boolean'); out.is_cancelled = b.is_cancelled; }
    invalid(!Object.keys(out).length); return out;
  }
  for (const [route, table, parse] of [['cursos', 'courses', courseBody], ['encontros', 'activities', activityBody]]) {
    app.post(`/api/admin/${route}`, async (req, res) => res.status(201).json(await data(req.db.from(table).insert(parse(req.body, false)).select().single())));
    app.patch(`/api/admin/${route}/:id`, async (req, res) => {
      const recordId = id(req.params.id);
      const changes = parse(req.body, true);
      if (table === 'activities' && changes.course_id !== undefined) {
        const current = await data(req.db.from(table).select('*').eq('id', recordId).maybeSingle());
        if (!current) throw new ApiError('NOT_FOUND');
        if (changes.course_id.toLowerCase() !== current.course_id.toLowerCase()) throw new ApiError('COURSE_IMMUTABLE');
        // O vínculo nunca é regravado: evita reatribuir presenças e mantém clientes antigos compatíveis.
        delete changes.course_id;
        if (!Object.keys(changes).length) return res.json(current);
      }
      const row = await data(req.db.from(table).update(changes).eq('id', recordId).select().maybeSingle());
      if (!row) throw new ApiError('NOT_FOUND'); res.json(row);
    });
    app.delete(`/api/admin/${route}/:id`, async (req, res) => {
      const row = await data(req.db.from(table).update(table === 'courses' ? { is_active: false } : { is_cancelled: true }).eq('id', id(req.params.id)).select('id').maybeSingle());
      if (!row) throw new ApiError('NOT_FOUND'); res.json({ ok: true });
    });
  }
  app.post('/api/admin/encontros/:id/qr', async (req, res) => {
    const activityId = id(req.params.id); const minutes = object(req.body).durationMinutes ?? 60;
    invalid(!Number.isInteger(minutes) || minutes < 5 || minutes > 240);
    const qrToken = randomBytes(32).toString('base64url');
    const result = await data(req.db.rpc('e2_issue_qr', { p_activity_id: activityId, p_hash: createHash('sha256').update(qrToken).digest('hex'), p_minutes: minutes }));
    const qrPayload = JSON.stringify({ v: 1, activityId, qrToken });
    const qrDataUrl = await QRCode.toDataURL(qrPayload, { errorCorrectionLevel: 'M', width: 360, margin: 4 });
    res.status(201).json({ ...result, qrToken, qrPayload, qrDataUrl });
  });
  app.get('/api/admin/encontros/:id/presencas', async (req, res) => {
    const rows = await data(req.db.from('attendances').select('id,user_id,scanned_at').eq('activity_id', id(req.params.id)).order('scanned_at').limit(1000));
    if (!rows?.length) return res.json([]);
    const profiles = await data(req.db.from('profiles').select('id,full_name').in('id', [...new Set(rows.map(r => r.user_id))]));
    const names = new Map((profiles || []).map(p => [p.id, p.full_name])); res.json(rows.map(r => ({ ...r, nome: names.get(r.user_id) || 'Aluno' })));
  });
  app.get('/api/admin/mensagens', async (req, res) => {
    const rows = await data(req.db.from('e2_messages').select('id,user_id,remetente,conteudo,created_at').order('created_at', { ascending: false }).limit(500));
    if (!rows?.length) return res.json([]);
    const profiles = await data(req.db.from('profiles').select('id,full_name').in('id', [...new Set(rows.map(r => r.user_id))]));
    const names = new Map((profiles || []).map(p => [p.id, p.full_name]));
    res.json(rows.map(r => ({ ...r, nome: names.get(r.user_id) || 'Aluno' })));
  });
  app.post('/api/admin/mensagens', async (req, res) => res.status(201).json(await data(req.db.rpc('e2_send_message', { p_content: text(object(req.body).conteudo, 2000), p_user_id: id(req.body.userId) }))));
  app.post('/api/admin/comunicados', async (req, res) => res.status(201).json(await data(req.db.from('e2_announcements').insert({ titulo: text(object(req.body).titulo, 120), conteudo: text(req.body.conteudo, 4000) }).select().single())));
  app.post('/api/admin/certificados', async (req, res) => {
    const b = object(req.body); id(b.userId); id(b.courseId); invalid(b.confirmed !== true);
    let url; try { url = new URL(text(b.certificateUrl, 2000)); } catch { throw new ApiError('INVALID_INPUT'); }
    invalid(url.protocol !== 'https:' || !!url.username || !!url.password);
    res.status(201).json(await data(req.db.rpc('e2_register_certificate', { p_user_id: b.userId, p_course_id: b.courseId, p_url: url.href, p_confirmed: true })));
  });
  app.use('/api', (req, res) => res.status(404).json({ error: 'Rota não encontrada.', code: 'NOT_FOUND' }));
  app.use((error, req, res, next) => {
    const code = error instanceof ApiError ? error.code : error.type === 'entity.parse.failed' || error.type === 'entity.too.large' ? 'INVALID_INPUT' : 'SERVICE_UNAVAILABLE';
    const [status, message] = ERRORS[code] || ERRORS.SERVICE_UNAVAILABLE;
    res.status(status).json({ error: message, code });
  });
  return app;
}

// O Vercel detecta este arquivo como entrada e exige um handler Express.
module.exports = createApp();
module.exports.createApp = createApp;
