-- SOMENTE LEITURA. Executar antes de aprovar 001_entrega2.sql e guardar saída sem dados pessoais.
select version(),current_user,current_setting('server_version_num') as version_num;
select table_name,column_name,data_type,is_nullable,column_default
from information_schema.columns where table_schema='public' and table_name in
 ('profiles','user_roles','courses','activities','enrollments','attendances','certificates')
order by table_name,ordinal_position;
select tablename,policyname,roles,cmd,qual,with_check from pg_policies
where schemaname='public' and tablename in ('profiles','user_roles','courses','activities','enrollments','attendances','certificates');
select table_name,grantee,privilege_type from information_schema.role_table_grants
where table_schema='public' and table_name in ('user_roles','attendances','enrollments');
select table_name,column_name,grantee,privilege_type from information_schema.column_privileges
where table_schema='public' and table_name in ('user_roles','attendances','enrollments');
select count(*) as duplicate_attendance_groups from
 (select 1 from public.attendances group by user_id,activity_id having count(*)>1) x;
select count(*) as duplicate_enrollment_groups from
 (select 1 from public.enrollments group by user_id,course_id having count(*)>1) x;
select rolname,rolsuper,rolbypassrls from pg_roles where rolname in ('anon','authenticated');
select pg_get_userbyid(roleid) as inherited_role,pg_get_userbyid(member) as member
from pg_auth_members where pg_get_userbyid(member) in ('anon','authenticated');
