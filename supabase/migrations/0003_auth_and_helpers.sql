-- 0003 · New-user trigger, role helpers and guard triggers.
--
-- Note: the guide calls the role helper current_role(), but CURRENT_ROLE is a built-in SQL
-- keyword in Postgres, so the helper is named current_app_role().
--
-- The helpers are SECURITY DEFINER so RLS policies can call them without recursion
-- (e.g. an orders policy that looks at rider_jobs whose policy looks at orders).

-- New auth user → profiles row with role CUSTOMER.
-- Sign up sends full_name and mobile in the user metadata; Google sends full_name / name.
create or replace function public.handle_new_user()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
declare
  v_name   text := coalesce(new.raw_user_meta_data ->> 'full_name', new.raw_user_meta_data ->> 'name', '');
  v_mobile text := new.raw_user_meta_data ->> 'mobile';
begin
  if v_mobile is not null and v_mobile !~ '^09[0-9]{9}$' then
    v_mobile := null;  -- never block sign-up over a badly typed number
  end if;

  insert into public.profiles (id, full_name, mobile, email, role)
  values (new.id, v_name, v_mobile, new.email, 'CUSTOMER')
  on conflict (id) do nothing;
  return new;
end;
$$;

create trigger on_auth_user_created
  after insert on auth.users
  for each row execute function public.handle_new_user();

-- Role of the signed-in user (null when signed out or deactivated).
create or replace function public.current_app_role()
returns public.app_role
language sql
stable
security definer
set search_path = public
as $$
  select role from public.profiles where id = auth.uid() and active;
$$;

create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select coalesce(public.current_app_role() = 'ADMIN', false);
$$;

create or replace function public.is_staff()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select coalesce(public.current_app_role() in ('ADMIN', 'STAFF'), false);
$$;

create or replace function public.is_on_duty_rider()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select coalesce(public.current_app_role() = 'RIDER', false)
     and exists (select 1 from public.riders r where r.profile_id = auth.uid() and r.on_duty);
$$;

-- Can the signed-in rider see this job? Own jobs always; AVAILABLE ones while on duty
-- unless this rider declined it.
create or replace function public.rider_can_see_job(p_job_id bigint)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (
    select 1
    from public.rider_jobs j
    where j.id = p_job_id
      and (
        j.rider_id = auth.uid()
        or (
          j.status = 'AVAILABLE'
          and public.is_on_duty_rider()
          and not exists (
            select 1 from public.rider_job_declines d
            where d.job_id = j.id and d.rider_id = auth.uid()
          )
        )
      )
  );
$$;

create or replace function public.rider_can_see_order(p_order_id bigint)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select coalesce(public.current_app_role() = 'RIDER', false)
     and (
       exists (select 1 from public.orders o where o.id = p_order_id and o.rider_id = auth.uid())
       or exists (
         select 1 from public.rider_jobs j
         where j.order_id = p_order_id and public.rider_can_see_job(j.id)
       )
     );
$$;

-- Profiles visible to the signed-in user: own, everyone for staff, customers of jobs a rider
-- can see, and the rider assigned to one of the customer's orders.
create or replace function public.can_see_profile(p_profile_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select p_profile_id = auth.uid()
      or public.is_staff()
      or exists (
           select 1 from public.orders o
           where o.customer_id = p_profile_id and public.rider_can_see_order(o.id)
         )
      or exists (
           select 1 from public.orders o
           where o.customer_id = auth.uid() and o.rider_id = p_profile_id
         );
$$;

-- Rider rows (incl. live GPS) visible to: the rider, staff, and customers with an active
-- order assigned to that rider.
create or replace function public.can_see_rider(p_profile_id uuid)
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select p_profile_id = auth.uid()
      or public.is_staff()
      or exists (
           select 1 from public.orders o
           where o.customer_id = auth.uid()
             and o.rider_id = p_profile_id
             and o.status not in ('COMPLETED', 'CANCELLED')
         );
$$;

-- Only an ADMIN may change role / active / job title. Requests without a signed-in user
-- (SQL Editor, Table Editor, service role) are allowed, so you can make the first ADMIN.
create or replace function public.guard_profile_update()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is not null and not public.is_admin() then
    if new.role is distinct from old.role
       or new.active is distinct from old.active
       or new.job_title is distinct from old.job_title
       or new.email is distinct from old.email then
      raise exception 'Only an admin can change role, status, job title or email'
        using errcode = '42501';
    end if;
  end if;
  if new.id is distinct from old.id then
    raise exception 'Profile id cannot change' using errcode = '42501';
  end if;
  return new;
end;
$$;

create trigger profiles_guard
  before update on public.profiles
  for each row execute function public.guard_profile_update();

-- A profile that becomes RIDER gets a riders row automatically.
create or replace function public.ensure_rider_row()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if new.role = 'RIDER' then
    insert into public.riders (profile_id) values (new.id)
    on conflict (profile_id) do nothing;
  end if;
  return new;
end;
$$;

create trigger profiles_rider_row
  after insert or update of role on public.profiles
  for each row execute function public.ensure_rider_row();

-- Riders may switch duty and send GPS, but only staff may change the plate number.
create or replace function public.guard_rider_update()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if auth.uid() is not null and not public.is_staff() then
    if new.plate_no is distinct from old.plate_no or new.profile_id is distinct from old.profile_id then
      raise exception 'Only staff can change the plate number' using errcode = '42501';
    end if;
  end if;
  return new;
end;
$$;

create trigger riders_guard
  before update on public.riders
  for each row execute function public.guard_rider_update();

create or replace function public.touch_updated_at()
returns trigger
language plpgsql
as $$
begin
  new.updated_at := now();
  return new;
end;
$$;

create trigger orders_touch
  before update on public.orders
  for each row execute function public.touch_updated_at();

create trigger shop_settings_touch
  before update on public.shop_settings
  for each row execute function public.touch_updated_at();
