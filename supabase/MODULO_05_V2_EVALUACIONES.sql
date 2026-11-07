-- Tadeo Conecta · Módulo 05 v2 — evaluaciones por tarjetas (no por estrellas)
-- Ejecutar UNA VEZ en el SQL Editor de Supabase.
--
-- ⚠️ IMPORTANTE: esto reemplaza por completo la tabla professor_ratings
-- anterior (la de 4 categorías 1-5). Si ya hay evaluaciones de prueba
-- guardadas, este script las borra — es intencional, porque la forma de
-- los datos cambia por completo (ya no son 4 enteros, son las respuestas
-- de cada tarjeta). Si necesitas conservarlas, haz un export antes de
-- correr esto.

drop table if exists public.professor_ratings cascade;

create table public.professor_ratings (
  student_id uuid not null references public.profiles(id) on delete cascade,
  professor_id uuid not null references public.profiles(id) on delete cascade,

  -- Las 6 preguntas de tarjetas (claridad, organización, justicia al
  -- calificar, disponibilidad, trato, ambiente). Cada valor es el índice
  -- de la tarjeta elegida: 0 = mejor tarjeta … 3 = peor tarjeta. El peso
  -- en estrellas de cada índice (5 / 4 / 2.5 / 1) se traduce en Kotlin,
  -- no acá — así cambiar la ponderación nunca necesita otra migración.
  category_answers jsonb not null,

  -- Nota obligatoria SOLO para las categorías donde se escogió una de las
  -- DOS tarjetas más críticas (índices 2 y 3) — la UI exige esta nota
  -- antes de dejar enviar. Mínimo 30 caracteres (validado en la UI, ver
  -- RatingViewModel.MIN_COMMENT_LENGTH). NUNCA se muestra a otros
  -- estudiantes — ver comentario de la tabla más abajo.
  category_notes jsonb not null default '{}'::jsonb,

  would_recommend boolean not null,
  -- Ahora obligatoria SIEMPRE, sea "sí" o "no" (antes solo si "no") —
  -- decisión de Juan para que el texto abierto que se llega a mostrar no
  -- sea solo negativo. Mínimo 45 caracteres.
  recommend_reason text not null check (char_length(trim(recommend_reason)) >= 45),
  recommend_for jsonb not null default '[]'::jsonb,

  workload text not null check (workload in ('poca', 'moderada', 'alta')),
  attendance_mandatory boolean not null,
  grading_basis text not null check (grading_basis in ('examenes', 'trabajos', 'participacion')),
  allows_review_of_exams boolean not null,
  class_style text not null check (class_style in ('teorica', 'practica')),

  -- Las dos preguntas abiertas: obligatorias, con un mínimo de caracteres
  -- para que no se puedan "pasar de largo" con una sola letra — mismos
  -- límites que valida la UI (ver RatingViewModel.MIN_TIP_LENGTH y
  -- MIN_COMMENT_LENGTH). El tip pide más detalle (70) porque es lo
  -- principal que otro estudiante va a leer antes de inscribirse.
  tip_comment text not null check (char_length(trim(tip_comment)) >= 70),
  hardest_part_comment text not null check (char_length(trim(hardest_part_comment)) >= 30),

  -- "Reportada" = oculta para todos menos quien la escribió (ver política
  -- de lectura más abajo y report_professor_rating). No hay panel de
  -- administración todavía, así que no hay un estado intermedio de revisión.
  reported boolean not null default false,

  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),

  primary key (student_id, professor_id)
);

comment on table public.professor_ratings is
  'Módulo 05 v2 — una fila = la evaluación de UN estudiante a UN profesor. '
  'category_answers/category_notes son jsonb a propósito: así se puede '
  'agregar o cambiar una pregunta de tarjeta sin otra migración. '
  'PRIVACIDAD (decisión de Juan): category_notes y recommend_reason nunca '
  'se muestran a otros estudiantes en la app — solo tip_comment y '
  'hardest_part_comment se llegan a ver públicamente, y solo cuando el '
  'profesor ya tiene suficientes evaluaciones (umbral del lado de la app). '
  'Así un solo estudiante molesto nunca es la única voz pública sobre un '
  'profesor, y las notas más críticas quedan como insumo privado.';

create index professor_ratings_professor_id_idx on public.professor_ratings (professor_id);

-- updated_at siempre al día cuando el estudiante edita su evaluación.
create or replace function public.set_professor_ratings_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

drop trigger if exists trg_professor_ratings_updated_at on public.professor_ratings;
create trigger trg_professor_ratings_updated_at
before update on public.professor_ratings
for each row execute function public.set_professor_ratings_updated_at();

-- RLS: cualquier autenticado lee las evaluaciones no reportadas (para
-- calcular promedios y ver reseñas); una reportada solo la sigue viendo
-- quien la escribió. Cada estudiante solo puede insertar/editar la suya.
alter table public.professor_ratings enable row level security;

drop policy if exists "read non-reported ratings" on public.professor_ratings;
create policy "read non-reported ratings"
on public.professor_ratings
for select
to authenticated
using (not reported or student_id = auth.uid());

drop policy if exists "insert own rating" on public.professor_ratings;
create policy "insert own rating"
on public.professor_ratings
for insert
to authenticated
with check (student_id = auth.uid());

drop policy if exists "update own rating" on public.professor_ratings;
create policy "update own rating"
on public.professor_ratings
for update
to authenticated
using (student_id = auth.uid())
with check (student_id = auth.uid());

grant select, insert, update on public.professor_ratings to authenticated;

-- Reportar = ocultar de inmediato (ver política de lectura arriba). La RLS
-- normal solo deja a cada estudiante editar SU PROPIA fila, así que
-- reportar la reseña de otro necesita saltarse esa restricción puntual —
-- mismo patrón que redeem_professor_code en FINAL_PATCH.sql.
create or replace function public.report_professor_rating(p_student_id uuid, p_professor_id uuid)
returns void
language plpgsql
security definer
set search_path = public, extensions
as $$
begin
  if auth.uid() is null then
    return;
  end if;

  update public.professor_ratings
  set reported = true
  where student_id = p_student_id and professor_id = p_professor_id;
end;
$$;

revoke all on function public.report_professor_rating(uuid, uuid) from public;
grant execute on function public.report_professor_rating(uuid, uuid) to authenticated;
