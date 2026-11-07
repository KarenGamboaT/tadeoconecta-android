-- Tadeo Conecta · Revisión general de la app completa — privacidad de autor
-- de reseña + freno a reportes abusivos. Ejecutar UNA VEZ en el SQL Editor
-- de Supabase, DESPUÉS de MODULO_08_09_PANEL_PROFESOR.sql.
--
-- Qué arregla:
--
-- 1) student_id de professor_ratings viajaba a CUALQUIER estudiante
--    autenticado que leyera las reseñas públicas (ReviewsScreen) — nunca se
--    mostraba en pantalla, pero sí en la respuesta de red de Supabase. Como
--    profiles.select está abierto a todo authenticated (ver FINAL_PATCH.sql),
--    cualquiera podía cruzar ese id con profiles y saber EXACTAMENTE quién
--    escribió cada comentario — justo lo que el umbral de 5 evaluaciones
--    (Módulo 05) existe para evitar. Se agrega un id propio (uuid) a cada
--    fila y la app lo usa para poder reportar, en vez de student_id — así
--    student_id ya nunca sale de la base de datos para una reseña ajena
--    (RatingRepositoryImpl.PUBLIC_RATING_COLUMNS ya no lo pide).
--
-- 2) report_professor_rating ocultaba una reseña con UN solo click de
--    CUALQUIER estudiante autenticado, sin ningún límite ni registro de
--    quién reportó — un profesor (o un amigo suyo) podía borrar de la vista
--    pública cualquier evaluación negativa real con un solo tap. Ahora cada
--    reporte queda registrado (y un mismo estudiante no puede reportar la
--    misma reseña dos veces) y la reseña solo pasa a "reportada" cuando
--    suficientes estudiantes DISTINTOS la reportaron.

-- ───────────────────────────────────────────────────────────────────────
-- 1) id propio por fila — ya no se expone quién escribió qué
-- ───────────────────────────────────────────────────────────────────────

alter table public.professor_ratings
  add column if not exists id uuid not null default gen_random_uuid();

-- No reemplaza la primary key (student_id, professor_id) — sigue siendo lo
-- que de verdad impide calificar dos veces — pero necesita ser única para
-- poder referenciarla desde professor_rating_reports.
do $$
begin
  if not exists (
    select 1 from pg_constraint where conname = 'professor_ratings_id_key'
  ) then
    alter table public.professor_ratings add constraint professor_ratings_id_key unique (id);
  end if;
end $$;

-- Vuelve a dar acceso de columna "desde cero": todas las de siempre MENOS
-- student_id (y category_notes/recommend_reason, que ya estaban fuera
-- desde MODULO_08_09_PANEL_PROFESOR.sql) — y CON id. get_my_rating y
-- get_professor_feedback siguen haciendo "select *" vía security definer,
-- así que ellas sí siguen trayendo student_id para quien corresponde.
revoke select on public.professor_ratings from authenticated;
grant select (
  id, professor_id, category_answers, would_recommend, recommend_for,
  workload, attendance_mandatory, grading_basis, allows_review_of_exams,
  class_style, tip_comment, hardest_part_comment, reported, created_at, updated_at
) on public.professor_ratings to authenticated;

-- ───────────────────────────────────────────────────────────────────────
-- 2) Reportes registrados + umbral, en vez de "un click = oculta"
-- ───────────────────────────────────────────────────────────────────────

-- Quién reportó qué reseña — nunca legible por un estudiante normal (ni
-- siquiera la propia fila): todo pasa por report_professor_rating. Si el
-- día de mañana hay un panel de administración, esta tabla es su insumo
-- natural para revisar reportes en vez de solo contarlos a ciegas.
create table if not exists public.professor_rating_reports (
  rating_id uuid not null references public.professor_ratings(id) on delete cascade,
  reporter_id uuid not null references public.profiles(id) on delete cascade,
  created_at timestamptz not null default now(),
  primary key (rating_id, reporter_id)
);

comment on table public.professor_rating_reports is
  'Revisión general — un reporte por (reseña, estudiante que reportó). '
  'report_professor_rating() cuenta reporteadores DISTINTOS de una misma '
  'reseña y recién la oculta (reported=true en professor_ratings) al '
  'llegar al umbral — ver esa función para el número exacto.';

alter table public.professor_rating_reports enable row level security;
-- Sin policies de select/update/delete a propósito: ni siquiera quien
-- reportó puede leer esta tabla de vuelta.
revoke all on public.professor_rating_reports from authenticated;

-- Reemplaza la función anterior (student_id, professor_id) por una que
-- recibe el id de la fila — mismo patrón security definer que
-- redeem_professor_code: la RLS normal de professor_rating_reports no deja
-- select/insert a nadie, así que SOLO esta función puede escribir ahí.
create or replace function public.report_professor_rating(p_rating_id uuid)
returns void
language plpgsql
security definer
set search_path = public, extensions
as $$
declare
  v_reporter uuid := auth.uid();
  v_report_count int;
  -- Cuántos estudiantes DISTINTOS tienen que reportar la misma reseña antes
  -- de que se oculte de la vista pública. Súbelo o bájalo acá si hace
  -- falta — es el único lugar que lo define.
  v_threshold int := 3;
begin
  if v_reporter is null then
    return;
  end if;

  insert into public.professor_rating_reports (rating_id, reporter_id)
  values (p_rating_id, v_reporter)
  on conflict (rating_id, reporter_id) do nothing;

  select count(*) into v_report_count
  from public.professor_rating_reports
  where rating_id = p_rating_id;

  if v_report_count >= v_threshold then
    update public.professor_ratings
    set reported = true
    where id = p_rating_id;
  end if;
end;
$$;

-- La firma vieja (student_id, professor_id) ya no la llama nadie desde la
-- app — se elimina para no dejar dos formas de reportar, una de ellas
-- insegura.
drop function if exists public.report_professor_rating(uuid, uuid);

revoke all on function public.report_professor_rating(uuid) from public;
grant execute on function public.report_professor_rating(uuid) to authenticated;

-- ───────────────────────────────────────────────────────────────────────
-- Verificación (debería devolver 1,1,1,1 — correr después de lo de arriba)
-- ───────────────────────────────────────────────────────────────────────
select
  (select count(*) from information_schema.columns
    where table_schema = 'public' and table_name = 'professor_ratings' and column_name = 'id') as id_column_exists,
  (select count(*) from information_schema.tables
    where table_schema = 'public' and table_name = 'professor_rating_reports') as reports_table_exists,
  (select count(*) from pg_proc where proname = 'report_professor_rating'
    and pg_get_function_identity_arguments(oid) = 'p_rating_id uuid') as new_report_fn_exists,
  (select count(*) from pg_proc where proname = 'report_professor_rating'
    and pg_get_function_identity_arguments(oid) = 'p_student_id uuid, p_professor_id uuid') as old_report_fn_gone;
