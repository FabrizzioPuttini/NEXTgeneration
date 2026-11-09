# Backend — Entrega 2

Express + Supabase: autenticação, cursos/inscrição, agenda, presença por QR,
histórico, card, certificados, teste de interesses, comunicados, suporte e notificações.
Painel administrativo em `/admin/`. Migração aplicada em 08/11/2026; API e painel publicados
em https://nextgeneration-seven.vercel.app/ e verificados em 09/11.
Veja [status da entrega](../STATUS.md) para alterações locais ainda não publicadas.

## Executar

Node.js 22. No diretório deste backend:

```sh
npm ci
npm test
npm start
```

Produção requer `SUPABASE_URL` e `SUPABASE_ANON_KEY` no ambiente, nunca no Git.
`PORT` é opcional (3000). Cada requisição usa cliente Supabase isolado e JWT do usuário.
Nenhuma service_role é necessária. Leia [contrato HTTP](docs/http-contract.md).

## Demonstração sem banco remoto

```sh
node scripts/demo-server.js
```

Abre somente localhost, porta 38766. Em `http://127.0.0.1:38766/admin/`, use
`admin@example.test` e senha fictícia `demonstracao123`. Aluno de teste:
`aluno@example.test`, mesma senha. Os dados são fictícios e somem ao reiniciar.
Esse script é separado do start de produção e recusa NODE_ENV=production ou VERCEL.
Não comprova integração com Supabase hospedado.

## Banco e publicação

Leia [runbook da migração](docs/migration-runbook.md). `sql/000_preflight.sql` é
somente leitura; `sql/001_entrega2.sql` está preparada, mas exige revisão do schema
real, backup e aprovação antes de aplicar. Ela restringe INSERT direto em inscrições
e presenças; o backend antigo perde compatibilidade de escrita nesses caminhos.

Na Vercel, Root Directory deve ser `src/Entrega2/Backend`. Manter as variáveis do
projeto Supabase atual, publicar a versão revisada e validar `/health`, login,
agenda e o fluxo completo de QR. Arquivos `public/admin/` são servidos como estáticos.
O painel só autoriza contas cujo registro protegido em user_roles tenha role admin.
Atribuição desse papel é ação administrativa externa, não oferecida pelo app.

## Evidência e limites

`npm test` executa testes HTTP com adaptador fictício e PostgreSQL local PGlite com
roles, RLS e RPCs reais. A fixture contém o subconjunto conhecido do schema;
não replica Supabase Auth, PostgREST, hospedagem nem concorrência de múltiplas conexões.
O serviço puro inicial foi substituído pela RPC transacional e removido: seus
25 testes históricos não contam como cobertura da versão atual.

O teste de interesses é demonstrativo, sem validade psicométrica. Certificados
são URLs HTTPS de documentos já emitidos, cadastradas após confirmação da equipe.
As notificações são consultadas no app; não há envio push por Firebase.
Listagens têm limites explícitos no contrato; não existe paginação nesta entrega.

## Cabeçalhos no Vercel

O Vercel serve `public/` pela CDN, sem executar `express.static()`. Por isso,
`vercel.json` define os cabeçalhos para todas as rotas, inclusive `/admin/` e seus
arquivos. O middleware Express protege as respostas dinâmicas e o servidor local.
Fonte: [Express no Vercel](https://vercel.com/docs/frameworks/backend/express#serving-static-assets).

Depois da publicação, confira com `curl -I https://nextgeneration-seven.vercel.app/admin/`:
`Content-Security-Policy`, `X-Content-Type-Options: nosniff`,
`Referrer-Policy: no-referrer`, `X-Frame-Options: DENY` e `Cache-Control: no-store`.
Se faltarem, confirme que o deploy usa o commit aprovado e Root Directory
`src/Entrega2/Backend`, incluindo seu `vercel.json`. A mudança local não comprova o deploy.
