# Contrato HTTP — Entrega 2, v1

Contrato congelado para integração Android em 08/11/2026. Alterações posteriores devem ser comunicadas.

Todas as rotas abaixo exigem `Authorization: Bearer <access_token>` salvo login/cadastro/refresh/health.
Datas são ISO8601 UTC; identificadores são strings UUID. Erros: `{error: string, code: string}`.
Falhas de dependência: 503. JWT ausente/inválido: 401. Administrador não autorizado: 403.
Listas retornam arrays (sem envelope). Campos adicionais são compatíveis.

## Android

- `POST /api/auth/login {email,senha}` → `{access_token,refresh_token,expires_in,expires_at,user:{id},message}` (existente).
- `POST /api/auth/refresh {refresh_token}` → `{access_token,refresh_token,expires_in,expires_at,user:{id}}`. Falha 401 exige login; preservar fila do mesmo user.id.
- `GET /api/cursos` → `[{id,title,description,location,workload,meetings_count,course_date,status}]`;
  status personalizado: `Disponível`, `Inscrito` ou `Concluído`.
- `POST /api/cursos/:id/inscricao {}` → `{message}`; reenvio já inscrito é sucesso.
- `GET /api/agenda` → `[{id,activity_id,course_id,activity_date,start_time,end_time,course_title,title,location,attended}]`.
- `POST /api/presencas/check-in {qrToken,activityId}` → `{attendance:{id,user_id,activity_id,scanned_at},created:boolean}`.
  Identidade e horário sempre vêm do servidor. Retentar o mesmo QR não duplica.
- `GET /api/presencas` → `[{id,activity_id,scanned_at,title,course_title}]` (somente próprias).
- `GET /api/card` → `{id,nome,escola,numero}`; numero é identificador acadêmico de demonstração derivado do UUID.
- `GET /api/certificados` → `[{id,title,workload,certificate_url,issued_at}]`.
- `GET /api/teste-perfil` → `{titulo,descricao,perguntas:[{id,enunciado,alternativas:[{id,texto}]}]}`.
- `POST /api/teste-perfil/resultados {answers:[{questionId,alternativeId}]}` → `{id,classificacao,resumo,created_at}`.
- `GET /api/teste-perfil/resultados` → `[{id,classificacao,resumo,created_at}]`.
- `GET /api/comunicados` → `[{id,titulo,conteudo,created_at}]`.
- `GET /api/mensagens` → `[{id,user_id,remetente,conteudo,created_at}]`; remetente = `aluno` ou `equipe`; últimas 500 em ordem cronológica.
- `POST /api/mensagens {conteudo}` → mensagem criada (201).
- `GET /api/notificacoes` → `[{id,titulo,conteudo,created_at,lida}]`.
- `PATCH /api/notificacoes/:id/lida {}` → `{ok:true}`.

## Sincronização

App guarda QR pendente, usuário dono e estado local; retenta em rede, com backoff.
Conteúdo do QR: JSON `{v:1,activityId:"uuid",qrToken:"token opaco"}`. activityId serve à correlação
e é conferido pelo servidor contra o token; nunca concede autorização. Guardar ambos na fila.
Sucesso confirma; 400/403/409/410 vira rejeitado; 401 requer login; 503/offline permanece pendente.
Reenvio de QR válido retorna created=false para presença existente. Histórico reconcilia resposta perdida.
Esta versão não aceita horário autodeclarado pelo celular nem garante aceitação de QR expirado.

## Painel web

- `GET /api/admin/me` → `{admin:true}`.
- `GET /api/admin/encontros` → `[{id,title,activity_date,course_id,course_title}]`.
- `POST /api/admin/encontros/:id/qr {durationMinutes:60}` → `{qrToken,qrPayload,qrDataUrl,opensAt,expiresAt,activityId}`; duração 5–240 minutos. qrPayload é a string JSON versionada para renderizar o QR; qrDataUrl é PNG base64 pronto para img src.
- `GET /api/admin/encontros/:id/presencas` → `[{id,user_id,scanned_at,nome}]`.
- `GET /api/admin/mensagens` → últimas 500 mensagens de suporte em ordem decrescente, com nome; `POST /api/admin/mensagens {userId,conteudo}` → resposta.
- `POST /api/admin/comunicados {titulo,conteudo}` → comunicado criado.

Painel e APIs usam o mesmo Express/Vercel. Privilégio admin será verificado no banco, sem service_role no browser.
Banco remoto não foi migrado: os endpoints E2 só funcionarão após aplicar SQL revisado e provisionar admin autorizado.

## Administração: cadastro e limites

- `GET /api/admin/alunos` → `[{id,full_name,school}]` (até500).
- `GET /api/admin/cursos` → cursos ativos/arquivados (até500).
- `POST /api/admin/cursos {title,description,location,course_date,workload,meetings_count,total_spots,is_active?}` → curso201.
- `PATCH /api/admin/cursos/:id` → mesmos campos opcionais, ao menos1; curso200.
- `DELETE /api/admin/cursos/:id` → `{ok:true}`; arquiva, não apaga vínculos.
- `POST /api/admin/encontros {title,course_id,activity_date,is_cancelled?}` → encontro201.
- `PATCH /api/admin/encontros/:id` → campos opcionais; encontro200.
- `DELETE /api/admin/encontros/:id` → `{ok:true}`; cancela e bloqueia QR, preserva histórico.
- `POST /api/admin/certificados {userId,courseId,certificateUrl,confirmed:true}` → certificado201;
  exige inscrição e confirmação de documento já emitido. Mesma URL/usuário/curso é idempotente sequencialmente.

Título curso/encontro:1–160; descrição0–4000; local0–300. workload1–10000,
meetings_count1–500, total_spots1–10000 inteiros. course_date/activity_date ISO8601.
Comunicado título1–120 e conteúdo1–4000; mensagem1–2000. URL certificado HTTPS até2000,
sem credenciais embutidas. Não é emissão automática nem comprovação arbitrária de conclusão.

Agenda, histórico e encontros/presenças administrativos: até1000. Cursos, certificados,
alunos e mensagens: até500. Resultados de perfil, comunicados e notificações: até100. Não há paginação nesta entrega. Notificações vêm de novos cursos ativos,
comunicados, certificado, resposta de suporte e alteração real de agenda para inscritos.
A notificação é consultada no app, sem push externo. Perfil inexistente usa nome de exibição
da sessão ou Aluno; escola fica vazia. Cadastro depende das regras de confirmação de email do Supabase.

## Perfil da conta

`GET /api/perfil` exige sessão e retorna `{nome, escola, email}`. Nome/escola vêm
do perfil do usuário autenticado; email vem da identidade validada pelo Supabase
Auth. Não aceita ID de outro usuário como seletor. Ausência de escola/email é
representada por string vazia. Idade não é coletada nem inferida neste contrato.
O campo email é uma adição compatível; clientes antigos podem ignorá-lo.
