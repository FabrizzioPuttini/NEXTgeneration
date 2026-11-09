-- Complemento da 001: preserva o curso original dos encontros e suas presenças.
-- Preparado/testado localmente. Aplicar no Supabase apenas após revisão/aprovação.
-- Não altera registros, permissões de tabelas, índices ou políticas RLS existentes.
-- CREATE/DROP TRIGGER toma lock na tabela; timeout aborta toda a transação caso
-- haja contenção. Não reaplicar 001 como parte desta correção.
begin;
set local lock_timeout = '5s';
set local statement_timeout = '30s';

create or replace function public.e2_guard_activity_course() returns trigger
language plpgsql security invoker set search_path = '' as $$
begin
  if new.course_id is distinct from old.course_id then
    raise exception 'COURSE_IMMUTABLE' using errcode = '23514';
  end if;
  return new;
end $$;
-- Função interna de trigger: não é uma RPC a ser exposta aos clientes.
revoke all on function public.e2_guard_activity_course() from public, anon, authenticated;

drop trigger if exists e2_activity_course_immutable on public.activities;
create trigger e2_activity_course_immutable
before update of course_id on public.activities
for each row execute function public.e2_guard_activity_course();
commit;

-- Rollback somente com aprovação: reabre a possibilidade de reatribuição por
-- clientes diretos; mantenha a proteção da API e avalie essa exposição antes.
-- Não reverte dados porque esta migration não modifica nenhum registro.
-- begin;
-- set local lock_timeout = '5s';
-- set local statement_timeout = '30s';
-- drop trigger if exists e2_activity_course_immutable on public.activities;
-- drop function if exists public.e2_guard_activity_course();
-- commit;
