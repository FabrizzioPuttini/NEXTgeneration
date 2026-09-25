-- ==============================================================================
-- Seed de dados institucionais (NÃO depende de nenhum usuário real).
-- Pode ser rodado a qualquer momento no SQL Editor do Supabase, sem UUID
-- de aluno. Cria a universidade parceira, os cursos e as activities
-- (encontros/aulas) usados para testar a tela de Agenda.
--
-- Pré-requisito: as tabelas já devem existir (schema.sql já foi aplicado
-- e as tabelas aparecem no Table Editor).
--
-- Separado de propósito do seed do aluno (seed_aluno_teste.sql): assim,
-- se o UUID do aluno estiver errado, só aquele script falha — este aqui
-- não é afetado.
-- ==============================================================================

-- 1. Universidade parceira
insert into
  public.universities (id, name, description)
values
  (
    '11111111-1111-1111-1111-111111111111',
    'FECAP',
    'Fundação Escola de Comércio Álvares Penteado'
  )
on conflict (id) do nothing;

-- 2. Cursos com carga horária e quantidade de encontros
insert into
  public.courses (
    id,
    title,
    description,
    university_id,
    location,
    course_date,
    workload,
    meetings_count,
    total_spots,
    available_spots,
    has_certificate
  )
values
  (
    '22222222-2222-2222-2222-222222222222',
    'Jornada Cloud AWS',
    'Aprenda os fundamentos de infraestrutura em nuvem e prepare-se para o mercado de tecnologia.',
    '11111111-1111-1111-1111-111111111111',
    'Auditório Principal',
    '2026-10-10 14:00:00-03',
    12,
    3,
    50,
    49,
    true
  ),
  (
    '33333333-3333-3333-3333-333333333333',
    'Desenvolvimento de Jogos Web',
    'Crie seu primeiro jogo, faça o controle de versão e hospede sua aplicação na Vercel.',
    '11111111-1111-1111-1111-111111111111',
    'Laboratório de Informática',
    '2026-10-15 19:00:00-03',
    8,
    2,
    30,
    30,
    true
  )
on conflict (id) do nothing;

-- 3. Activities (agenda individual de cada aula)
insert into
  public.activities (id, course_id, title, activity_date)
values
  -- 3 encontros do curso de Cloud AWS
  (
    'aaaa1111-1111-1111-1111-111111111111',
    '22222222-2222-2222-2222-222222222222',
    'Aula 1: Introdução à Nuvem',
    '2026-10-10 14:00:00-03'
  ),
  (
    'aaaa2222-2222-2222-2222-222222222222',
    '22222222-2222-2222-2222-222222222222',
    'Aula 2: Serviços Essenciais AWS',
    '2026-10-17 14:00:00-03'
  ),
  (
    'aaaa3333-3333-3333-3333-333333333333',
    '22222222-2222-2222-2222-222222222222',
    'Aula 3: Deploy e Segurança',
    '2026-10-24 14:00:00-03'
  ),
  -- 2 encontros do curso de Jogos Web
  (
    'bbbb1111-1111-1111-1111-111111111111',
    '33333333-3333-3333-3333-333333333333',
    'Aula 1: Lógica e GitHub',
    '2026-10-15 19:00:00-03'
  ),
  (
    'bbbb2222-2222-2222-2222-222222222222',
    '33333333-3333-3333-3333-333333333333',
    'Aula 2: Deploy na Vercel',
    '2026-10-22 19:00:00-03'
  )
on conflict (id) do nothing;

-- Fim. Confira em Table Editor: universities (1 linha), courses (2 linhas),
-- activities (5 linhas).
