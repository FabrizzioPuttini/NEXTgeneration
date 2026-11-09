const test = require('node:test');
const assert = require('node:assert/strict');
const { createApp } = require('../src/app');
const { createDemoClients } = require('./fixtures/demo-clients');
const UID = '11111111-1111-4111-8111-111111111111';
const AID = '55555555-5555-4555-8555-555555555555';

async function server(options, fn) {
  const app = createApp(options);
  const listener = app.listen(0, '127.0.0.1');
  await new Promise(resolve => listener.once('listening', resolve));
  try { await fn(`http://127.0.0.1:${listener.address().port}`); }
  finally { await new Promise(resolve => listener.close(resolve)); }
}
function clients({ admin = false, rpcError, authError, capture = [] } = {}) {
  return token => ({
    auth: {
      getUser: async () => authError ? { error: authError } : token === 'valid' ? { data: { user: { id: UID } }, error: null } : { error: { message: 'bad token', status: 401 } },
      signInWithPassword: async input => { capture.push(input); return authError ? { error: authError } : { data: { user: { id: UID }, session: { access_token: 'valid', refresh_token: 'refresh', expires_in: 3600, expires_at: 10000 } }, error: null }; },
      refreshSession: async () => ({ data: { user: { id: UID }, session: { access_token: 'valid', refresh_token: 'new-refresh', expires_in: 3600, expires_at: 10000 } } }),
    },
    rpc: async (name, args) => {
      capture.push({ name, args });
      if (name === 'e2_is_admin') return { data: admin, error: null };
      if (rpcError) return { error: { message: rpcError } };
      if (name === 'e2_issue_qr') return { data: { activityId: AID, opensAt: '2026-11-09T10:00:00Z', expiresAt: '2026-11-09T11:00:00Z' } };
      return { data: { attendance: { id: 'att', user_id: UID, activity_id: AID, scanned_at: '2026-11-09T10:00:00Z' }, created: true } };
    },
  });
}
const headers = { Authorization: 'Bearer valid', 'Content-Type': 'application/json' };

test('conversa conserva as últimas 500 mensagens em ordem cronológica', async () => {
  const fixture = createDemoClients();
  fixture.tables.e2_messages.push(...Array.from({ length: 501 }, (_, i) => ({
    id: String(i), user_id: fixture.ids.student, remetente: 'aluno', conteudo: String(i),
    created_at: new Date(1700000000000 + i * 1000).toISOString(),
  })));
  await server({ createClient: fixture.createClient }, async base => {
    const rows = await (await fetch(base + '/api/mensagens', { headers: { Authorization: 'Bearer demo-aluno' } })).json();
    assert.equal(rows.length, 500); assert.equal(rows[0].conteudo, '1'); assert.equal(rows.at(-1).conteudo, '500');
  });
});

test('health disponível e API privada exige sessão', async () => {
  await server({ createClient: clients() }, async base => {
    assert.equal((await fetch(base + '/health')).status, 200);
    const r = await fetch(base + '/api/card');
    assert.equal(r.status, 401);
    assert.equal((await r.json()).code, 'UNAUTHENTICATED');
  });
});

test('indisponibilidade Auth é 503 e não invalida sessão como credencial incorreta', async () => {
  await server({ createClient: clients({ authError: { status: 503, message: 'upstream offline' } }) }, async base => {
    assert.equal((await fetch(base + '/api/card', { headers })).status, 503);
    assert.equal((await fetch(base + '/api/auth/login', { method: 'POST', headers, body: JSON.stringify({ email: 'a@example.test', senha: 'senha123' }) })).status, 503);
  });
});

test('admin redireciona somente URL sem barra e não entra em loop', async () => {
  await server({ createClient: clients() }, async base => {
    const redirect = await fetch(base + '/admin', { redirect: 'manual' });
    assert.equal(redirect.status, 302); assert.equal(redirect.headers.get('location'), '/admin/');
    const page = await fetch(base + '/admin/', { redirect: 'manual' });
    assert.equal(page.status, 200);
    assert.match(page.headers.get('content-type'), /text\/html/);
    assert.equal(page.headers.get('x-frame-options'), 'DENY');
  });
});
test('login preserva senha e refresh devolve identidade e expiração', async () => {
  const capture = [];
  await server({ createClient: clients({ capture }) }, async base => {
    const r = await fetch(base + '/api/auth/login', { method: 'POST', headers, body: JSON.stringify({ email: ' aluno@example.test ', senha: ' senha com espaços ' }) });
    assert.equal(r.status, 200);
    assert.equal((await r.json()).user.id, UID);
    assert.equal(capture[0].password, ' senha com espaços ');
    const refresh = await fetch(base + '/api/auth/refresh', { method: 'POST', headers, body: JSON.stringify({ refresh_token: 'refresh' }) });
    assert.equal(refresh.status, 200);
    assert.equal((await refresh.json()).refresh_token, 'new-refresh');
  });
});
test('check-in usa RPC transacional e ignora user_id do corpo', async () => {
  const capture = [];
  await server({ createClient: clients({ capture }) }, async base => {
    const r = await fetch(base + '/api/presencas/check-in', { method: 'POST', headers, body: JSON.stringify({ qrToken: 'a'.repeat(43), activityId: AID, user_id: 'evil' }) });
    assert.equal(r.status, 201);
    assert.deepEqual(capture[0], { name: 'e2_check_in', args: { p_token: 'a'.repeat(43), p_activity_id: AID } });
  });
});
test('check-in inválido falha antes de chamar banco', async () => {
  const capture = [];
  await server({ createClient: clients({ capture }) }, async base => {
    const r = await fetch(base + '/api/presencas/check-in', { method: 'POST', headers, body: JSON.stringify({ qrToken: 'bad', activityId: AID }) });
    assert.equal(r.status, 400); assert.equal(capture.length, 0);
  });
});
test('SQL erro conhecido mapeia status e erro desconhecido não vaza detalhes', async () => {
  for (const [rpcError, status, code] of [['QR_EXPIRED', 410, 'QR_EXPIRED'], ['password=db-secret', 503, 'SERVICE_UNAVAILABLE']]) {
    await server({ createClient: clients({ rpcError }) }, async base => {
      const r = await fetch(base + '/api/presencas/check-in', { method: 'POST', headers, body: JSON.stringify({ qrToken: 'a'.repeat(43), activityId: AID }) });
      assert.equal(r.status, status); const body = await r.json();
      assert.equal(body.code, code); assert.ok(!JSON.stringify(body).includes('db-secret'));
    });
  }
});
test('aluno não acessa área administrativa', async () => {
  await server({ createClient: clients() }, async base => assert.equal((await fetch(base + '/api/admin/me', { headers })).status, 403));
});
test('admin emite QR JSON vinculado ao encontro com PNG e hash persistido', async () => {
  const capture = [];
  await server({ createClient: clients({ admin: true, capture }) }, async base => {
    const r = await fetch(base + `/api/admin/encontros/${AID}/qr`, { method: 'POST', headers, body: JSON.stringify({ durationMinutes: 60 }) });
    assert.equal(r.status, 201); const body = await r.json();
    assert.deepEqual(JSON.parse(body.qrPayload), { v: 1, activityId: AID, qrToken: body.qrToken });
    assert.match(body.qrDataUrl, /^data:image\/png;base64,/);
    assert.match(capture[1].args.p_hash, /^[a-f0-9]{64}$/);
    assert.notEqual(capture[1].args.p_hash, body.qrToken);
  });
});

test('certificado administrativo exige HTTPS e confirmação explícita', async () => {
  const capture = [];
  await server({ createClient: clients({ admin: true, capture }) }, async base => {
    for (const [certificateUrl, confirmed] of [['http://example.test/file.pdf', true], ['https://example.test/file.pdf', false]]) {
      const r = await fetch(base + '/api/admin/certificados', { method: 'POST', headers, body: JSON.stringify({ userId: UID, courseId: AID, certificateUrl, confirmed }) });
      assert.equal(r.status, 400);
    }
    const r = await fetch(base + '/api/admin/certificados', { method: 'POST', headers, body: JSON.stringify({ userId: UID, courseId: AID, certificateUrl: 'https://example.test/file.pdf', confirmed: true }) });
    assert.equal(r.status, 201);
    assert.equal(capture.at(-1).name, 'e2_register_certificate');
  });
});

test('fluxos HTTP de listas, suporte, quiz, CRUD e presença usam o contrato integrado com fixture explícita', async () => {
  const fixture = createDemoClients();
  await server(fixture, async base => {
    const send = async (url, token, method = 'GET', body) => {
      const r = await fetch(base + url, { method, headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' }, ...(body ? { body: JSON.stringify(body) } : {}) });
      const json = await r.json(); assert.ok(r.ok, `${url}: ${r.status} ${JSON.stringify(json)}`); return json;
    };
    const student = 'demo-aluno', admin = 'demo-admin';
    const courses = await send('/api/cursos', student); assert.equal(courses[0].status, 'Inscrito');
    const agenda = await send('/api/agenda', student); assert.equal(agenda[0].activity_id, fixture.ids.activity);
    const card = await send('/api/card', student); assert.equal(card.id, fixture.ids.student);
    assert.equal((await send('/api/certificados', student)).length, 0);
    const qr = await send(`/api/admin/encontros/${fixture.ids.activity}/qr`, admin, 'POST', { durationMinutes: 60 });
    await send('/api/presencas/check-in', student, 'POST', { qrToken: qr.qrToken, activityId: fixture.ids.activity });
    assert.equal((await send('/api/presencas', student))[0].activity_id, fixture.ids.activity);
    assert.equal((await send('/api/agenda', student))[0].attended, true);
    assert.equal((await send(`/api/admin/encontros/${fixture.ids.activity}/presencas`, admin))[0].nome, 'Aluno Demonstração');
    await send('/api/mensagens', student, 'POST', { conteudo: 'Preciso de ajuda' });
    assert.equal((await send('/api/admin/mensagens', admin))[0].user_id, fixture.ids.student);
    await send('/api/admin/mensagens', admin, 'POST', { userId: fixture.ids.student, conteudo: 'Vamos ajudar' });
    assert.equal((await send('/api/mensagens', student)).length, 2);
    const notifications = await send('/api/notificacoes', student); assert.equal(notifications[0].lida, false);
    await send(`/api/notificacoes/${notifications[0].id}/lida`, student, 'PATCH', {});
    assert.equal((await send('/api/notificacoes', student))[0].lida, true);
    const quiz = await send('/api/teste-perfil', student);
    assert.equal(quiz.perguntas[0].alternativas[0].area, undefined);
    const answers = quiz.perguntas.map(q => ({ questionId: q.id, alternativeId: q.alternativas[0].id }));
    assert.equal((await send('/api/teste-perfil/resultados', student, 'POST', { answers })).classificacao, 'Tecnologia e análise');
    assert.equal((await send('/api/teste-perfil/resultados', student)).length, 1);
    const course = await send('/api/admin/cursos', admin, 'POST', { title: 'Novo curso', course_date: '2026-11-09T10:00:00Z', workload: 4, meetings_count: 1 });
    assert.equal(course.title, 'Novo curso');
    await send(`/api/admin/cursos/${course.id}`, admin, 'PATCH', { title: 'Curso atualizado' });
    await send(`/api/admin/cursos/${course.id}`, admin, 'DELETE');
    assert.equal(fixture.tables.courses.find(c => c.id === course.id).is_active, false);
    const activity = await send('/api/admin/encontros', admin, 'POST', { title: 'Extra', course_id: fixture.ids.course, activity_date: '2026-11-09T12:00:00Z' });
    await send(`/api/admin/encontros/${activity.id}`, admin, 'DELETE');
    assert.equal(fixture.tables.activities.find(a => a.id === activity.id).is_cancelled, true);
    await send('/api/admin/comunicados', admin, 'POST', { titulo: 'Aviso', conteudo: 'Atualização' });
    assert.ok((await send('/api/comunicados', student)).some(c => c.titulo === 'Aviso'));
  });
});


test('perfil devolve apenas identidade autenticada, com email e escola, sem inventar idade', async () => {
  const fixture = createDemoClients();
  const createClient = token => {
    const client = fixture.createClient(token);
    const getUser = client.auth.getUser;
    client.auth.getUser = async () => {
      const result = await getUser();
      if (result.data?.user) result.data.user.email = token === 'demo-admin' ? 'admin@example.test' : 'aluno@example.test';
      return result;
    };
    return client;
  };
  await server({ createClient }, async base => {
    assert.equal((await fetch(base + '/api/perfil')).status, 401);
    const response = await fetch(base + '/api/perfil?user_id=' + fixture.ids.admin, {
      headers: { Authorization: 'Bearer demo-aluno' },
    });
    assert.equal(response.status, 200);
    assert.deepEqual(await response.json(), {
      nome: 'Aluno Demonstração', escola: 'Escola fictícia', email: 'aluno@example.test',
    });
    const admin = await (await fetch(base + '/api/perfil', {
      headers: { Authorization: 'Bearer demo-admin' },
    })).json();
    assert.equal(admin.email, 'admin@example.test');
    assert.notEqual(admin.nome, 'Aluno Demonstração');
  });
});

test('cadastro distingue validação de indisponibilidade do provedor sem vazar detalhes', async () => {
  for (const [upstreamStatus, expectedStatus, expectedCode] of [
    [400, 400, 'INVALID_INPUT'], [422, 400, 'INVALID_INPUT'],
    [503, 503, 'SERVICE_UNAVAILABLE'], [500, 503, 'SERVICE_UNAVAILABLE'],
    [429, 503, 'SERVICE_UNAVAILABLE'], [undefined, 503, 'SERVICE_UNAVAILABLE'],
  ]) {
    const createClient = () => ({ auth: { signUp: async () => ({ error: { status: upstreamStatus, message: 'private-provider-detail' } }) } });
    await server({ createClient }, async base => {
      const response = await fetch(base + '/api/auth/cadastro', { method: 'POST', headers, body: JSON.stringify({ email: 'aluno@example.test', nome: 'Aluno', senha: 'senha1234' }) });
      assert.equal(response.status, expectedStatus, `upstream ${upstreamStatus}`);
      const result = await response.json();
      assert.equal(result.code, expectedCode);
      assert.ok(!JSON.stringify(result).includes('private-provider-detail'));
    });
  }
});

test('edição de encontro não pode reatribuir presenças a outro curso', async () => {
  const fixture = createDemoClients();
  const originalCourse = fixture.ids.course;
  const otherCourse = '66666666-6666-4666-8666-666666666666';
  fixture.tables.courses.push({ ...fixture.tables.courses[0], id: otherCourse, title: 'Outro curso' });
  fixture.tables.attendances.push({ id: 'attendance', user_id: fixture.ids.student, activity_id: fixture.ids.activity, scanned_at: new Date().toISOString() });
  await server(fixture, async base => {
    const response = await fetch(base + `/api/admin/encontros/${fixture.ids.activity}`, { method: 'PATCH', headers: { ...headers, Authorization: 'Bearer demo-admin' }, body: JSON.stringify({ course_id: otherCourse, title: 'Título adulterado' }) });
    assert.equal(response.status, 409);
    assert.equal((await response.json()).code, 'COURSE_IMMUTABLE');
    assert.equal(fixture.tables.activities[0].course_id, originalCourse);
    assert.equal(fixture.tables.activities[0].title, 'Encontro de demonstração');
    const history = await (await fetch(base + '/api/presencas', { headers: { Authorization: 'Bearer demo-aluno' } })).json();
    assert.equal(history[0].course_title, 'Primeiros passos em tecnologia');
  });
});

test('edição aceita mesmo curso e alterações de título sem reenviar vínculo ao banco', async () => {
  const fixture = createDemoClients(); const updates = [];
  const createClient = token => {
    const client = fixture.createClient(token); const from = client.from;
    client.from = table => {
      const query = from(table); const update = query.update;
      query.update = body => { updates.push({ ...body }); return update.call(query, body); };
      return query;
    };
    return client;
  };
  await server({ createClient }, async base => {
    for (const body of [{ course_id: fixture.ids.course, title: 'Título atualizado' }, { title: 'Outro título' }, { course_id: fixture.ids.course }]) {
      const response = await fetch(base + `/api/admin/encontros/${fixture.ids.activity}`, { method: 'PATCH', headers: { ...headers, Authorization: 'Bearer demo-admin' }, body: JSON.stringify(body) });
      assert.equal(response.status, 200);
    }
    assert.equal(fixture.tables.activities[0].course_id, fixture.ids.course);
    assert.equal(fixture.tables.activities[0].title, 'Outro título');
    assert.equal(updates.length, 2);
    assert.ok(updates.every(body => !Object.hasOwn(body, 'course_id')));
  });
});
