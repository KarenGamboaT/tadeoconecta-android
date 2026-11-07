-- Módulo 07 — Notificaciones reales (antes: dos tarjetas de muestra fijas en
-- NotificationsScreen y un punto rojo en la campana que siempre estaba
-- encendido, sin relación con nada). Alcance confirmado con Juan: solo
-- notificaciones DENTRO de la app (tabla + campana con indicador real +
-- pantalla real). Push (notificación del sistema con la app cerrada) queda
-- fuera de este módulo a propósito — necesitaría Firebase Cloud Messaging,
-- que no existe todavía en este proyecto.

create table if not exists public.notifications (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references public.profiles(id) on delete cascade,
  -- Catálogo cerrado de orígenes reales (ver disparadores en
  -- RatingRepositoryImpl.submitRating e ImprovementGoalRepositoryImpl.setActionDone).
  -- Si se agrega un disparador nuevo más adelante, esta lista crece con un
  -- "alter table ... drop constraint ... add constraint ...", no hace falta
  -- otra tabla.
  type text not null check (type in ('new_review', 'review_submitted', 'goal_completed')),
  title text not null,
  body text not null,
  created_at timestamptz not null default now(),
  -- null = no leída. Se marca con mark_all_notifications_read() al abrir la
  -- pantalla de notificaciones (bulk "marcar todas", no una por una).
  read_at timestamptz
);

comment on table public.notifications is
  'Módulo 07 — una fila = una notificación real para un usuario (estudiante o '
  'profesor). Nunca se inserta desde el cliente con un insert directo (no hay '
  'grant de insert): toda creación pasa por create_notification(), porque '
  'varios disparadores notifican a UN USUARIO DISTINTO del que ejecuta la '
  'acción (p.ej. el estudiante que evalúa notifica al profesor).';

create index if not exists notifications_user_id_created_at_idx
  on public.notifications (user_id, created_at desc);

alter table public.notifications enable row level security;

drop policy if exists "select own notifications" on public.notifications;
create policy "select own notifications"
on public.notifications
for select
to authenticated
using (user_id = auth.uid());

-- El único update que hace el cliente es "marcar leída" (ver
-- mark_all_notifications_read más abajo) — esta política existe por si en el
-- futuro se necesita marcar una sola notificación, pero ya protege que nadie
-- pueda tocar una notificación que no es suya.
drop policy if exists "update own notifications" on public.notifications;
create policy "update own notifications"
on public.notifications
for update
to authenticated
using (user_id = auth.uid())
with check (user_id = auth.uid());

-- Sin insert ni delete: se revoca todo y se otorga solo select/update. Crear
-- notificaciones es exclusivo de create_notification (security definer).
grant select, update on public.notifications to authenticated;
revoke insert, delete on public.notifications from authenticated;

-- RPC con security definer (mismo patrón que report_professor_rating /
-- redeem_professor_code): la RLS normal de esta tabla no tiene política de
-- insert para nadie, así que esta es la ÚNICA puerta para crear una
-- notificación, incluso una para uno mismo (confirmación al estudiante).
-- p_type se valida dos veces: el check constraint de la tabla es la garantía
-- real, pero validarlo también acá da un error más claro si algún día se usa
-- mal desde otra parte del código.
create or replace function public.create_notification(p_user_id uuid, p_type text, p_title text, p_body text)
returns void
language plpgsql
security definer
set search_path = public, extensions
as $$
begin
  if auth.uid() is null then
    return;
  end if;
  if p_type not in ('new_review', 'review_submitted', 'goal_completed') then
    raise exception 'tipo de notificación inválido: %', p_type;
  end if;

  insert into public.notifications (user_id, type, title, body)
  values (p_user_id, p_type, p_title, p_body);
end;
$$;

revoke all on function public.create_notification(uuid, text, text, text) from public;
grant execute on function public.create_notification(uuid, text, text, text) to authenticated;

-- Marcar todas como leídas de un solo golpe al abrir la pantalla — no hace
-- falta security definer porque la política de update de arriba ya permite
-- que cada usuario toque sus propias filas; esto solo evita que el reloj del
-- celular (en vez de now() de Postgres) decida qué quedó "leído".
create or replace function public.mark_all_notifications_read()
returns void
language sql
set search_path = public, extensions
as $$
  update public.notifications
  set read_at = now()
  where user_id = auth.uid() and read_at is null;
$$;

revoke all on function public.mark_all_notifications_read() from public;
grant execute on function public.mark_all_notifications_read() to authenticated;

-- Verificación rápida (debe dar: notif_table_exists=1, notif_policies_count=2,
-- create_fn_count=1, mark_read_fn_count=1).
select
  (select count(*) from pg_tables where schemaname = 'public' and tablename = 'notifications') as notif_table_exists,
  (select count(*) from pg_policies where schemaname = 'public' and tablename = 'notifications') as notif_policies_count,
  (select count(*) from pg_proc where proname = 'create_notification') as create_fn_count,
  (select count(*) from pg_proc where proname = 'mark_all_notifications_read') as mark_read_fn_count;
