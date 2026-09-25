# Backend — Tela de Agenda (App Próxima Etapa)

Backend REST em Node.js/Express que serve a tela de **Agenda** do app
Android da ONG **Próxima Etapa**. É apenas uma camada HTTP sobre um banco
Supabase (PostgreSQL) que já existe — este backend não cria, altera nem
migra nenhuma tabela.

Cada integrante do grupo é responsável pelo backend da própria tela; este
serviço cobre somente `/api/agenda`.

## Stack

- Node.js + Express
- `@supabase/supabase-js`
- `dotenv`
- `cors`
- JavaScript puro (CommonJS), sem TypeScript, sem ORM

## Estrutura de pastas

```
src/
  server.js              -> sobe o Express, registra rotas
  config/supabase.js     -> factory de client Supabase
  middleware/auth.js      -> extrai e valida o token do usuário
  routes/
    agenda.routes.js
  controllers/
    agenda.controller.js
.env.example
```

## Instalação

```bash
npm install
```

## Configuração do `.env`

Copie o arquivo de exemplo e preencha com os dados do seu projeto Supabase:

```bash
cp .env.example .env
```

Variáveis necessárias:

| Variável | Onde encontrar |
|---|---|
| `SUPABASE_URL` | Painel do Supabase → Project Settings → API → Project URL |
| `SUPABASE_ANON_KEY` | Painel do Supabase → Project Settings → API → anon public key |
| `PORT` | Porta local, ex.: `3000` (no Render, a plataforma define automaticamente) |

**Nunca commite o `.env`** — ele já está no `.gitignore` do repositório.

## Como rodar localmente

```bash
npm start
```

Ou, em modo desenvolvimento (reinicia automaticamente ao salvar arquivos,
usando `node --watch`, sem depender do `nodemon`):

```bash
npm run dev
```

O servidor sobe em `http://localhost:3000` (ou na porta definida em `PORT`).

## Endpoints

A rota abaixo, exceto `/health`, exige o header:

```
Authorization: Bearer <access_token>
```

O `access_token` é o JWT retornado pelo login do aluno no Supabase Auth
(feito pelo próprio app Android).

### `GET /health`

Verifica se o serviço está no ar. Sem autenticação — usado pelo Render.

**Request:**
```bash
curl http://localhost:3000/health
```

**Response `200`:**
```json
{ "status": "ok" }
```

### `GET /api/agenda`

Retorna os encontros do aluno logado, ordenados por data e horário de
início. Aceita o filtro opcional `?from=YYYY-MM-DD`.

**Request:**
```bash
curl "http://localhost:3000/api/agenda?from=2026-03-01" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

**Response `200`:**
```json
[
  {
    "activity_date": "2026-03-01",
    "start_time": "19:00:00",
    "end_time": "21:00:00",
    "course_title": "Introdução à Programação",
    "title": "Aula 1 — Lógica de programação",
    "location": "Sala 3",
    "attended": false
  }
]
```

## Testando localmente com `curl`

### 1. Obter um `access_token` de teste no Supabase

O app Android faz login direto no Supabase Auth. Para testar o backend
isoladamente (sem o app), você pode chamar o mesmo endpoint que o app usa:

```bash
curl -X POST "https://<SEU_PROJETO>.supabase.co/auth/v1/token?grant_type=password" \
  -H "apikey: $SUPABASE_ANON_KEY" \
  -H "Content-Type: application/json" \
  -d '{ "email": "aluno@teste.com", "password": "senha-do-aluno" }'
```

A resposta traz um campo `access_token` — use esse valor no header
`Authorization: Bearer <access_token>` das chamadas abaixo.

```bash
export ACCESS_TOKEN="<cole_aqui_o_access_token>"
```

### 2. Testar os endpoints

```bash
# health check (sem token)
curl http://localhost:3000/health

# agenda do aluno logado (sem filtro)
curl http://localhost:3000/api/agenda \
  -H "Authorization: Bearer $ACCESS_TOKEN"

# agenda filtrando a partir de uma data
curl "http://localhost:3000/api/agenda?from=2026-03-01" \
  -H "Authorization: Bearer $ACCESS_TOKEN"
```

## Por que o backend repassa o JWT do aluno em vez de usar a `service_role`

O banco tem **Row Level Security (RLS)** ativa em todas as tabelas: cada
aluno só pode ver/alterar os próprios dados. Essa regra é aplicada pelo
Postgres com base em `auth.uid()` — o id do usuário autenticado na
requisição atual.

Se o backend usasse a `service_role key` para consultar os dados, a RLS
seria **ignorada** (é assim que a `service_role` funciona: ela tem
permissão total e passa por cima das políticas). Nesse cenário, qualquer
aluno conseguiria ver a agenda de todos os outros alunos, quebrando o
requisito central do projeto.

Por isso, a cada requisição autenticada, o middleware
(`src/middleware/auth.js`) cria um **novo client Supabase** repassando o
JWT do aluno recebido no header `Authorization`, usando a `anon key`
(nunca a `service_role`):

```js
createClient(SUPABASE_URL, SUPABASE_ANON_KEY, {
  global: { headers: { Authorization: `Bearer ${tokenDoUsuario}` } }
});
```

Com isso, toda consulta feita com esse client já chega ao Postgres como
sendo "o aluno X", e a RLS filtra os dados automaticamente. O backend não
precisa (e não deve) escrever nenhum `WHERE student_id = ...` manual —
isso é responsabilidade do banco, garantida pela política de RLS.
