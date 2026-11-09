// Somente fixtures locais. Nunca importado pelo entrypoint de produção.
const { randomUUID, createHash } = require('node:crypto');
const ids = { student: '11111111-1111-4111-8111-111111111111', admin: '33333333-3333-4333-8333-333333333333', course: '44444444-4444-4444-8444-444444444444', activity: '55555555-5555-4555-8555-555555555555' };
function createDemoClients() {
  const now = () => new Date().toISOString();
  const tables = {
    profiles: [{ id: ids.student, full_name: 'Aluno Demonstração', school: 'Escola fictícia' }],
    courses: [{ id: ids.course, title: 'Primeiros passos em tecnologia', description: 'Curso fictício para validar a entrega.', location: 'Sala de demonstração', course_date: now(), workload: 12, meetings_count: 1, total_spots: 30, is_active: true }],
    activities: [{ id: ids.activity, course_id: ids.course, title: 'Encontro de demonstração', activity_date: now(), is_cancelled: false }],
    enrollments: [{ id: randomUUID(), user_id: ids.student, course_id: ids.course, status: 'enrolled' }],
    attendances: [], certificates: [],
    e2_announcements: [{ id: randomUUID(), titulo: 'Bem-vindo à demonstração', conteudo: 'Todos os dados deste ambiente são fictícios.', created_at: now() }],
    e2_notifications: [], e2_notification_reads: [], e2_messages: [], e2_profile_results: [],
    e2_profile_questions: [1, 2, 3].map((position) => ({ id: `10000000-0000-4000-8000-00000000000${position}`, position,
      enunciado: ['Qual atividade mais desperta sua curiosidade?', 'Em um grupo, o que prefere fazer?', 'Qual oficina gostaria de experimentar?'][position - 1],
      alternatives: [{ id: 'a', texto: 'Investigar lógica e tecnologia', area: 'Tecnologia e análise' }, { id: 'b', texto: 'Ajudar e ensinar pessoas', area: 'Pessoas e educação' }, { id: 'c', texto: 'Criar e comunicar ideias', area: 'Criação e comunicação' }] })),
  };
  const codes = new Map();
  const session = (admin) => ({ access_token: admin ? 'demo-admin' : 'demo-aluno', refresh_token: admin ? 'refresh-demo-admin' : 'refresh-demo-aluno', expires_in: 3600, expires_at: Math.floor(Date.now() / 1000) + 3600 });
  class Query {
    constructor(table, userId, admin) { this.table = table; this.userId = userId; this.admin = admin; this.filters = []; this.cap = Infinity; }
    select(fields = '*') { this.fields = fields; return this; }
    eq(k, v) { this.filters.push(row => row[k] === v); return this; }
    in(k, values) { this.filters.push(row => values.includes(row[k])); return this; }
    gte(k, value) { this.filters.push(row => row[k] >= value); return this; }
    order(k, opts = {}) { this.sort = [k, opts.ascending !== false]; return this; }
    limit(n) { this.cap = n; return this; }
    single() { this.one = true; return this; }
    maybeSingle() { this.one = true; return this; }
    insert(body) { this.insertBody = body; return this; }
    update(body) { this.updateBody = body; return this; }
    async then(resolve, reject) {
      try {
        const table = tables[this.table]; if (!table) throw new Error('unknown fixture table');
        if (this.insertBody) table.push({ id: randomUUID(), created_at: now(), is_active: true, is_cancelled: false, ...this.insertBody });
        let rows = table.filter(r => this.filters.every(f => f(r)));
        if (this.insertBody) rows = [table.at(-1)];
        if (!this.admin && ['enrollments', 'attendances', 'certificates', 'e2_messages', 'e2_profile_results', 'e2_notification_reads'].includes(this.table)) rows = rows.filter(r => r.user_id === this.userId);
        if (this.table === 'e2_notifications') rows = rows.filter(r => !r.user_id || r.user_id === this.userId);
        if (this.updateBody) rows.forEach(r => Object.assign(r, this.updateBody));
        if (this.sort) { const [key, asc] = this.sort; rows.sort((a, b) => String(a[key]).localeCompare(String(b[key])) * (asc ? 1 : -1)); }
        rows = rows.slice(0, this.cap).map(r => {
          const row = { ...r };
          if (this.fields?.includes('courses(')) row.courses = tables.courses.find(c => c.id === r.course_id);
          if (this.fields?.includes('activities(')) { const a = tables.activities.find(a => a.id === r.activity_id); row.activities = a ? { ...a, courses: tables.courses.find(c => c.id === a.course_id) } : null; }
          return row;
        });
        return resolve({ data: this.one ? rows[0] || null : rows, error: null });
      } catch (err) { return reject(err); }
    }
  }
  function createClient(token) {
    const admin = token === 'demo-admin'; const valid = admin || token === 'demo-aluno'; const userId = admin ? ids.admin : ids.student;
    return {
      auth: {
        getUser: async () => valid ? { data: { user: { id: userId, email: admin ? 'admin@example.test' : 'aluno@example.test' } } } : { error: { message: 'invalid fixture token', status: 401 } },
        signInWithPassword: async ({ email, password }) => {
          if (!['admin@example.test', 'aluno@example.test'].includes(email) || password !== 'demonstracao123') return { error: { message: 'invalid fixture login', status: 400 } };
          const isAdmin = email.startsWith('admin'); return { data: { user: { id: isAdmin ? ids.admin : ids.student }, session: session(isAdmin) } };
        },
        refreshSession: async ({ refresh_token }) => ['refresh-demo-admin', 'refresh-demo-aluno'].includes(refresh_token)
          ? { data: { user: { id: refresh_token.endsWith('admin') ? ids.admin : ids.student }, session: session(refresh_token.endsWith('admin')) } } : { error: { message: 'bad refresh', status: 400 } },
        signUp: async () => ({ error: { message: 'Cadastro indisponível na fixture' } }),
      },
      from: table => new Query(table, userId, admin),
      rpc: async (name, args = {}) => {
        const fail = message => ({ error: { message } });
        if (!valid) return fail('UNAUTHENTICATED');
        if (name === 'e2_is_admin') return { data: admin };
        if (name === 'e2_issue_qr') {
          if (!admin) return fail('FORBIDDEN');
          const record = { activityId: args.p_activity_id, opensAt: now(), expiresAt: new Date(Date.now() + args.p_minutes * 60000).toISOString() };
          codes.set(args.p_hash, record); return { data: record };
        }
        if (name === 'e2_check_in') {
          const q = codes.get(createHash('sha256').update(args.p_token).digest('hex'));
          if (!q || q.activityId !== args.p_activity_id) return fail('INVALID_QR');
          if (Date.now() >= Date.parse(q.expiresAt)) return fail('QR_EXPIRED');
          const activity = tables.activities.find(a => a.id === q.activityId);
          if (!activity || activity.is_cancelled) return fail('NOT_FOUND');
          if (!tables.enrollments.some(e => e.user_id === userId && e.course_id === activity.course_id && ['enrolled', 'attended'].includes(e.status))) return fail('NOT_ENROLLED');
          let attendance = tables.attendances.find(a => a.user_id === userId && a.activity_id === q.activityId); const created = !attendance;
          if (!attendance) { attendance = { id: randomUUID(), user_id: userId, activity_id: q.activityId, scanned_at: now() }; tables.attendances.push(attendance); }
          return { data: { attendance, created } };
        }
        if (name === 'e2_enroll') {
          const c = tables.courses.find(c => c.id === args.p_course_id && c.is_active);
          if (!c) return fail('NOT_FOUND');
          if (!tables.enrollments.some(e => e.user_id === userId && e.course_id === c.id)) tables.enrollments.push({ id: randomUUID(), user_id: userId, course_id: c.id, status: 'enrolled' });
          return { data: { ok: true } };
        }
        if (name === 'e2_send_message') {
          if (args.p_user_id && !admin) return fail('FORBIDDEN');
          const m = { id: randomUUID(), user_id: args.p_user_id || userId, remetente: args.p_user_id ? 'equipe' : 'aluno', conteudo: args.p_content, created_at: now() };
          tables.e2_messages.push(m);
          if (args.p_user_id) tables.e2_notifications.push({ id: randomUUID(), user_id: args.p_user_id, titulo: 'Resposta da equipe', conteudo: 'Abra suas mensagens.', created_at: now() });
          return { data: m };
        }
        if (name === 'e2_mark_notification_read') {
          const n = tables.e2_notifications.find(n => n.id === args.p_id && (!n.user_id || n.user_id === userId)); if (!n) return fail('NOT_FOUND');
          tables.e2_notification_reads.push({ notification_id: n.id, user_id: userId }); return { data: { ok: true } };
        }
        if (name === 'e2_submit_profile') {
          const scores = new Map(); if (args.p_answers.length !== tables.e2_profile_questions.length) return fail('INVALID_ANSWERS');
          for (const q of tables.e2_profile_questions) {
            const answer = args.p_answers.find(a => a.questionId === q.id); const alternative = q.alternatives.find(a => a.id === answer?.alternativeId);
            if (!alternative) return fail('INVALID_ANSWERS'); scores.set(alternative.area, (scores.get(alternative.area) || 0) + 1);
          }
          const classificacao = [...scores].sort((a, b) => b[1] - a[1] || a[0].localeCompare(b[0]))[0][0];
          const r = { id: randomUUID(), user_id: userId, classificacao, resumo: 'Exploração acadêmica. Não é diagnóstico psicológico.', created_at: now() }; tables.e2_profile_results.push(r); return { data: r };
        }
        if (name === 'e2_register_certificate') {
          if (!admin) return fail('FORBIDDEN');
          if (!args.p_confirmed || !/^https:\/\//.test(args.p_url)) return fail('INVALID_INPUT');
          if (!tables.enrollments.some(e => e.user_id === args.p_user_id && e.course_id === args.p_course_id)) return fail('NOT_ENROLLED');
          const r = { id: randomUUID(), user_id: args.p_user_id, course_id: args.p_course_id, certificate_url: args.p_url, issued_at: now() };
          tables.certificates.push(r); return { data: r };
        }
        return fail('SERVICE_UNAVAILABLE');
      },
    };
  }
  return { createClient, tables, ids };
}
module.exports = { createDemoClients, ids };
