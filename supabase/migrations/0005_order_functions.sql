-- 0005 · Prices, placing orders, GCash payment and order-status triggers.
-- Every price is calculated here from shop_settings and soaps; prices sent by the
-- browser are never used.
--
-- Errors: functions raise readable messages (shown to the user as-is).
-- SQLSTATE 'PT4xx' makes PostgREST answer with that HTTP status (e.g. PT409 → 409).

-- Great-circle distance in km (Haversine).
create or replace function public.distance_km(
  lat1 double precision, lng1 double precision,
  lat2 double precision, lng2 double precision
)
returns double precision
language sql
immutable
as $$
  select 2 * 6371 * asin(sqrt(
    power(sin(radians(lat2 - lat1) / 2), 2)
    + cos(radians(lat1)) * cos(radians(lat2)) * power(sin(radians(lng2 - lng1) / 2), 2)
  ));
$$;

-- Price breakdown: kg × rate + kg × soap add-on + delivery fee (delivery only).
-- Below min_kg is charged as min_kg.
create or replace function public.calculate_price(
  p_service_type public.service_type,
  p_kg integer,
  p_soap_id bigint
)
returns table (
  charged_kg         integer,
  rate_per_kg        numeric,
  soap_name          text,
  soap_add_on_per_kg numeric,
  laundry_cost       numeric,
  soap_cost          numeric,
  delivery_fee       numeric,
  total              numeric
)
language plpgsql
stable
security definer
set search_path = public
as $$
declare
  s     public.shop_settings;
  soap  public.soaps;
  v_kg  integer;
  v_fee numeric;
begin
  if p_service_type is null then
    raise exception 'Choose a service.' using errcode = 'PT400';
  end if;
  if p_kg is null or p_kg <= 0 then
    raise exception 'Enter the laundry weight in kg.' using errcode = 'PT400';
  end if;

  select * into s from public.shop_settings where id = 1;
  if not found then
    raise exception 'Shop settings are missing. Ask the admin to run the seed file.' using errcode = 'PT500';
  end if;

  select * into soap from public.soaps where id = p_soap_id and available;
  if not found then
    raise exception 'That fabric soap is not available.' using errcode = 'PT400';
  end if;

  v_kg  := greatest(p_kg, s.min_kg);
  v_fee := case when p_service_type = 'PICKUP_DELIVERY' then s.delivery_fee else 0 end;

  charged_kg         := v_kg;
  rate_per_kg        := s.rate_per_kg;
  soap_name          := soap.name;
  soap_add_on_per_kg := soap.add_on_per_kg;
  laundry_cost       := round(v_kg * s.rate_per_kg, 2);
  soap_cost          := round(v_kg * soap.add_on_per_kg, 2);
  delivery_fee       := v_fee;
  total              := laundry_cost + soap_cost + delivery_fee;
  return next;
end;
$$;

-- rpc quote_order: the live estimate shown in the order wizard.
create or replace function public.quote_order(
  p_service_type public.service_type,
  p_kg integer,
  p_soap_id bigint
)
returns jsonb
language sql
stable
security definer
set search_path = public
as $$
  select to_jsonb(p) from public.calculate_price(p_service_type, p_kg, p_soap_id) p;
$$;

-- rpc place_order: validates everything, prices it, creates the order, its first
-- history entry and (for delivery) the PICKUP + RETURN_DELIVERY rider jobs.
create or replace function public.place_order(
  p_service_type    public.service_type,
  p_kg              integer,
  p_soap_id         bigint,
  p_time_slot       text,
  p_payment_method  public.payment_method,
  p_lat             double precision default null,
  p_lng             double precision default null,
  p_address         text default null,
  p_address_details text default null,
  p_gcash_reference text default null,
  p_gcash_number    text default null
)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  s        public.shop_settings;
  price    record;
  v_order  public.orders;
  v_ref    text := nullif(regexp_replace(coalesce(p_gcash_reference, ''), '\s', '', 'g'), '');
  v_number text := nullif(regexp_replace(coalesce(p_gcash_number, ''), '\s', '', 'g'), '');
  v_dist   double precision;
begin
  if auth.uid() is null then
    raise exception 'Please sign in to place an order.' using errcode = 'PT401';
  end if;
  if public.current_app_role() is distinct from 'CUSTOMER' then
    raise exception 'Only customer accounts can place orders.' using errcode = 'PT403';
  end if;
  if coalesce(trim(p_time_slot), '') = '' then
    raise exception 'Choose a time slot.' using errcode = 'PT400';
  end if;

  select * into s from public.shop_settings where id = 1;
  select * into price from public.calculate_price(p_service_type, p_kg, p_soap_id);

  -- Payment
  if p_payment_method = 'CASH' and not s.cash_enabled then
    raise exception 'Cash payment is turned off right now.' using errcode = 'PT400';
  end if;
  if p_payment_method = 'GCASH' then
    if not s.gcash_enabled then
      raise exception 'GCash payment is turned off right now.' using errcode = 'PT400';
    end if;
    if v_ref is null or v_ref !~ '^[0-9]{13}$' then
      raise exception 'Enter the 13-digit GCash reference number.' using errcode = 'PT400';
    end if;
    if v_number is not null and v_number !~ '^09[0-9]{9}$' then
      raise exception 'Enter your GCash number as 09XXXXXXXXX.' using errcode = 'PT400';
    end if;
  else
    v_ref := null;
    v_number := null;
  end if;

  -- Location
  if p_service_type = 'PICKUP_DELIVERY' then
    if p_lat is null or p_lng is null then
      raise exception 'Pin your location on the map.' using errcode = 'PT400';
    end if;
    if p_lat not between -90 and 90 or p_lng not between -180 and 180 then
      raise exception 'The pinned location is not valid.' using errcode = 'PT400';
    end if;
    v_dist := public.distance_km(s.shop_lat, s.shop_lng, p_lat, p_lng);
    if v_dist > s.delivery_radius_km then
      raise exception 'Sorry, your pin is % km away. We deliver within % km of the shop.',
        round(v_dist::numeric, 1), trim_scale(s.delivery_radius_km)
        using errcode = 'PT400';
    end if;
  end if;

  insert into public.orders (
    customer_id, service_type, kg, soap_id, soap_name, time_slot,
    lat, lng, address, address_details,
    payment_method, payment_status, gcash_number, gcash_reference,
    rate_per_kg, soap_add_on_per_kg, laundry_cost, soap_cost, delivery_fee, total
  ) values (
    auth.uid(), p_service_type, price.charged_kg, p_soap_id, price.soap_name, trim(p_time_slot),
    case when p_service_type = 'PICKUP_DELIVERY' then p_lat end,
    case when p_service_type = 'PICKUP_DELIVERY' then p_lng end,
    case when p_service_type = 'PICKUP_DELIVERY' then nullif(trim(p_address), '') end,
    case when p_service_type = 'PICKUP_DELIVERY' then nullif(trim(p_address_details), '') end,
    p_payment_method,
    case when p_payment_method = 'GCASH' then 'PENDING_VERIFICATION' else 'UNPAID' end::public.payment_status,
    v_number, v_ref,
    price.rate_per_kg, price.soap_add_on_per_kg, price.laundry_cost, price.soap_cost,
    price.delivery_fee, price.total
  )
  returning * into v_order;

  insert into public.order_status_history (order_id, event, status, changed_by)
  values (v_order.id, 'PLACED', v_order.status, auth.uid());

  if p_payment_method = 'GCASH' then
    insert into public.order_status_history (order_id, event, status, note, changed_by)
    values (v_order.id, 'PAYMENT_SUBMITTED', v_order.status, 'Ref ' || v_ref, auth.uid());
  end if;

  if p_service_type = 'PICKUP_DELIVERY' then
    insert into public.rider_jobs (order_id, type, status)
    values (v_order.id, 'PICKUP', 'AVAILABLE'),
           (v_order.id, 'RETURN_DELIVERY', 'WAITING');
  end if;

  return jsonb_build_object(
    'id', v_order.id,
    'order_no', v_order.order_no,
    'status', v_order.status,
    'payment_status', v_order.payment_status,
    'kg', v_order.kg,
    'soap_name', v_order.soap_name,
    'time_slot', v_order.time_slot,
    'laundry_cost', v_order.laundry_cost,
    'soap_cost', v_order.soap_cost,
    'delivery_fee', v_order.delivery_fee,
    'total', v_order.total
  );
end;
$$;

-- rpc attach_gcash_proof: saves the screenshot path after the upload to
-- gcash-proofs/<user id>/<order no>.jpg.
create or replace function public.attach_gcash_proof(p_order_id bigint, p_path text)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if p_path is null or split_part(p_path, '/', 1) <> auth.uid()::text then
    raise exception 'The screenshot must be inside your own folder.' using errcode = 'PT403';
  end if;

  update public.orders
     set gcash_proof_path = p_path
   where id = p_order_id
     and customer_id = auth.uid()
     and payment_method = 'GCASH';

  if not found then
    raise exception 'Order not found.' using errcode = 'PT404';
  end if;
end;
$$;

-- rpc resubmit_gcash: after a rejection ("Rejected — please resend").
create or replace function public.resubmit_gcash(
  p_order_id bigint,
  p_reference text,
  p_gcash_number text default null
)
returns void
language plpgsql
security definer
set search_path = public
as $$
declare
  v_ref    text := regexp_replace(coalesce(p_reference, ''), '\s', '', 'g');
  v_number text := nullif(regexp_replace(coalesce(p_gcash_number, ''), '\s', '', 'g'), '');
  v_order  public.orders;
begin
  if v_ref !~ '^[0-9]{13}$' then
    raise exception 'Enter the 13-digit GCash reference number.' using errcode = 'PT400';
  end if;
  if v_number is not null and v_number !~ '^09[0-9]{9}$' then
    raise exception 'Enter your GCash number as 09XXXXXXXXX.' using errcode = 'PT400';
  end if;

  update public.orders
     set gcash_reference = v_ref,
         gcash_number = coalesce(v_number, gcash_number),
         payment_status = 'PENDING_VERIFICATION'
   where id = p_order_id
     and customer_id = auth.uid()
     and payment_method = 'GCASH'
     and payment_status in ('REJECTED', 'PENDING_VERIFICATION')
  returning * into v_order;

  if not found then
    raise exception 'This payment cannot be changed.' using errcode = 'PT409';
  end if;

  insert into public.order_status_history (order_id, event, status, note, changed_by)
  values (v_order.id, 'PAYMENT_SUBMITTED', v_order.status, 'Ref ' || v_ref, auth.uid());
end;
$$;

-- Trigger: every order status change is written to order_status_history, and the
-- rider jobs follow the order (READY opens the return delivery; CANCELLED closes jobs).
create or replace function public.on_order_status_change()
returns trigger
language plpgsql
security definer
set search_path = public
as $$
begin
  if new.status is distinct from old.status then
    insert into public.order_status_history (order_id, event, status, changed_by)
    values (new.id, new.status::text::public.order_event, new.status, auth.uid());

    if new.status = 'READY' then
      -- Offered to all on-duty riders, or straight to the rider the admin assigned.
      update public.rider_jobs
         set status = case when rider_id is null then 'AVAILABLE' else 'ACCEPTED' end::public.job_status,
             accepted_at = case when rider_id is null then null else now() end,
             version = version + 1
       where order_id = new.id and type = 'RETURN_DELIVERY' and status = 'WAITING';
    elsif old.status = 'READY' and new.status in ('PENDING', 'WASHING') then
      update public.rider_jobs
         set status = 'WAITING', version = version + 1
       where order_id = new.id and type = 'RETURN_DELIVERY' and status = 'AVAILABLE';
    elsif new.status = 'CANCELLED' then
      update public.rider_jobs
         set status = 'CANCELLED', version = version + 1
       where order_id = new.id and status not in ('COMPLETED', 'CANCELLED');
    end if;
  end if;
  return new;
end;
$$;

create trigger orders_status_change
  after update of status on public.orders
  for each row execute function public.on_order_status_change();
