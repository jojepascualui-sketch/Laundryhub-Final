-- 0007 · Admin console functions (screens A1–A5). All require ADMIN or STAFF.
-- "Today" and report dates use Philippine time (Asia/Manila).

create or replace function public.require_staff()
returns void
language plpgsql
stable
security definer
set search_path = public
as $$
begin
  if not public.is_staff() then
    raise exception 'Admin or staff access only.' using errcode = 'PT403';
  end if;
end;
$$;

create or replace function public.manila_date(p_ts timestamptz)
returns date
language sql
immutable
as $$
  select (p_ts at time zone 'Asia/Manila')::date;
$$;

-- rpc admin_dashboard (A1): summary cards, order pipeline and recent orders.
-- Pipeline counts every open order, plus orders completed today.
create or replace function public.admin_dashboard()
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_today  date := public.manila_date(now());
  v_result jsonb;
begin
  perform public.require_staff();

  select jsonb_build_object(
    'orders_today', (
      select count(*) from public.orders
      where public.manila_date(created_at) = v_today and status <> 'CANCELLED'),
    'waiting_to_process', (
      select count(*) from public.orders where status = 'PENDING'),
    'kilos_today', (
      select coalesce(sum(kg), 0) from public.orders
      where public.manila_date(created_at) = v_today and status <> 'CANCELLED'),
    'sales_today', (
      select coalesce(sum(total), 0) from public.orders
      where public.manila_date(created_at) = v_today and status <> 'CANCELLED'),
    'pending_gcash', (
      select count(*) from public.orders where payment_status = 'PENDING_VERIFICATION'),
    'pipeline', jsonb_build_object(
      'PENDING',          (select count(*) from public.orders where status = 'PENDING'),
      'WASHING',          (select count(*) from public.orders where status = 'WASHING'),
      'READY',            (select count(*) from public.orders where status = 'READY'),
      'OUT_FOR_DELIVERY', (select count(*) from public.orders where status = 'OUT_FOR_DELIVERY'),
      'COMPLETED',        (select count(*) from public.orders
                           where status = 'COMPLETED' and public.manila_date(updated_at) = v_today)
    ),
    'recent', coalesce((
      select jsonb_agg(r order by r.created_at desc)
      from (
        select o.id, o.order_no, p.full_name as customer_name, o.service_type, o.kg,
               o.payment_method, o.payment_status, o.total, o.status, o.created_at
        from public.orders o
        join public.profiles p on p.id = o.customer_id
        order by o.created_at desc
        limit 8
      ) r
    ), '[]'::jsonb)
  ) into v_result;

  return v_result;
end;
$$;

-- rpc admin_reports (A5): sales between two dates (inclusive), cancelled orders excluded.
create or replace function public.admin_reports(p_from date, p_to date)
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  v_result jsonb;
begin
  perform public.require_staff();
  if p_from is null or p_to is null or p_from > p_to then
    raise exception 'Choose a valid date range.' using errcode = 'PT400';
  end if;

  with o as (
    select * from public.orders
    where status <> 'CANCELLED'
      and public.manila_date(created_at) between p_from and p_to
  )
  select jsonb_build_object(
    'from', p_from,
    'to', p_to,
    'order_count',    (select count(*) from o),
    'total_sales',    (select coalesce(sum(total), 0) from o),
    'kilos',          (select coalesce(sum(kg), 0) from o),
    'cash_sales',     (select coalesce(sum(total), 0) from o where payment_method = 'CASH'),
    'gcash_sales',    (select coalesce(sum(total), 0) from o where payment_method = 'GCASH'),
    'delivery_count', (select count(*) from o where service_type = 'PICKUP_DELIVERY'),
    'walkin_count',   (select count(*) from o where service_type = 'WALK_IN'),
    'by_soap', coalesce((
      select jsonb_agg(jsonb_build_object(
               'soap_id', s.id, 'name', s.name,
               'orders', coalesce(x.orders, 0), 'total', coalesce(x.total, 0))
             order by s.sort_order, s.id)
      from public.soaps s
      left join (
        select soap_id, count(*) as orders, sum(total) as total from o group by soap_id
      ) x on x.soap_id = s.id
    ), '[]'::jsonb)
  ) into v_result;

  return v_result;
end;
$$;

-- rpc admin_users (A4): one list per tab (CUSTOMER, RIDER, or STAFF = ADMIN + STAFF).
create or replace function public.admin_users(p_role public.app_role)
returns jsonb
language plpgsql
stable
security definer
set search_path = public
as $$
begin
  perform public.require_staff();

  return coalesce((
    select jsonb_agg(u order by u.full_name)
    from (
      select p.id, p.full_name, p.mobile, p.email, p.role, p.job_title, p.active, p.created_at,
             (select count(*) from public.orders o where o.customer_id = p.id) as order_count,
             r.plate_no, r.on_duty, r.last_seen
      from public.profiles p
      left join public.riders r on r.profile_id = p.id
      where case
              when p_role in ('ADMIN', 'STAFF') then p.role in ('ADMIN', 'STAFF')
              else p.role = p_role
            end
    ) u
  ), '[]'::jsonb);
end;
$$;

-- rpc admin_assign_rider (A2 "Assign rider"). Null unassigns.
-- Open jobs move to the chosen rider; a waiting return delivery is reserved for them.
create or replace function public.admin_assign_rider(p_order_id bigint, p_rider_id uuid)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  perform public.require_staff();

  if p_rider_id is not null and not exists (
    select 1 from public.profiles where id = p_rider_id and role = 'RIDER' and active
  ) then
    raise exception 'Choose an active rider.' using errcode = 'PT400';
  end if;

  update public.orders set rider_id = p_rider_id where id = p_order_id;
  if not found then
    raise exception 'Order not found.' using errcode = 'PT404';
  end if;

  if p_rider_id is null then
    update public.rider_jobs
       set rider_id = null,
           status = case when status = 'WAITING' then 'WAITING' else 'AVAILABLE' end::public.job_status,
           current_step = 0,
           accepted_at = null,
           version = version + 1
     where order_id = p_order_id and status in ('WAITING', 'AVAILABLE', 'ACCEPTED', 'IN_PROGRESS');
  else
    update public.rider_jobs
       set rider_id = p_rider_id,
           status = case when status = 'AVAILABLE' then 'ACCEPTED' else status end,
           accepted_at = case when status = 'AVAILABLE' then now() else accepted_at end,
           version = version + 1
     where order_id = p_order_id and status in ('WAITING', 'AVAILABLE', 'ACCEPTED', 'IN_PROGRESS');
  end if;
end;
$$;

-- rpc verify_gcash_payment (A2 "Verify payment" / "Reject").
create or replace function public.verify_gcash_payment(
  p_order_id bigint,
  p_approve boolean,
  p_note text default null
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_order public.orders;
begin
  perform public.require_staff();

  update public.orders
     set payment_status = case when p_approve then 'PAID' else 'REJECTED' end::public.payment_status
   where id = p_order_id
     and payment_method = 'GCASH'
     and payment_status in ('PENDING_VERIFICATION', 'REJECTED')
  returning * into v_order;

  if not found then
    raise exception 'This GCash payment is not waiting for verification.' using errcode = 'PT409';
  end if;

  insert into public.order_status_history (order_id, event, status, note, changed_by)
  values (
    v_order.id,
    case when p_approve then 'PAYMENT_VERIFIED' else 'PAYMENT_REJECTED' end::public.order_event,
    v_order.status,
    nullif(trim(p_note), ''),
    auth.uid()
  );
end;
$$;
