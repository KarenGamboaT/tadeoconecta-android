-- Módulo 09 — historial de metas de mejora del profesor. Antes solo existía
-- la meta ACTIVA (professor_improvement_goals, un solo renglón por
-- profesor): al reiniciarla o quitarla no quedaba ningún registro de las
-- anteriores. Esta tabla es un log de solo-inserción: una fila por cada
-- meta que se reemplaza o se quita, con una foto de cómo quedó
-- (categoría, las 3 acciones y si se alcanzó a completar o no).

create table if not exists public.professor_improvement_goal_history (
  id uuid primary key default gen_random_uuid(),
  professor_id uuid not null references public.profiles(id) on delete cascade,
  -- Mismo texto plano que professor_improvement_goals.category (RatingCategory.key).
  category text not null,
  actions_done jsonb not null,
  -- Foto de si las 3 acciones estaban en true justo antes de archivar esta
  -- fila — así "Historial de metas" puede distinguir una meta que de verdad
  -- se completó de una que se cambió o se quitó a medio camino.
  completed boolean not null,
  archived_at timestamptz not null default now()
);

comment on table public.professor_improvement_goal_history is
  'Módulo 09 — un renglón por cada vez que una meta de professor_improvement_goals '
  'se reemplaza o se quita. Se escribe desde ImprovementGoalRepositoryImpl.setGoal/'
  'clearGoal, justo antes de upsert/delete sobre la meta activa. Inmutable: sin '
  'policy de update ni de delete, cada profesor solo ve y agrega las suyas.';

create index if not exists professor_improvement_goal_history_professor_id_idx
  on public.professor_improvement_goal_history (professor_id, archived_at desc);

alter table public.professor_improvement_goal_history enable row level security;

drop policy if exists "select own goal history" on public.professor_improvement_goal_history;
create policy "select own goal history"
on public.professor_improvement_goal_history
for select
to authenticated
using (professor_id = auth.uid());

-- No hace falta security definer: cada profesor solo archiva SU PROPIA meta
-- (nunca la de otro), así que un insert normal con check professor_id =
-- auth.uid() ya es suficiente — mismo nivel de confianza que
-- professor_improvement_goals.
drop policy if exists "insert own goal history" on public.professor_improvement_goal_history;
create policy "insert own goal history"
on public.professor_improvement_goal_history
for insert
to authenticated
with check (professor_id = auth.uid());

-- Sin update ni delete para nadie: un historial que se puede editar después
-- no sirve como historial.
grant select, insert on public.professor_improvement_goal_history to authenticated;
revoke update, delete on public.professor_improvement_goal_history from authenticated;

-- Verificación rápida (debe dar: history_table_exists=1, history_policies_count=2).
select
  (select count(*) from pg_tables where schemaname = 'public' and tablename = 'professor_improvement_goal_history') as history_table_exists,
  (select count(*) from pg_policies where schemaname = 'public' and tablename = 'professor_improvement_goal_history') as history_policies_count;
