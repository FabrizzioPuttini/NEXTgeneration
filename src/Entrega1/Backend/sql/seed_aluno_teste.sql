-- ==============================================================================
-- Seed do aluno de teste (DEPENDE de um usuário real já criado em
-- Authentication > Users no Supabase).
--
-- Rode este script SEPARADO do seed_institucional.sql. Se o UUID abaixo
-- estiver errado ou o usuário não existir, só este script falha — o
-- seed institucional (universities/courses/activities) não é afetado.
--
-- Passo a passo:
--   1. Supabase Dashboard > Authentication > Users > Add user
--      (ou crie pelo signup do app)
--   2. Copie o UUID do usuário criado (coluna "id" na lista de users)
--   3. Substitua TODAS as ocorrências de 'COLOQUE_SEU_UUID_AQUI' abaixo
--      pelo UUID copiado
--   4. Rode este script no SQL Editor
-- ==============================================================================

-- Aluno de teste em uso: aluno@teste.com
-- UID: a50fc5ad-b974-444f-8009-ccdb4e0deacd

-- 1. Perfil público do aluno
insert into public.profiles (id, full_name, school, grade, city, points, level)
values
  (
    'a50fc5ad-b974-444f-8009-ccdb4e0deacd',
    'Estudante de Teste',
    'Escola Estadual SP',
    '3º Ano Ensino Médio',
    'São Paulo',
    150,
    'Explorador'
  )
on conflict (id) do nothing;

-- 2. Inscrição no curso de Cloud AWS
insert into public.enrollments (user_id, course_id, status)
values
  (
    'a50fc5ad-b974-444f-8009-ccdb4e0deacd',
    '22222222-2222-2222-2222-222222222222',
    'enrolled'
  );

-- 3. Simula que o aluno já escaneou o QR Code da Aula 1 de AWS
insert into public.attendances (user_id, activity_id)
values
  (
    'a50fc5ad-b974-444f-8009-ccdb4e0deacd',
    'aaaa1111-1111-1111-1111-111111111111'
  )
on conflict (user_id, activity_id) do nothing;

-- Fim. Confira em Table Editor: profiles (1 linha), enrollments (1 linha),
-- attendances (1 linha). Depois disso, GET /api/agenda para este usuário
-- deve trazer os 3 encontros do curso de Cloud AWS, com o primeiro
-- marcado como attended = true.
