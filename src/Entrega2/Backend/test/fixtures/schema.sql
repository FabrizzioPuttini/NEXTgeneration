-- Fixture sintética mínima: não é export do banco real.
create role anon;
create role authenticated;
create schema auth;
create function auth.uid() returns uuid language sql stable as $$
 select nullif(current_setting('request.jwt.claim.sub', true),'')::uuid
$$;
grant usage on schema public,auth to anon,authenticated;
grant execute on function auth.uid() to anon,authenticated;
create table auth.users(id uuid primary key);
create table public.profiles(id uuid primary key references auth.users,full_name text,school text);
create table public.user_roles(id uuid primary key default gen_random_uuid(),user_id uuid references auth.users,role text);
create table public.courses(id uuid primary key default gen_random_uuid(),title text,description text,location text,course_date timestamptz,workload int,meetings_count int,total_spots int default 30,is_active boolean default true);
create table public.activities(id uuid primary key default gen_random_uuid(),course_id uuid references courses,title text,activity_date timestamptz);
create table public.enrollments(id uuid primary key default gen_random_uuid(),user_id uuid references auth.users,course_id uuid references courses,status text,created_at timestamptz default now());
create table public.attendances(id uuid primary key default gen_random_uuid(),user_id uuid references auth.users,activity_id uuid references activities,scanned_at timestamptz default now());
create table public.certificates(id uuid primary key default gen_random_uuid(),user_id uuid references auth.users,course_id uuid references courses,certificate_url text,issued_at timestamptz default now());
alter table attendances enable row level security;
create policy attendances_select_own on attendances for select to authenticated using(auth.uid()=user_id);
create policy attendances_insert_own on attendances for insert to authenticated with check(auth.uid()=user_id);
grant select,insert,update,delete on attendances to public,anon,authenticated;
alter table user_roles enable row level security;
create policy roles_select_own on user_roles for select to authenticated using(auth.uid()=user_id);
grant select on user_roles to authenticated;
alter table courses enable row level security;
alter table activities enable row level security;
alter table profiles enable row level security;
alter table enrollments enable row level security;
alter table certificates enable row level security;
create policy courses_select_authenticated on courses for select to authenticated using(true);
create policy activities_select_authenticated on activities for select to authenticated using(true);
create policy profiles_select_own on profiles for select to authenticated using(auth.uid()=id);
create policy profiles_insert_own on profiles for insert to authenticated with check(auth.uid()=id);
create policy enrollments_select_own on enrollments for select to authenticated using(auth.uid()=user_id);
create policy enrollments_insert_own on enrollments for insert to authenticated with check(auth.uid()=user_id);
create policy certificates_select_own on certificates for select to authenticated using(auth.uid()=user_id);
grant select on courses,activities,enrollments,certificates,profiles to authenticated;
grant insert on profiles,enrollments to authenticated;
