const test = require('node:test');
const assert = require('node:assert/strict');
const http = require('node:http');
const app = require('../src/app');

test('entrada do Vercel exporta handler HTTP com health e validação de cadastro', async () => {
  assert.equal(typeof app, 'function');
  const server = http.createServer(app);
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const base = `http://127.0.0.1:${server.address().port}`;
  try {
    const health = await fetch(`${base}/health`);
    assert.equal(health.status, 200);
    assert.deepEqual(await health.json(), { status: 'ok' });
    const signup = await fetch(`${base}/api/auth/cadastro`, {
      method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{}',
    });
    assert.equal(signup.status, 400);
    assert.equal((await signup.json()).code, 'INVALID_INPUT');
  } finally {
    await new Promise(resolve => server.close(resolve));
  }
});
