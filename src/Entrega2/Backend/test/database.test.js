const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { PGlite } = require('@electric-sql/pglite');
const { createHash } = require('node:crypto');
const U = '11111111-1111-4111-8111-111111111111';
const OTHER = '22222222-2222-4222-8222-222222222222';
const ADMIN = '33333333-3333-4333-8333-333333333333';
const COURSE = '44444444-4444-4444-8444-444444444444';
const ACTIVITY = '55555555-5555-4555-8555-555555555555';
const TOKEN = 'testQrOpaque'.repeat(4);

async function database() {
  const db = new PGlite();
  await db.exec(fs.readFileSync(path.join(__dirname, 'fixtures/schema.sql'), 'utf8'));
  await db.exec(fs.readFileSync(path.join(__dirname, '../sql/001_entrega2.sql'), 'utf8'));
  await db.query('insert into auth.users(id) values ($1),($2),($3)', [U, OTHER, ADMIN]);
  await db.query("insert into user_roles(user_id,role) values($1,'admin')", [ADMIN]);
  await db.query("insert into courses(id,title) values($1,'Curso fictício')", [COURSE]);
  await db.query("insert into activities(id,course_id,title,activity_date) values($1,$2,'Encontro',now())", [ACTIVITY, COURSE]);
  await db.query("insert into enrollments(user_id,course_id,status) values($1,$2,'enrolled')", [U, COURSE]);
  return db;
}
async function asUser(db, id) {
  await db.exec('reset role');
  await db.query("select set_config('request.jwt.claim.sub',$1,false)", [id || '']);
  await db.exec('set role authenticated');
}
async function issue(db) {
  await asUser(db, ADMIN);
  await db.query('select e2_issue_qr($1,$2,60)', [ACTIVITY, createHash('sha256').update(TOKEN).digest('hex')]);
}

test('RPC de presença nega sessão ausente no PostgreSQL local', async () => {
  const db = new PGlite();
  try {
    await db.exec(fs.readFileSync(path.join(__dirname, 'fixtures/schema.sql'), 'utf8'));
    await db.exec(fs.readFileSync(path.join(__dirname, '../sql/001_entrega2.sql'), 'utf8'));
    await assert.rejects(db.query('select public.e2_check_in($1)', ['a'.repeat(43)]), /UNAUTHENTICATED/);
  } finally { await db.close(); }
});

test('presença transacional: autorização, janela, repetição e bloqueio do DML direto', async () => {
  const db = await database();
  try {
    await asUser(db, U);
    await assert.rejects(db.query('select e2_issue_qr($1,$2,60)', [ACTIVITY, 'b'.repeat(64)]), /FORBIDDEN/);
    await assert.rejects(db.query("insert into user_roles(user_id,role) values($1,'admin')", [U]), /permission denied/);
    await issue(db);
    await asUser(db, OTHER);
    await assert.rejects(db.query('select e2_check_in($1,$2)', [TOKEN, ACTIVITY]), /NOT_ENROLLED/);
    await asUser(db, U);
    await assert.rejects(db.query('select e2_check_in($1,$2)', ['wrong'.repeat(10), ACTIVITY]), /INVALID_QR/);
    await assert.rejects(db.query('select e2_check_in($1,$2)', [TOKEN, COURSE]), /INVALID_QR/);
    const first = (await db.query('select e2_check_in($1,$2) result', [TOKEN, ACTIVITY])).rows[0].result;
    const again = (await db.query('select e2_check_in($1,$2) result', [TOKEN, ACTIVITY])).rows[0].result;
    assert.equal(first.created, true);
    assert.equal(again.created, false);
    assert.equal(first.attendance.id, again.attendance.id);
    await assert.rejects(db.query('insert into attendances(user_id,activity_id) values($1,$2)', [U, ACTIVITY]), /permission denied/);
    await assert.rejects(db.query('update attendances set scanned_at=now()'), /permission denied/);
    await asUser(db, OTHER);
    assert.equal((await db.query('select * from attendances')).rows.length, 0);
    await db.exec('reset role');
    await db.exec("update e2_qr_codes set opens_at=now()-interval '2 hours',expires_at=now()-interval '1 hour'");
    await asUser(db, U);
    await assert.rejects(db.query('select e2_check_in($1,$2)', [TOKEN, ACTIVITY]), /QR_EXPIRED/);
  } finally { await db.close(); }
});

test('inscrição respeita capacidade, repetição, curso ativo e proíbe escrita direta', async () => {
  const db = await database();
  try {
    await db.query('update courses set total_spots=1 where id=$1', [COURSE]);
    await asUser(db, U);
    assert.equal((await db.query('select e2_enroll($1) result', [COURSE])).rows[0].result.created, false);
    await asUser(db, OTHER);
    await assert.rejects(db.query('select e2_enroll($1)', [COURSE]), /COURSE_FULL/);
    await assert.rejects(db.query("insert into enrollments(user_id,course_id,status) values($1,$2,'attended')", [OTHER, COURSE]), /permission denied/);
    await db.exec('reset role');
    await db.query('update courses set total_spots=2 where id=$1', [COURSE]);
    await asUser(db, OTHER);
    assert.equal((await db.query('select e2_enroll($1) result', [COURSE])).rows[0].result.created, true);
    await db.exec('reset role');
    await db.query('update courses set is_active=false where id=$1', [COURSE]);
    await asUser(db, U);
    await assert.rejects(db.query('select e2_enroll($1)', [COURSE]), /NOT_FOUND/);
    await db.exec('reset role;set role anon');
    await assert.rejects(db.query('select e2_enroll($1)', [COURSE]), /permission denied/);
  } finally { await db.close(); }
});

test('QR futuro e encontro cancelado são recusados; mudança real de agenda notifica inscritos', async () => {
  const db = await database();
  try {
    await issue(db); await db.exec('reset role');
    await db.exec("update e2_qr_codes set opens_at=now()+interval '1 hour',expires_at=now()+interval '2 hours'");
    await asUser(db, U);
    await assert.rejects(db.query('select e2_check_in($1,$2)', [TOKEN, ACTIVITY]), /QR_NOT_ACTIVE/);
    await asUser(db, ADMIN);
    await db.query('update activities set title=title where id=$1', [ACTIVITY]);
    await asUser(db, U);
    assert.equal((await db.query('select * from e2_notifications where user_id=$1', [U])).rows.length, 0);
    await asUser(db, ADMIN);
    await db.query('update activities set is_cancelled=true where id=$1', [ACTIVITY]);
    await asUser(db, U);
    assert.equal((await db.query('select * from e2_notifications where user_id=$1', [U])).rows.length, 1);
    await assert.rejects(db.query('select e2_check_in($1,$2)', [TOKEN, ACTIVITY]), /NOT_FOUND/);
    await asUser(db, OTHER);
    assert.equal((await db.query('select * from e2_notifications where user_id=$1', [U])).rows.length, 0);
  } finally { await db.close(); }
});

test('certificado exige admin, confirmação, HTTPS, inscrição e permanece privado', async () => {
  const db = await database(); const url = 'https://example.test/certificado.pdf';
  try {
    await asUser(db, U);
    await assert.rejects(db.query('select e2_register_certificate($1,$2,$3,true)', [U, COURSE, url]), /FORBIDDEN/);
    await asUser(db, ADMIN);
    await assert.rejects(db.query('select e2_register_certificate($1,$2,$3,false)', [U, COURSE, url]), /INVALID_INPUT/);
    await assert.rejects(db.query('select e2_register_certificate($1,$2,$3,true)', [U, COURSE, 'http://example.test/a']), /INVALID_INPUT/);
    await assert.rejects(db.query('select e2_register_certificate($1,$2,$3,true)', [OTHER, COURSE, url]), /NOT_ENROLLED/);
    const first = (await db.query('select e2_register_certificate($1,$2,$3,true) result', [U, COURSE, url])).rows[0].result;
    const again = (await db.query('select e2_register_certificate($1,$2,$3,true) result', [U, COURSE, url])).rows[0].result;
    assert.equal(first.id, again.id);
    await asUser(db, U);
    assert.equal((await db.query('select * from certificates')).rows.length, 1);
    assert.equal((await db.query('select * from e2_notifications where user_id=$1', [U])).rows.length, 1);
    await asUser(db, OTHER);
    assert.equal((await db.query('select * from certificates')).rows.length, 0);
  } finally { await db.close(); }
});

test('mensagens, notificações e teste de perfil respeitam dono e cálculo do servidor', async () => {
  const db = await database();
  try {
    await asUser(db, U);
    const msg = (await db.query('select e2_send_message($1,$2) result', ['Minha dúvida', null])).rows[0].result;
    assert.equal(msg.remetente, 'aluno');
    await assert.rejects(db.query('select e2_send_message($1,$2)', ['Fingir equipe', OTHER]), /FORBIDDEN/);
    await asUser(db, OTHER);
    assert.equal((await db.query('select * from e2_messages')).rows.length, 0);
    await asUser(db, ADMIN);
    await db.query('select e2_send_message($1,$2)', ['Resposta da equipe', U]);
    await asUser(db, OTHER);
    const own = (await db.query('select * from e2_notifications where user_id=$1', [U])).rows;
    assert.equal(own.length, 0);
    await db.exec('reset role');
    const notification = (await db.query('select id from e2_notifications where user_id=$1', [U])).rows[0];
    await asUser(db, OTHER);
    await assert.rejects(db.query('select e2_mark_notification_read($1)', [notification.id]), /NOT_FOUND/);
    await asUser(db, U);
    await db.query('select e2_mark_notification_read($1)', [notification.id]);
    const questions = (await db.query('select id,alternatives from e2_profile_questions order by position')).rows;
    assert.ok(questions.length >= 3);
    await assert.rejects(db.query('select e2_submit_profile($1)', [JSON.stringify([])]), /INVALID_ANSWERS/);
    const answers = questions.map(q => ({ questionId: q.id, alternativeId: q.alternatives[0].id }));
    const result = (await db.query('select e2_submit_profile($1) result', [JSON.stringify(answers)])).rows[0].result;
    assert.equal(result.classificacao, 'Tecnologia e análise');
    await asUser(db, OTHER);
    assert.equal((await db.query('select * from e2_profile_results')).rows.length, 0);
  } finally { await db.close(); }
});
