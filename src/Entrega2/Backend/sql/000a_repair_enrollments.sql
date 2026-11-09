-- Reparo limitado ao preflight de 08/11/2026. Revisar e aprovar antes do apply.
-- Backup independente é obrigatório; e2_recovery é somente arquivo de recuperação.
begin;
set local lock_timeout='5s';
set local statement_timeout='30s';
lock table public.enrollments in share row exclusive mode;

do $$ begin
 if (select array_agg(attname::text order by attname) from pg_attribute
     where attrelid='public.enrollments'::regclass and attnum>0 and not attisdropped)
    is distinct from array['attended_at','course_id','created_at','id','points_earned','status','user_id']::text[] then
  raise exception 'REPAIR_SCHEMA_CHANGED';
 end if;
 if exists(select 1 from pg_constraint where contype='f' and confrelid='public.enrollments'::regclass)
    or exists(select 1 from pg_trigger where tgrelid='public.enrollments'::regclass and not tgisinternal) then
  raise exception 'REPAIR_DEPENDENCIES_CHANGED';
 end if;
end $$;

create schema if not exists e2_recovery;
revoke all on schema e2_recovery from public,anon,authenticated;
create table if not exists e2_recovery.enrollment_duplicates_20261108(
 original_id uuid primary key,
 canonical_id uuid not null,
 row_data jsonb not null,
 canonical_data jsonb not null,
 archived_at timestamptz not null default clock_timestamp()
);
alter table e2_recovery.enrollment_duplicates_20261108 enable row level security;
revoke all on e2_recovery.enrollment_duplicates_20261108 from public,anon,authenticated;

create temporary table e2_repair_candidates on commit drop as
select e.id as original_id,
 first_value(e.id) over(partition by user_id,course_id order by created_at,id) as canonical_id,
 to_jsonb(e) as row_data,
 first_value(to_jsonb(e)) over(partition by user_id,course_id order by created_at,id) as canonical_data,
 row_number() over(partition by user_id,course_id order by created_at,id) as copy_number,
 count(*) over(partition by user_id,course_id) as copies
from public.enrollments e;

do $$ declare extras integer; groups_count integer; affected integer; before_count bigint; begin
 select count(*) into before_count from public.enrollments;
 select count(*),count(distinct canonical_id) into extras,groups_count
 from e2_repair_candidates where copy_number>1;
 if extras=0 then return; end if;
 -- Escopo exato observado. Nova duplicidade exige outro preflight, não limpeza ampla.
 if extras<>7 or groups_count<>4 then raise exception 'REPAIR_SCOPE_CHANGED'; end if;
 if exists(select 1 from e2_repair_candidates where copies>1 and
   (row_data->>'status' is distinct from 'enrolled'
    or row_data->'attended_at' is distinct from 'null'::jsonb
    or row_data->'points_earned' is distinct from '0'::jsonb
    or row_data->>'user_id' is null or row_data->>'course_id' is null
    or row_data->>'created_at' is null)) then
  raise exception 'REPAIR_CONFLICTING_ENROLLMENT';
 end if;
 if exists(select 1 from e2_recovery.enrollment_duplicates_20261108 a
   full join (select original_id,canonical_id,row_data,canonical_data from e2_repair_candidates where copy_number>1) c
   using(original_id)
   where a.original_id is not null and
    (c.original_id is null or a.canonical_id<>c.canonical_id or a.row_data<>c.row_data or a.canonical_data<>c.canonical_data)) then
  raise exception 'REPAIR_ARCHIVE_CONFLICT';
 end if;
 insert into e2_recovery.enrollment_duplicates_20261108(original_id,canonical_id,row_data,canonical_data)
 select original_id,canonical_id,row_data,canonical_data from e2_repair_candidates where copy_number>1
 on conflict(original_id) do nothing;
 if (select count(*) from e2_recovery.enrollment_duplicates_20261108)<>extras then
  raise exception 'REPAIR_ARCHIVE_COUNT_MISMATCH';
 end if;
 delete from public.enrollments e using e2_recovery.enrollment_duplicates_20261108 a
 where e.id=a.original_id and to_jsonb(e)=a.row_data;
 get diagnostics affected=row_count;
 if affected<>extras or (select count(*) from public.enrollments)<>before_count-extras then
  raise exception 'REPAIR_DELETE_COUNT_MISMATCH';
 end if;
end $$;
create unique index if not exists e2_enrollment_once on public.enrollments(user_id,course_id);
do $$ begin
 if not exists(select 1 from pg_index i where i.indexrelid='public.e2_enrollment_once'::regclass
   and i.indrelid='public.enrollments'::regclass and i.indisunique and i.indisvalid
   and i.indnkeyatts=2 and i.indnatts=2 and i.indpred is null and i.indexprs is null
   and i.indkey[0]=(select attnum from pg_attribute where attrelid=i.indrelid and attname='user_id')
   and i.indkey[1]=(select attnum from pg_attribute where attrelid=i.indrelid and attname='course_id')) then
  raise exception 'REPAIR_INDEX_MISMATCH';
 end if;
end $$;
commit;

-- Resultado sem identificadores pessoais.
select (select count(*) from e2_recovery.enrollment_duplicates_20261108) as archived_copies,
 (select count(*) from (select 1 from public.enrollments group by user_id,course_id having count(*)>1) d) as duplicate_groups;
