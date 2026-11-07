-- Tadeo Conecta · Módulo 08 (resultados del profesor) + Módulo 09 (plan de
-- mejora) — Ejecutar UNA VEZ en el SQL Editor de Supabase, DESPUÉS de
-- MODULO_05_V2_EVALUACIONES.sql.
--
-- Esta migración hace dos cosas:
--
-- 1) CIERRA A NIVEL DE BASE DE DATOS el hueco de privacidad que hasta ahora
--    solo estaba tapado en Kotlin: la política RLS "read non-reported
--    ratings" de professor_ratings deja leer CUALQUIER columna de CUALQUIER
--    fila no reportada a CUALQUIER estudiante autenticado. ReviewsScreen
--    nunca pedía category_notes/recommend_reason así que no se veían en
--    pantalla, pero esas dos columnas SÍ viajaban en la respuesta de
--    Supabase a cada estudiante — cualquiera con un proxy de red las podía
--    leer igual. Ahora se revoca el select de esas dos columnas para el rol
--    authenticated y se exponen solo a través de dos funciones RPC que
--    comprueban fila por fila quién puede verlas:
--      - get_my_rating: el propio estudiante, para precargar su edición.
--      - get_professor_feedback: el propio profesor, para este panel nuevo.
--    Nadie más puede llegar a esas dos columnas por ninguna vía.
--
-- 2) Crea professor_improvement_goals: el "objetivo activo" del plan de
--    mejora (antes vivía solo en memoria en MainShell.kt y se perdía al
--    cerrar la app) ahora persiste por profesor.

-- ───────────────────────────────────────────────────────────────────────
-- 1) Privacidad real de category_notes / recommend_reason
-- ───────────────────────────────────────────────────────────────────────

-- Vuelve a dar acceso de columna "desde cero": todas las columnas de
-- siempre MENOS category_notes y recommend_reason. Quien de verdad necesita
-- esas dos (el propio estudiante o el propio profesor) las obtiene por RPC.
revoke select on public.professor_ratings from authenticated;
grant select (
  student_id, professor_id, category_answers, would_recommend, recommend_for,
  workload, attendance_mandatory, grading_basis, allows_review_of_exams,
  class_style, tip_comment, hardest_part_comment, reported, created_at, updated_at
) on public.professor_ratings to authenticated;

-- Reemplaza el select directo que usaba RatingRepositoryImpl.myRating(): el
-- estudiante autenticado solo recibe SU PROPIA fila para ESE profesor,
-- completa (incluidas las dos columnas ahora restringidas) — así el
-- formulario sigue precargando su nota/justificación al editar.
create or replace function public.get_my_rating(p_professor_id uuid)
returns setof public.professor_ratings
language sql
security definer
set search_path = public, extensions
stable
as $$
  select *
  from public.professor_ratings
  where student_id = auth.uid() and professor_id = p_professor_id;
$$;

revoke all on function public.get_my_rating(uuid) from public;
grant execute on function public.get_my_rating(uuid) to authenticated;

-- Lo que alimenta el panel nuevo (Módulo 08/09): TODAS las evaluaciones del
-- profesor autenticado, completas — incluidas category_notes y
-- recommend_reason, que es justo el dato privado que este módulo existe
-- para mostrarle. No recibe nada de ningún OTRO profesor (auth.uid() fija
-- cuál es "el propio"), y un estudiante normal no puede ejecutar esto para
-- ver feedback ajeno porque siempre se filtra por su propio auth.uid().
create or replace function public.get_professor_feedback()
returns setof public.professor_ratings
language sql
security definer
set search_path = public, extensions
stable
as $$
  select *
  from public.professor_ratings
  where professor_id = auth.uid() and not reported;
$$;

revoke all on function public.get_professor_feedback() from public;
grant execute on function public.get_professor_feedback() to authenticated;

-- ───────────────────────────────────────────────────────────────────────
-- 2) Objetivo de mejora persistente (Módulo 09)
-- ───────────────────────────────────────────────────────────────────────

create table if not exists public.professor_improvement_goals (
  professor_id uuid primary key references public.profiles(id) on delete cascade,
  -- Clave de RatingCategory (ver Rating.kt) sobre la que está trabajando.
  category text not null,
  -- Progreso de las 3 acciones recomendadas para esa categoría; un array
  -- de 3 booleans en jsonb, igual de simple que category_answers.
  actions_done jsonb not null default '[false, false, false]'::jsonb,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

comment on table public.professor_improvement_goals is
  'Módulo 09 — el plan de mejora que el profesor marcó como objetivo activo. '
  'Una fila por profesor (quitar el objetivo borra la fila); se recalcula/ '
  'reemplaza si elige trabajar otra categoría.';

create or replace function public.set_improvement_goal_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_improvement_goal_updated_at on public.professor_improvement_goals;
create trigger trg_improvement_goal_updated_at
before update on public.professor_improvement_goals
for each row execute function public.set_improvement_goal_updated_at();

alter table public.professor_improvement_goals enable row level security;

drop policy if exists "read own goal" on public.professor_improvement_goals;
create policy "read own goal"
on public.professor_improvement_goals
for select
to authenticated
using (professor_id = auth.uid());

drop policy if exists "insert own goal" on public.professor_improvement_goals;
create policy "insert own goal"
on public.professor_improvement_goals
for insert
to authenticated
with check (professor_id = auth.uid());

drop policy if exists "update own goal" on public.professor_improvement_goals;
create policy "update own goal"
on public.professor_improvement_goals
for update
to authenticated
using (professor_id = auth.uid())
with check (professor_id = auth.uid());

drop policy if exists "delete own goal" on public.professor_improvement_goals;
create policy "delete own goal"
on public.professor_improvement_goals
for delete
to authenticated
using (professor_id = auth.uid());

grant select, insert, update, delete on public.professor_improvement_goals to authenticated;
