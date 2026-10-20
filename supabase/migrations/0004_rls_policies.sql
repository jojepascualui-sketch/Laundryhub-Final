-- 0004 · Row Level Security on every table, plus table grants.
--
-- Summary
--   profiles             own row; staff all; riders see customers of their jobs;
--                        customers see their assigned rider
--   shop_settings, soaps everyone reads; ADMIN writes
--   orders               customers read own (create only through rpc place_order);
--                        riders read orders of jobs they can see; ADMIN/STAFF read + update all
--   order_status_history follows orders; written by triggers / functions
--   riders               own row (duty + GPS); staff all; customers read their active rider
--   rider_jobs           riders read AVAILABLE (on duty) + own; customers read own orders' jobs;
--                        accept/advance only through rpc accept_job / advance_job
--   rider_job_declines   rider reads own; written by rpc decline_job

alter table public.profiles             enable row level security;
alter table public.shop_settings        enable row level security;
alter table public.soaps                enable row level security;
alter table public.orders               enable row level security;
alter table public.order_status_history enable row level security;
alter table public.riders               enable row level security;
alter table public.rider_jobs           enable row level security;
alter table public.rider_job_declines   enable row level security;

-- Grants: the anon role (signed out) may only read prices and soaps.
revoke all on all tables in schema public from anon;
grant select on public.shop_settings, public.soaps to anon;
grant select, insert, update, delete on all tables in schema public to authenticated;
grant usage on all sequences in schema public to authenticated;

-- profiles -------------------------------------------------------------------
create policy profiles_select on public.profiles
  for select to authenticated
  using (public.can_see_profile(id));

create policy profiles_update_own on public.profiles
  for update to authenticated
  using (id = auth.uid())
  with check (id = auth.uid());

create policy profiles_update_admin on public.profiles
  for update to authenticated
  using (public.is_admin())
  with check (public.is_admin());

-- shop_settings --------------------------------------------------------------
create policy shop_settings_read on public.shop_settings
  for select to anon, authenticated
  using (true);

create policy shop_settings_admin_update on public.shop_settings
  for update to authenticated
  using (public.is_admin())
  with check (public.is_admin());

-- soaps ----------------------------------------------------------------------
create policy soaps_read on public.soaps
  for select to anon, authenticated
  using (true);

create policy soaps_admin_insert on public.soaps
  for insert to authenticated
  with check (public.is_admin());

create policy soaps_admin_update on public.soaps
  for update to authenticated
  using (public.is_admin())
  with check (public.is_admin());

create policy soaps_admin_delete on public.soaps
  for delete to authenticated
  using (public.is_admin());

-- orders ---------------------------------------------------------------------
create policy orders_select on public.orders
  for select to authenticated
  using (
    customer_id = auth.uid()
    or public.is_staff()
    or public.rider_can_see_order(id)
  );

-- No insert policy on purpose: customers create orders only through rpc place_order,
-- which recalculates every price on the server.

create policy orders_staff_update on public.orders
  for update to authenticated
  using (public.is_staff())
  with check (public.is_staff());

create policy orders_admin_delete on public.orders
  for delete to authenticated
  using (public.is_admin());

-- order_status_history -------------------------------------------------------
create policy history_select on public.order_status_history
  for select to authenticated
  using (
    public.is_staff()
    or public.rider_can_see_order(order_id)
    or exists (
      select 1 from public.orders o
      where o.id = order_id and o.customer_id = auth.uid()
    )
  );

create policy history_staff_insert on public.order_status_history
  for insert to authenticated
  with check (public.is_staff());

-- riders ---------------------------------------------------------------------
create policy riders_select on public.riders
  for select to authenticated
  using (public.can_see_rider(profile_id));

create policy riders_update_own on public.riders
  for update to authenticated
  using (profile_id = auth.uid())
  with check (profile_id = auth.uid());

create policy riders_staff_update on public.riders
  for update to authenticated
  using (public.is_staff())
  with check (public.is_staff());

create policy riders_staff_insert on public.riders
  for insert to authenticated
  with check (public.is_staff());

-- rider_jobs -----------------------------------------------------------------
create policy rider_jobs_select on public.rider_jobs
  for select to authenticated
  using (
    public.is_staff()
    or public.rider_can_see_job(id)
    or exists (
      select 1 from public.orders o
      where o.id = order_id and o.customer_id = auth.uid()
    )
  );

create policy rider_jobs_staff_update on public.rider_jobs
  for update to authenticated
  using (public.is_staff())
  with check (public.is_staff());

create policy rider_jobs_staff_insert on public.rider_jobs
  for insert to authenticated
  with check (public.is_staff());

-- rider_job_declines ---------------------------------------------------------
create policy declines_select on public.rider_job_declines
  for select to authenticated
  using (rider_id = auth.uid() or public.is_staff());
