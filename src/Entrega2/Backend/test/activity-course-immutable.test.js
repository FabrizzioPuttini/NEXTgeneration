const test = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const { PGlite } = require('@electric-sql/pglite');
const STUDENT = '11111111-1111-4111-8111-111111111111';
const ADMIN = '33333333-3333-4333-8333-333333333333';
const COURSE = '44444444-4444-4444-8444-444444444444';
const OTHER_COURSE = '66666666-6666-4666-8666-666666666666';
const ACTIVITY = '55555555-5555-4555-8555-555555555555';
const migration = () => fs.readFileSync(path.join(__dirname, '../sql/002_activity_course_immutable.sql'), 'utf8');
async function database() {
  const db = new PGlite();
  await db.exec(fs.readFileSync(path.join(__dirname, 'fixtures/schema.sql'), 'utf8'));
  await db.exec(fs.readFileSync(path.join(__dirname, '../sql/001_entrega2.sql'), 'utf8'));
  await db.query('insert into auth.users(id) values($1),($2)', [STUDENT, ADMIN]);
  await db.query("insert into user_roles(user_id,role) values($1,'admin')", [ADMIN]);
  await db.query("insert into courses(id,title) values($1,'Curso A'),($2,'Curso B')", [COURSE, OTHER_COURSE]);
  await db.query("insert into activities(id,course_id,title,activity_date) values($1,$2,'Encontro original','2026-11-19T22:00:00Z')", [ACTIVITY, COURSE]);
  await db.query("insert into enrollments(user_id,course_id,status) values($1,$2,'enrolled')", [STUDENT, COURSE]);
  await db.query('insert into attendances(user_id,activity_id) values($1,$2)', [STUDENT, ACTIVITY]);
  await db.exec(migration());
  return db;
}
async function asAdmin(db) {
  await db.query("select set_config('request.jwt.claim.sub',$1,false)", [ADMIN]);
  await db.exec('set role authenticated');
}

test('SQL002 impede reatribuir curso via atualização direta e conserva histórico', async () => {
  const db = await database();
  try {
    await asAdmin(db);
    await assert.rejects(db.query("update activities set course_id=$1,title='Indevido' where id=$2", [OTHER_COURSE, ACTIVITY]), /COURSE_IMMUTABLE/);
    await assert.rejects(db.query('update activities set course_id=null where id=$1', [ACTIVITY]), /COURSE_IMMUTABLE/);
    await db.exec('reset role');
    const rows = (await db.query('select a.course_id,a.title,c.title course_title from attendances p join activities a on a.id=p.activity_id join courses c on c.id=a.course_id where p.user_id=$1', [STUDENT])).rows;
    assert.deepEqual(rows, [{ course_id: COURSE, title: 'Encontro original', course_title: 'Curso A' }]);
  } finally { await db.close(); }
});

test('SQL002 permite mesmo curso, título/data e novos encontros', async () => {
  const db = await database();
  try {
    await asAdmin(db);
    await db.query('update activities set course_id=$1 where id=$2', [COURSE, ACTIVITY]);
    await db.query("update activities set title='Encontro atualizado',activity_date='2026-11-20T22:00:00Z' where id=$1", [ACTIVITY]);
    await db.query("insert into activities(course_id,title,activity_date) values($1,'Novo encontro',now())", [OTHER_COURSE]);
    const row = (await db.query('select course_id,title,activity_date from activities where id=$1', [ACTIVITY])).rows[0];
    assert.equal(row.course_id, COURSE); assert.equal(row.title, 'Encontro atualizado');
    assert.equal(new Date(row.activity_date).toISOString(), '2026-11-20T22:00:00.000Z');
  } finally { await db.close(); }
});

test('SQL002 reaplicável mantém um único trigger e preserva registros', async () => {
  const db = await database();
  try {
    await db.exec(migration());
    const triggers = (await db.query("select tgname from pg_trigger where tgrelid='public.activities'::regclass and tgname='e2_activity_course_immutable' and not tgisinternal")).rows;
    assert.equal(triggers.length, 1);
    assert.equal((await db.query('select count(*)::int n from attendances')).rows[0].n, 1);
    await asAdmin(db);
    await assert.rejects(db.query('update activities set course_id=$1 where id=$2', [OTHER_COURSE, ACTIVITY]), /COURSE_IMMUTABLE/);
  } finally { await db.close(); }
});
