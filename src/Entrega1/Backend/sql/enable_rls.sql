-- ==============================================================================
-- Habilita Row Level Security (RLS) em todas as tabelas do schema.sql
-- e define as policies necessárias para o app Android + backend (Agenda/Perfil).
--
-- Contexto: o Supabase Table Editor mostrava todas as tabelas como
-- "RLS disabled" / "UNRESTRICTED". Sem RLS, qualquer client autenticado
-- com a anon key consegue ler e escrever linhas de QUALQUER usuário,
-- mesmo que o backend já filtre por req.userId no código (defesa de
-- aplicação != defesa de banco).
--
-- Rode este script no SQL Editor do Supabase (Dashboard > SQL Editor)
-- do projeto "NextGeneration".
-- ==============================================================================

-- ------------------------------------------------------------------------------
-- 1. TABELAS USADAS PELA AGENDA E PELO PERFIL (backend atual)
-- ------------------------------------------------------------------------------

-- profiles: cada aluno só vê e edita o próprio perfil.
alter table public.profiles enable row level security;

create policy "profiles_select_own"
on public.profiles for select
to authenticated
using (auth.uid() = id);

create policy "profiles_update_own"
on public.profiles for update
to authenticated
using (auth.uid() = id)
with check (auth.uid() = id);

-- O próprio processo de cadastro (signup) deve poder criar o perfil do
-- usuário recém-criado.
create policy "profiles_insert_own"
on public.profiles for insert
to authenticated
with check (auth.uid() = id);

-- courses: conteúdo público (vitrine de cursos), leitura liberada para
-- qualquer usuário autenticado. Escrita fica só para service_role
-- (painel admin), por isso não há policy de insert/update/delete aqui.
alter table public.courses enable row level security;

create policy "courses_select_authenticated"
on public.courses for select
to authenticated
using (true);

-- universities: referenciada pelos cursos (nome/banner da universidade
-- parceira); leitura pública para autenticados, sem escrita pelo app.
alter table public.universities enable row level security;

create policy "universities_select_authenticated"
on public.universities for select
to authenticated
using (true);

-- activities: encontros/aulas dos cursos. Leitura liberada para
-- autenticados (o backend já filtra por curso em que o aluno está
-- inscrito antes de listar); sem escrita pelo app.
alter table public.activities enable row level security;

create policy "activities_select_authenticated"
on public.activities for select
to authenticated
using (true);

-- enrollments: só o próprio aluno vê e cria as próprias inscrições.
alter table public.enrollments enable row level security;

create policy "enrollments_select_own"
on public.enrollments for select
to authenticated
using (auth.uid() = user_id);

create policy "enrollments_insert_own"
on public.enrollments for insert
to authenticated
with check (auth.uid() = user_id);

-- attendances: só o próprio aluno vê e registra a própria presença
-- (check-in via QR Code). A constraint unique_attendance_per_activity
-- do schema já impede duplicidade; a policy garante que ninguém
-- registra presença em nome de outro aluno.
alter table public.attendances enable row level security;

create policy "attendances_select_own"
on public.attendances for select
to authenticated
using (auth.uid() = user_id);

create policy "attendances_insert_own"
on public.attendances for insert
to authenticated
with check (auth.uid() = user_id);

-- ------------------------------------------------------------------------------
-- 2. TABELAS AINDA NÃO CONSUMIDAS PELO BACKEND ATUAL
-- Habilitamos RLS com policies conservadoras: leitura pública onde o
-- dado é claramente institucional/de vitrine, e "sem policy" (bloqueio
-- total para anon/authenticated) onde o dado é sensível ou interno,
-- deixando acesso só para a service_role (painel admin).
-- ------------------------------------------------------------------------------

-- Conteúdo institucional/marketing: leitura pública para autenticados.
alter table public.badges enable row level security;
create policy "badges_select_authenticated"
on public.badges for select
to authenticated
using (true);

alter table public.impact_stats enable row level security;
create policy "impact_stats_select_authenticated"
on public.impact_stats for select
to authenticated
using (true);

alter table public.news_posts enable row level security;
create policy "news_posts_select_published"
on public.news_posts for select
to authenticated
using (is_published = true);

alter table public.videos enable row level security;
create policy "videos_select_active"
on public.videos for select
to authenticated
using (is_active = true);

-- news_comments: leitura pública dos comentários de posts publicados;
-- inserção permitida para autenticados (comentar como o próprio usuário).
alter table public.news_comments enable row level security;
create policy "news_comments_select_authenticated"
on public.news_comments for select
to authenticated
using (true);

create policy "news_comments_insert_authenticated"
on public.news_comments for insert
to authenticated
with check (true);

-- Dados pessoais do aluno: só o próprio dono vê.
alter table public.certificates enable row level security;
create policy "certificates_select_own"
on public.certificates for select
to authenticated
using (auth.uid() = user_id);

alter table public.user_badges enable row level security;
create policy "user_badges_select_own"
on public.user_badges for select
to authenticated
using (auth.uid() = user_id);

alter table public.test_results enable row level security;
create policy "test_results_select_own"
on public.test_results for select
to authenticated
using (auth.uid() = user_id);

create policy "test_results_insert_own"
on public.test_results for insert
to authenticated
with check (auth.uid() = user_id);

-- user_roles: controla quem é admin/student. Só o próprio usuário lê o
-- próprio papel; nenhuma escrita liberada pelo app (atribuição de role
-- é operação administrativa, feita via service_role).
alter table public.user_roles enable row level security;
create policy "user_roles_select_own"
on public.user_roles for select
to authenticated
using (auth.uid() = user_id);

-- Tabelas internas/administrativas: RLS habilitado e SEM policies para
-- anon/authenticated. Isso bloqueia todo acesso via app, deixando o
-- acesso restrito à service_role (painel admin/backoffice).
alter table public.ai_knowledge enable row level security;
alter table public.ai_settings enable row level security;

-- ==============================================================================
-- Fim do script. Após rodar, confira no Table Editor que todas as
-- tabelas mostram "RLS enabled" (sem o badge "UNRESTRICTED").
-- ==============================================================================
