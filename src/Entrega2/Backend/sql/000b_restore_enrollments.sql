-- NÃO faz parte do apply. Recuperação manual do reparo, antes de ativar E2.
-- Requer aprovação específica. Recusa se canônicas mudaram ou E2 foi instalado.
begin;
set local lock_timeout='5s';
set local statement_timeout='30s';
lock table public.enrollments in share row exclusive mode;
lock table e2_recovery.enrollment_duplicates_20261108 in share mode;
do $$ begin
 if to_regprocedure('public.e2_enroll(uuid)') is not null then
  raise exception 'RESTORE_E2_ACTIVE';
 end if;
 if (select count(*) from e2_recovery.enrollment_duplicates_20261108)<>7 then
  raise exception 'RESTORE_ARCHIVE_COUNT_MISMATCH';
 end if;
 if exists(select 1 from e2_recovery.enrollment_duplicates_20261108 a
   left join public.enrollments e on e.id=a.canonical_id
   where e.id is null or to_jsonb(e)<>a.canonical_data) then
  raise exception 'RESTORE_CANONICAL_CHANGED';
 end if;
 if exists(select 1 from e2_recovery.enrollment_duplicates_20261108 a
   join public.enrollments e on e.id=a.original_id where to_jsonb(e)<>a.row_data) then
  raise exception 'RESTORE_ID_CONFLICT';
 end if;
end $$;
-- A remoção do índice reabre possibilidade de duplicação; somente recuperação pré-E2.
drop index if exists public.e2_enrollment_once;
insert into public.enrollments(id,user_id,course_id,status,attended_at,points_earned,created_at)
select r.id,r.user_id,r.course_id,r.status,r.attended_at,r.points_earned,r.created_at
from e2_recovery.enrollment_duplicates_20261108 a
cross join lateral jsonb_populate_record(null::public.enrollments,a.row_data) r
on conflict(id) do nothing;
do $$ begin
 if exists(select 1 from e2_recovery.enrollment_duplicates_20261108 a
   left join public.enrollments e on e.id=a.original_id where e.id is null or to_jsonb(e)<>a.row_data) then
  raise exception 'RESTORE_VERIFICATION_FAILED';
 end if;
end $$;
commit;
