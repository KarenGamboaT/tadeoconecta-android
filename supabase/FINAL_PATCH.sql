-- Tadeo Conecta · parche final de permisos para la app Android
-- Ejecutar UNA VEZ en Supabase SQL Editor después del esquema inicial.
-- Es idempotente respecto a las policies listadas aquí.

-- Privilegios del Data API (RLS sigue siendo la barrera de seguridad real)
grant select, update on public.profiles to authenticated;
grant select, insert, update on public.student_profiles to authenticated;
grant select, update on public.professor_profiles to authenticated;
grant select on public.subjects to authenticated;
grant select, insert, update, delete on public.professor_subjects to authenticated;

-- Solo un profesor autenticado puede mantener sus relaciones de materias.
drop policy if exists "professor inserts own subjects" on public.professor_subjects;
create policy "professor inserts own subjects"
on public.professor_subjects
for insert
to authenticated
with check (
  professor_id = auth.uid()
  and exists (
    select 1 from public.profiles p
    where p.id = auth.uid() and p.role = 'professor'
  )
);

drop policy if exists "professor updates own subjects" on public.professor_subjects;
create policy "professor updates own subjects"
on public.professor_subjects
for update
to authenticated
using (professor_id = auth.uid())
with check (professor_id = auth.uid());

drop policy if exists "professor deletes own subjects" on public.professor_subjects;
create policy "professor deletes own subjects"
on public.professor_subjects
for delete
to authenticated
using (professor_id = auth.uid());

-- Refuerzo de la función que convierte una cuenta verificada en profesor.
create or replace function public.redeem_professor_code(p_code text)
returns boolean
language plpgsql
security definer
set search_path = public, extensions
as $$
declare
  v_code public.professor_registration_codes;
  v_uid uuid := auth.uid();
begin
  if v_uid is null then
    return false;
  end if;

  select *
  into v_code
  from public.professor_registration_codes
  where active = true
    and uses < max_uses
    and (expires_at is null or expires_at > now())
    and crypt(p_code, code_hash) = code_hash
  order by created_at asc
  limit 1
  for update;

  if not found then
    return false;
  end if;

  update public.professor_registration_codes
  set uses = uses + 1
  where id = v_code.id;

  update public.profiles
  set role = 'professor', updated_at = now()
  where id = v_uid;

  delete from public.student_profiles where user_id = v_uid;

  insert into public.professor_profiles(user_id, is_verified)
  values (v_uid, true)
  on conflict (user_id)
  do update set is_verified = true;

  return true;
end;
$$;

revoke all on function public.redeem_professor_code(text) from public;
grant execute on function public.redeem_professor_code(text) to authenticated;
