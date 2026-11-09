# Próxima Etapa — App do Aluno

Projeto Interdisciplinar do 3º semestre de Ciência da Computação, FECAP, 2º semestre de 2026.
Aplicativo Android para estudantes, API Express/Supabase e painel web para a equipe.

## Integrantes

| Nome | GitHub |
|---|---|
| Fabrizzio Puttini | [FabrizzioPuttini](https://github.com/FabrizzioPuttini) |
| Kaike Cavalcante | [kaikecav](https://github.com/kaikecav) |
| Enzo Sakita | [EnzoSakita](https://github.com/EnzoSakita) |
| Luiz Roberto Silvestre | [Luiz-Silvestre](https://github.com/Luiz-Silvestre) |

## Entrega 2

Implementação em `src/Entrega2`, integrada à `main`. A Entrega 1 foi preservada.
A migração Supabase foi aplicada em 08/11/2026. Em 09/11, API e painel publicados
responderam, e Luiz confirmou cadastro/login e acesso administrativo.
API: https://nextgeneration-seven.vercel.app/ — painel: https://nextgeneration-seven.vercel.app/admin/.
O ensaio completo de QR online/offline no ambiente publicado ainda está pendente.
Consulte o status abaixo para distinguir correções locais de alterações publicadas.

- [Comece pelos testes](src/Entrega2/TESTAR.md)
- [Android e configuração da API](src/Entrega2/Frontend/README.md)
- [Backend e painel administrativo](src/Entrega2/Backend/README.md)
- [Contrato HTTP](src/Entrega2/Backend/docs/http-contract.md)
- [Migração e publicação](src/Entrega2/Backend/docs/migration-runbook.md)
- [Documentos das disciplinas](documentos/README.md)
- [Princípios de POO e estruturas de dados](<documentos/Entrega 2/Programação Orientada a Objetos e Estrutura de Dados/README.md>)

## Funcionalidades implementadas

Login/cadastro, cursos e inscrição, agenda, leitura de QR, histórico de presença,
carteirinha, consulta de certificados, teste de interesses, comunicados, mensagens
e notificações periódicas. O painel permite gerenciar cursos e encontros, exibir
QR, acompanhar presença, publicar comunicados e responder alunos.

O aplicativo usa ConstraintLayout, Retrofit/Gson, SQLite e WorkManager. Dados salvos
e fila de presença são separados por usuário. Leitura offline fica pendente até a
validação pelo servidor; ela não garante presença se o código expirar antes do envio.
Notificações usam consulta periódica, não push instantâneo. O teste de interesses
é demonstrativo e não tem validade psicométrica.

## Demonstração local rápida

Requer Node.js 22 e Android Studio/SDK instalados.

```sh
cd src/Entrega2/Backend
npm ci
node scripts/demo-server.js
```

Painel: `http://127.0.0.1:38766/admin/`. Contas fictícias: `admin@example.test` e
`aluno@example.test`, senha `demonstracao123`. O ambiente é local e seus dados são
apagados ao reiniciar. A demonstração não usa o Supabase real.

Abra `src/Entrega2/Frontend` no Android Studio. Para usar o servidor de demonstração,
configure a URL debug conforme [TESTAR.md](src/Entrega2/TESTAR.md). Para usar o ambiente
hospedado, confirme o deploy do backend E2. A migração deste projeto já foi aplicada;
não a reaplique como etapa de teste.

## Estrutura

```text
src/Entrega1/       Código da primeira entrega preservado
src/Entrega2/
  Frontend/        Android Java/XML
  Backend/         Express, SQL, testes e public/admin
  TESTAR.md        Roteiro de teste e preparação da entrega
documentos/        Relatórios, diagramas e materiais acadêmicos
```

## Verificação e limites

Build Android, testes JVM, testes SQLite no emulador, testes HTTP/SQL locais e
verificação do painel no Chrome têm evidências no repositório. A revisão final e as
pendências estão em [status da entrega](src/Entrega2/STATUS.md). Testes locais não
substituem teste integrado no Supabase/Vercel. No celular físico, foram confirmados
QR online, fila offline com sincronização, câmera na vertical e abertura/download de PDF
de demonstração, usando backend local.

## Technical summary (English)

Próxima Etapa is an Android student application with an Express API and an administrative
web panel. Students can enroll in courses, view their schedule, scan attendance QR codes,
and access learning information. The Android client stores cached data and pending
attendance requests in SQLite, partitioned by authenticated user. WorkManager retries
pending requests and reconciles server history. PostgreSQL functions validate enrollment,
token validity and duplicate attendance inside a transaction. Supabase Auth identifies
users, and row-level security restricts access. The administrative panel uses the same
API deployment. Local tests use synthetic data. Physical-device QR scanning, offline
synchronization and opening/downloading a sample PDF were confirmed with the local
backend. Hosted end-to-end validation remains pending.

## Dados e demonstrações

As fixtures e evidências criadas para a Entrega 2 usam dados sintéticos. Não incluir
credenciais, arquivos .env, dados pessoais reais ou tokens de produção no repositório.
