-- Preparada localmente. Exige preflight, backup e aprovação antes de aplicar.
begin;
set local lock_timeout='5s';
set local statement_timeout='30s';
do $$ begin
 if exists(select 1 from public.attendances group by user_id,activity_id having count(*)>1) then raise exception 'PREFLIGHT_DUPLICATE_ATTENDANCES'; end if;
 if exists(select 1 from public.enrollments group by user_id,course_id having count(*)>1) then raise exception 'PREFLIGHT_DUPLICATE_ENROLLMENTS'; end if;
end $$;
create unique index if not exists e2_attendance_once on public.attendances(user_id,activity_id);
create unique index if not exists e2_enrollment_once on public.enrollments(user_id,course_id);
alter table public.activities add column if not exists is_cancelled boolean not null default false;
revoke insert,update,delete on public.attendances,public.user_roles,public.enrollments from public,anon,authenticated;
-- Privilégios por coluna também podem manter bypass mesmo após REVOKE na tabela.
do $$ declare t text; cols text; r text; begin
 foreach t in array array['attendances','enrollments','user_roles'] loop
  select string_agg(quote_ident(attname),',') into cols from pg_attribute where attrelid=('public.'||t)::regclass and attnum>0 and not attisdropped;
  execute format('revoke insert(%s),update(%s) on public.%I from public,anon,authenticated',cols,cols,t);
  foreach r in array array['anon','authenticated'] loop
   if has_any_column_privilege(r,'public.'||t,'INSERT') or has_any_column_privilege(r,'public.'||t,'UPDATE') or has_table_privilege(r,'public.'||t,'DELETE') then
    raise exception 'PREFLIGHT_INHERITED_WRITE_PRIVILEGE % %',r,t;
   end if;
  end loop;
 end loop;
end $$;
alter table public.courses enable row level security;
alter table public.activities enable row level security;
alter table public.profiles enable row level security;

create or replace function public.e2_is_admin() returns boolean
language sql stable security definer set search_path='' as $$
 select auth.uid() is not null and exists(select 1 from public.user_roles where user_id=auth.uid() and role::text='admin')
$$;
revoke all on function public.e2_is_admin() from public,anon;
grant execute on function public.e2_is_admin() to authenticated;

create table if not exists public.e2_qr_codes(
 id uuid primary key default gen_random_uuid(),activity_id uuid not null references public.activities,
 token_hash text not null unique check(token_hash ~ '^[a-f0-9]{64}$'),opens_at timestamptz not null,
 expires_at timestamptz not null,created_by uuid not null references auth.users,check(expires_at>opens_at));
create table if not exists public.e2_announcements(
 id uuid primary key default gen_random_uuid(),titulo text not null check(length(titulo) between 1 and 120),
 conteudo text not null check(length(conteudo) between 1 and 4000),created_at timestamptz not null default now());
create table if not exists public.e2_notifications(
 id uuid primary key default gen_random_uuid(),user_id uuid references auth.users,
 titulo text not null,conteudo text not null,created_at timestamptz not null default now());
create table if not exists public.e2_notification_reads(
 notification_id uuid references public.e2_notifications,user_id uuid references auth.users,
 read_at timestamptz not null default now(),primary key(notification_id,user_id));
create table if not exists public.e2_messages(
 id uuid primary key default gen_random_uuid(),user_id uuid not null references auth.users,
 remetente text not null check(remetente in ('aluno','equipe')),
 conteudo text not null check(length(conteudo) between 1 and 2000),created_at timestamptz not null default now());
create table if not exists public.e2_profile_questions(
 id uuid primary key default gen_random_uuid(),position int not null unique,enunciado text not null,
 alternatives jsonb not null check(jsonb_typeof(alternatives)='array'));
create table if not exists public.e2_profile_results(
 id uuid primary key default gen_random_uuid(),user_id uuid not null references auth.users,
 classificacao text not null,resumo text not null,answers jsonb not null,created_at timestamptz not null default now());
create index if not exists e2_messages_owner_date on public.e2_messages(user_id,created_at);
create index if not exists e2_notifications_owner_date on public.e2_notifications(user_id,created_at);
create index if not exists e2_results_owner_date on public.e2_profile_results(user_id,created_at);

do $$ declare t text; begin
 foreach t in array array['e2_qr_codes','e2_announcements','e2_notifications','e2_notification_reads','e2_messages','e2_profile_questions','e2_profile_results'] loop
  execute format('alter table public.%I enable row level security',t);
  execute format('revoke all on public.%I from public,anon,authenticated',t);
  if t<>'e2_qr_codes' then execute format('grant select on public.%I to authenticated',t); end if;
 end loop;
end $$;
grant insert on public.e2_announcements to authenticated;
drop policy if exists e2_announcements_read on public.e2_announcements;
create policy e2_announcements_read on public.e2_announcements for select to authenticated using(true);
drop policy if exists e2_announcements_admin on public.e2_announcements;
create policy e2_announcements_admin on public.e2_announcements for insert to authenticated with check(public.e2_is_admin());
drop policy if exists e2_notifications_read on public.e2_notifications;
create policy e2_notifications_read on public.e2_notifications for select to authenticated using(user_id is null or user_id=auth.uid());
drop policy if exists e2_reads_own on public.e2_notification_reads;
create policy e2_reads_own on public.e2_notification_reads for select to authenticated using(user_id=auth.uid());
drop policy if exists e2_messages_read on public.e2_messages;
create policy e2_messages_read on public.e2_messages for select to authenticated using(user_id=auth.uid() or public.e2_is_admin());
drop policy if exists e2_questions_read on public.e2_profile_questions;
create policy e2_questions_read on public.e2_profile_questions for select to authenticated using(true);
drop policy if exists e2_results_read on public.e2_profile_results;
create policy e2_results_read on public.e2_profile_results for select to authenticated using(user_id=auth.uid());
grant select,insert,update on public.courses,public.activities to authenticated;
drop policy if exists e2_courses_admin on public.courses;
create policy e2_courses_admin on public.courses for all to authenticated using(public.e2_is_admin()) with check(public.e2_is_admin());
drop policy if exists e2_activities_admin on public.activities;
create policy e2_activities_admin on public.activities for all to authenticated using(public.e2_is_admin()) with check(public.e2_is_admin());
drop policy if exists e2_attendances_admin_read on public.attendances;
create policy e2_attendances_admin_read on public.attendances for select to authenticated using(public.e2_is_admin());
drop policy if exists e2_profiles_admin_read on public.profiles;
create policy e2_profiles_admin_read on public.profiles for select to authenticated using(public.e2_is_admin());
grant select on public.profiles,public.attendances to authenticated;

create or replace function public.e2_issue_qr(p_activity_id uuid,p_hash text,p_minutes int default 60)
returns jsonb language plpgsql security definer set search_path='' as $$
declare v public.e2_qr_codes;
begin
 if not public.e2_is_admin() then raise exception 'FORBIDDEN'; end if;
 if p_hash is null or p_hash !~ '^[a-f0-9]{64}$' or p_minutes is null or p_minutes<5 or p_minutes>240 then raise exception 'INVALID_INPUT'; end if;
 perform 1 from public.activities where id=p_activity_id and not is_cancelled for share;
 if not found then raise exception 'NOT_FOUND'; end if;
 insert into public.e2_qr_codes(activity_id,token_hash,opens_at,expires_at,created_by)
 values(p_activity_id,p_hash,clock_timestamp(),clock_timestamp()+make_interval(mins=>p_minutes),auth.uid()) returning * into v;
 return jsonb_build_object('activityId',v.activity_id,'opensAt',v.opens_at,'expiresAt',v.expires_at);
end $$;
create or replace function public.e2_check_in(p_token text,p_activity_id uuid default null)
returns jsonb language plpgsql security definer set search_path='' as $$
declare q public.e2_qr_codes; a public.activities; v public.attendances; created boolean;
begin
 if auth.uid() is null then raise exception 'UNAUTHENTICATED'; end if;
 if p_token is null or length(p_token) not between 32 and 256 or p_token !~ '^[A-Za-z0-9_-]+$' or p_activity_id is null then raise exception 'INVALID_INPUT'; end if;
 select * into q from public.e2_qr_codes where token_hash=encode(sha256(convert_to(p_token,'UTF8')),'hex') for share;
 if not found or q.activity_id<>p_activity_id then raise exception 'INVALID_QR'; end if;
 select * into a from public.activities where id=q.activity_id and not is_cancelled for share;
 if not found then raise exception 'NOT_FOUND'; end if;
 perform 1 from public.enrollments where user_id=auth.uid() and course_id=a.course_id and status in ('enrolled','attended') for share;
 if not found then raise exception 'NOT_ENROLLED'; end if;
 if clock_timestamp()<q.opens_at then raise exception 'QR_NOT_ACTIVE'; end if;
 if clock_timestamp()>=q.expires_at then raise exception 'QR_EXPIRED'; end if;
 insert into public.attendances(user_id,activity_id,scanned_at) values(auth.uid(),a.id,clock_timestamp())
 on conflict(user_id,activity_id) do nothing returning * into v;
 created:=found;
 if not created then select * into v from public.attendances where user_id=auth.uid() and activity_id=a.id; end if;
 return jsonb_build_object('attendance',jsonb_build_object('id',v.id,'user_id',v.user_id,'activity_id',v.activity_id,'scanned_at',v.scanned_at),'created',created);
end $$;
create or replace function public.e2_send_message(p_content text,p_user_id uuid default null)
returns jsonb language plpgsql security definer set search_path='' as $$
declare v public.e2_messages; target uuid; sender text;
begin
 if auth.uid() is null then raise exception 'UNAUTHENTICATED'; end if;
 if p_content is null or length(trim(p_content)) not between 1 and 2000 then raise exception 'INVALID_INPUT'; end if;
 if p_user_id is not null then
  if not public.e2_is_admin() then raise exception 'FORBIDDEN'; end if;
  target:=p_user_id; sender:='equipe';
 else target:=auth.uid(); sender:='aluno'; end if;
 insert into public.e2_messages(user_id,remetente,conteudo) values(target,sender,trim(p_content)) returning * into v;
 if sender='equipe' then insert into public.e2_notifications(user_id,titulo,conteudo) values(target,'Resposta da equipe','Sua dúvida recebeu uma resposta. Abra Mensagens.'); end if;
 return to_jsonb(v);
end $$;
create or replace function public.e2_enroll(p_course_id uuid) returns jsonb
language plpgsql security definer set search_path='' as $$
declare c public.courses; e public.enrollments; enrolled_count int;
begin
 if auth.uid() is null then raise exception 'UNAUTHENTICATED'; end if;
 select * into c from public.courses where id=p_course_id and is_active for update;
 if not found then raise exception 'NOT_FOUND'; end if;
 select * into e from public.enrollments where user_id=auth.uid() and course_id=p_course_id;
 if found and e.status in ('enrolled','attended') then return jsonb_build_object('ok',true,'created',false); end if;
 select count(*) into enrolled_count from public.enrollments where course_id=p_course_id and status in ('enrolled','attended');
 if c.total_spots is not null and enrolled_count>=c.total_spots then raise exception 'COURSE_FULL'; end if;
 insert into public.enrollments(user_id,course_id,status) values(auth.uid(),p_course_id,'enrolled')
 on conflict(user_id,course_id) do update set status='enrolled';
 return jsonb_build_object('ok',true,'created',true);
end $$;
create or replace function public.e2_mark_notification_read(p_id uuid) returns jsonb
language plpgsql security definer set search_path='' as $$
begin
 if auth.uid() is null then raise exception 'UNAUTHENTICATED'; end if;
 perform 1 from public.e2_notifications where id=p_id and (user_id is null or user_id=auth.uid());
 if not found then raise exception 'NOT_FOUND'; end if;
 insert into public.e2_notification_reads(notification_id,user_id) values(p_id,auth.uid()) on conflict do nothing;
 return jsonb_build_object('ok',true);
end $$;
create or replace function public.e2_register_certificate(p_user_id uuid,p_course_id uuid,p_url text,p_confirmed boolean)
returns jsonb language plpgsql security definer set search_path='' as $$
declare v public.certificates;
begin
 if not public.e2_is_admin() then raise exception 'FORBIDDEN'; end if;
 if p_confirmed is distinct from true or p_url is null or length(p_url)>2000 or p_url !~ '^https://[^/@[:space:]]+(/[^[:space:]]*)?$' then raise exception 'INVALID_INPUT'; end if;
 perform 1 from public.enrollments where user_id=p_user_id and course_id=p_course_id and status in ('enrolled','attended');
 if not found then raise exception 'NOT_ENROLLED'; end if;
 -- O responsável confirma que o documento foi emitido e que os critérios foram atendidos.
 select * into v from public.certificates where user_id=p_user_id and course_id=p_course_id and certificate_url=p_url limit 1;
 if not found then insert into public.certificates(user_id,course_id,certificate_url,issued_at)
  values(p_user_id,p_course_id,p_url,clock_timestamp()) returning * into v; end if;
 return to_jsonb(v);
end $$;

insert into public.e2_profile_questions(id,position,enunciado,alternatives) values
('10000000-0000-4000-8000-000000000001',1,'Qual atividade mais desperta sua curiosidade?',
 '[{"id":"a","texto":"Resolver desafios de lógica","area":"Tecnologia e análise"},{"id":"b","texto":"Ajudar pessoas a aprender","area":"Pessoas e educação"},{"id":"c","texto":"Criar imagens ou histórias","area":"Criação e comunicação"}]'),
('10000000-0000-4000-8000-000000000002',2,'Em um projeto de grupo, você prefere:',
 '[{"id":"a","texto":"Investigar dados e soluções","area":"Tecnologia e análise"},{"id":"b","texto":"Organizar a colaboração","area":"Pessoas e educação"},{"id":"c","texto":"Apresentar ideias de forma criativa","area":"Criação e comunicação"}]'),
('10000000-0000-4000-8000-000000000003',3,'Que oficina você gostaria de experimentar?',
 '[{"id":"a","texto":"Programação e ciência","area":"Tecnologia e análise"},{"id":"b","texto":"Educação e impacto social","area":"Pessoas e educação"},{"id":"c","texto":"Design e produção de conteúdo","area":"Criação e comunicação"}]') on conflict(id) do nothing;
create or replace function public.e2_submit_profile(p_answers jsonb) returns jsonb
language plpgsql security definer set search_path='' as $$
declare count_q int; matched int; classification text; v public.e2_profile_results;
begin
 if auth.uid() is null then raise exception 'UNAUTHENTICATED'; end if;
 if p_answers is null or jsonb_typeof(p_answers)<>'array' then raise exception 'INVALID_ANSWERS'; end if;
 select count(*) into count_q from public.e2_profile_questions;
 if jsonb_array_length(p_answers)<>count_q or count_q=0 then raise exception 'INVALID_ANSWERS'; end if;
 select count(distinct q.id) into matched from jsonb_array_elements(p_answers) x
 join public.e2_profile_questions q on q.id::text=x->>'questionId'
 cross join lateral jsonb_array_elements(q.alternatives) alt where alt->>'id'=x->>'alternativeId';
 if matched<>count_q then raise exception 'INVALID_ANSWERS'; end if;
 select alt->>'area' into classification from jsonb_array_elements(p_answers) x
 join public.e2_profile_questions q on q.id::text=x->>'questionId'
 cross join lateral jsonb_array_elements(q.alternatives) alt where alt->>'id'=x->>'alternativeId'
 group by alt->>'area' order by count(*) desc,alt->>'area' asc limit 1;
 insert into public.e2_profile_results(user_id,classificacao,resumo,answers) values(auth.uid(),classification,
 'Exploração acadêmica de interesses. Não é diagnóstico psicológico nem decisão profissional. Em empate, a ordem alfabética define a área exibida.',p_answers) returning * into v;
 return jsonb_build_object('id',v.id,'classificacao',v.classificacao,'resumo',v.resumo,'created_at',v.created_at);
end $$;
create or replace function public.e2_notify_content() returns trigger
language plpgsql security definer set search_path='' as $$
begin
 if tg_table_name='courses' then
  if new.is_active then insert into public.e2_notifications(titulo,conteudo) values('Novo curso disponível',new.title); end if;
 elsif tg_table_name='certificates' then
  insert into public.e2_notifications(user_id,titulo,conteudo) values(new.user_id,'Certificado disponível','Consulte seus certificados no aplicativo.');
 elsif tg_table_name='e2_announcements' then
  insert into public.e2_notifications(titulo,conteudo) values(new.titulo,left(new.conteudo,500));
 elsif tg_table_name='activities' then
  if new.activity_date is distinct from old.activity_date or new.is_cancelled is distinct from old.is_cancelled or new.title is distinct from old.title then
   insert into public.e2_notifications(user_id,titulo,conteudo)
   select distinct user_id,'Sua agenda foi atualizada',new.title from public.enrollments
   where course_id=new.course_id and status in ('enrolled','attended');
  end if;
 end if;
 return new;
end $$;
drop trigger if exists e2_course_notification on public.courses;
create trigger e2_course_notification after insert on public.courses for each row execute function public.e2_notify_content();
drop trigger if exists e2_certificate_notification on public.certificates;
create trigger e2_certificate_notification after insert on public.certificates for each row execute function public.e2_notify_content();
drop trigger if exists e2_announcement_notification on public.e2_announcements;
create trigger e2_announcement_notification after insert on public.e2_announcements for each row execute function public.e2_notify_content();
drop trigger if exists e2_activity_notification on public.activities;
create trigger e2_activity_notification after update on public.activities for each row execute function public.e2_notify_content();
revoke all on function public.e2_issue_qr(uuid,text,int),public.e2_check_in(text,uuid),public.e2_send_message(text,uuid),
 public.e2_mark_notification_read(uuid),public.e2_submit_profile(jsonb),public.e2_enroll(uuid),public.e2_register_certificate(uuid,uuid,text,boolean),public.e2_notify_content() from public,anon;
grant execute on function public.e2_issue_qr(uuid,text,int),public.e2_check_in(text,uuid),public.e2_send_message(text,uuid),
 public.e2_mark_notification_read(uuid),public.e2_submit_profile(jsonb),public.e2_enroll(uuid),public.e2_register_certificate(uuid,uuid,text,boolean) to authenticated;
commit;
