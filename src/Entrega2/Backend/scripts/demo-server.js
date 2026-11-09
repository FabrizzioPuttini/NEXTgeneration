// Ambiente demonstrativo local, isolado do servidor real e sem Supabase.
if (process.env.NODE_ENV === 'production' || process.env.VERCEL) throw new Error('Demonstração proibida em produção.');
const { createApp } = require('../src/app');
const { createDemoClients } = require('../test/fixtures/demo-clients');
const app = createApp(createDemoClients());
app.listen(38766, '127.0.0.1', () => console.log('DEMONSTRAÇÃO LOCAL FICTÍCIA: http://127.0.0.1:38766/admin/'));
