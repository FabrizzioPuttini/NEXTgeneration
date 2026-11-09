const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { PGlite } = require('@electric-sql/pglite');
const sql = name => {
  const file = path.join(__dirname, '../sql', name);
  return fs.existsSync(file) ? fs.readFileSync(file, 'utf8') : '-- comportamento ainda não implementado';
};
const REPAIR = '000a_repair_enrollments.sql';
const RESTORE = '000b_restore_enrollments.sql';

async function fixture() {
  const db = new PGlite();
  await db.exec(fs.readFileSync(path.join(__dirname, 'fixtures/schema.sql'), 'utf8'));
  await db.exec(`alter table enrollments add column attended_at timestamptz;
    alter table enrollments add column points_earned integer default 0;
    insert into auth.users(id) select ('10000000-0000-4000-8000-'||lpad(n::text,12,'0'))::uuid from generate_series(1,5) n;
    insert into courses(id,title) values ('20000000-0000-4000-8000-000000000001','Sintético');
    insert into enrollments(id,user_id,course_id,status,created_at)
    select ('30000000-0000-4000-8000-'||lpad((u*10+n)::text,12,'0'))::uuid,
      ('10000000-0000-4000-8000-'||lpad(u::text,12,'0'))::uuid,
      '20000000-0000-4000-8000-000000000001','enrolled', '2026-09-01Z'::timestamptz + n * interval '1 hour'
    from (values(1,3),(2,4),(3,2),(4,2),(5,1)) groups(u,copies), lateral generate_series(1,copies) n;`);
  return db;
}
async function rows(db) {
  return (await db.query('select to_jsonb(e) value from enrollments e order by id')).rows.map(r => r.value);
}

test('reparo preserva as canônicas e arquiva exatamente sete cópias completas', async () => {
  const db = await fixture();
  try {
    const before = await rows(db);
    await db.exec(sql(REPAIR));
    const active = await rows(db);
    assert.equal(active.length, 5);
    const archived = (await db.query('select row_data from e2_recovery.enrollment_duplicates_20261108 order by original_id')).rows.map(r => r.row_data);
    assert.equal(archived.length, 7);
    assert.deepEqual([...active, ...archived].sort((a,b)=>a.id.localeCompare(b.id)), before);
    assert.ok(active.every(r => r.id.endsWith('1')));
    await assert.rejects(db.exec("insert into enrollments(user_id,course_id,status) select user_id,course_id,'enrolled' from enrollments limit 1"), /duplicate key/);
  } finally { await db.close(); }
});

test('reparo é idempotente e mantém o arquivo inacessível aos clientes', async () => {
  const db = await fixture();
  try {
    await db.exec(sql(REPAIR));
    const first = await rows(db);
    await db.exec(sql(REPAIR));
    assert.deepEqual(await rows(db), first);
    assert.equal((await db.query('select count(*)::int n from e2_recovery.enrollment_duplicates_20261108')).rows[0].n,7);
    for (const role of ['anon','authenticated']) {
      await db.exec('set role '+role);
      await assert.rejects(db.query('select row_data from e2_recovery.enrollment_duplicates_20261108'), /permission denied/);
      await db.exec('reset role');
    }
  } finally { await db.close(); }
});

test('reparo rejeita cópias com evidência de presença sem alterar nenhuma linha', async () => {
  const db = await fixture();
  try {
    await db.exec("update enrollments set status='attended',attended_at=now(),points_earned=10 where id='30000000-0000-4000-8000-000000000013'");
    const before = await rows(db);
    await assert.rejects(db.exec(sql(REPAIR)), /REPAIR_CONFLICTING_ENROLLMENT/);
    await db.exec('rollback');
    assert.deepEqual(await rows(db),before);
  } finally { await db.close(); }
});

test('restore recompõe exatamente as inscrições anteriores e pode ser repetido', async () => {
  const db = await fixture();
  try {
    const before = await rows(db);
    await db.exec(sql(REPAIR));
    await db.exec(sql(RESTORE));
    assert.deepEqual(await rows(db),before);
    await db.exec(sql(RESTORE));
    assert.deepEqual(await rows(db),before);
  } finally { await db.close(); }
});

test('restore para se a inscrição canônica mudou depois do reparo', async () => {
  const db = await fixture();
  try {
    await db.exec(sql(REPAIR));
    await db.exec("update enrollments set status='attended' where id='30000000-0000-4000-8000-000000000011'");
    const before = await rows(db);
    await assert.rejects(db.exec(sql(RESTORE)), /RESTORE_CANONICAL_CHANGED/);
    await db.exec('rollback');
    assert.deepEqual(await rows(db),before);
  } finally { await db.close(); }
});

test('novo escopo ou dependência interrompe o reparo sem perda de dados', async () => {
  const mutations = [
    ["insert into enrollments(user_id,course_id,status) select user_id,course_id,'enrolled' from enrollments limit 1", /REPAIR_SCOPE_CHANGED/],
    ['create table enrollment_notes(enrollment_id uuid references enrollments(id))', /REPAIR_DEPENDENCIES_CHANGED/],
    ['alter table enrollments add column extra_info text', /REPAIR_SCHEMA_CHANGED/]
  ];
  for (const [mutation,error] of mutations) {
    const db = await fixture();
    try {
      await db.exec(mutation);
      const before = await rows(db);
      await assert.rejects(db.exec(sql(REPAIR)),error);
      await db.exec('rollback');
      assert.deepEqual(await rows(db),before);
    } finally { await db.close(); }
  }
});

test('migração E2 funciona após reparo e bloqueia rollback incompatível', async () => {
  const db = await fixture();
  try {
    await db.exec(sql(REPAIR));
    await db.exec(sql('001_entrega2.sql'));
    assert.equal((await db.query("select count(*)::int n from pg_tables where schemaname='public' and tablename like 'e2_%'")).rows[0].n,7);
    await assert.rejects(db.exec(sql(RESTORE)),/RESTORE_E2_ACTIVE/);
    await db.exec('rollback');
    assert.equal((await rows(db)).length,5);
  } finally { await db.close(); }
});

test('reparo mantém INSERT E1 para inscrição nova antes da ativação E2', async () => {
  const db = await fixture();
  try {
    await db.exec(sql(REPAIR));
    await db.exec("insert into courses(id,title) values('20000000-0000-4000-8000-000000000002','Novo curso')");
    await db.exec("select set_config('request.jwt.claim.sub','10000000-0000-4000-8000-000000000005',false);set role authenticated");
    await db.exec("insert into enrollments(user_id,course_id,status) values('10000000-0000-4000-8000-000000000005','20000000-0000-4000-8000-000000000002','enrolled')");
    assert.equal((await db.query('select count(*)::int n from enrollments')).rows[0].n,2);
  } finally { await db.close(); }
});
