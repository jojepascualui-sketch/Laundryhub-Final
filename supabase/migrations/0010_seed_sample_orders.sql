-- 0010 · OPTIONAL sample data matching screen A2 (orders LH-0001 … LH-0006).
-- Creates six sample customers (they cannot sign in: no password) and their orders.
-- Run it once, after 0009, on a fresh database. It does nothing if the samples exist.
--
-- To remove the samples later, run:
--   delete from public.orders where customer_id in
--     (select id from public.profiles where email like '%@sample.laundryhub.test');
--   delete from auth.users where email like '%@sample.laundryhub.test';

do $$
declare
  c record;
  v_order_id bigint;
  v_soap record;
  s public.shop_settings;
begin
  if exists (select 1 from auth.users where email like '%@sample.laundryhub.test') then
    raise notice 'Sample data already present, skipping.';
    return;
  end if;

  select * into s from public.shop_settings where id = 1;

  for c in
    select * from (values
      -- name,             mobile,        service,          kg, soap,              pay,     pay_status,    status,             slot,                   lat,     lng,      address
      ('Maria Santos',    '09170001021', 'PICKUP_DELIVERY', 10, 'Hypoallergenic',  'GCASH', 'PAID',         'OUT_FOR_DELIVERY', 'Today, 2–4 PM',        14.5935, 120.9850, 'Brgy. San Isidro'),
      ('Juan Dela Cruz',  '09180003340', 'WALK_IN',          8, 'Standard',        'CASH',  'UNPAID',       'WASHING',          'Today, morning',       null,    null,     null),
      ('Ana Reyes',       '09270005582', 'PICKUP_DELIVERY', 12, 'Premium Scented', 'CASH',  'UNPAID',       'PENDING',          'Today, 4–6 PM',        14.5978, 120.9786, 'Brgy. Poblacion'),
      ('Mark Villanueva', '09190007714', 'WALK_IN',          9, 'Baby-Safe',       'GCASH', 'PENDING_VERIFICATION', 'READY',    'Today, afternoon',     null,    null,     null),
      ('Liza Gonzaga',    '09170008812', 'PICKUP_DELIVERY',  8, 'Standard',        'GCASH', 'PAID',         'COMPLETED',        'Today, 9–11 AM',       14.5852, 120.9902, 'Brgy. Mabini'),
      ('Paolo Tan',       '09190006650', 'PICKUP_DELIVERY', 15, 'Hypoallergenic',  'CASH',  'UNPAID',       'PENDING',          'Tomorrow, 9–11 AM',    14.6011, 120.9877, 'Brgy. Bagong Silang')
    ) as t(name, mobile, service, kg, soap, pay, pay_status, status, slot, lat, lng, address)
  loop
    -- Sample customer (profiles row is created by the on_auth_user_created trigger).
    insert into auth.users (
      instance_id, id, aud, role, email, encrypted_password, email_confirmed_at,
      raw_app_meta_data, raw_user_meta_data, created_at, updated_at,
      confirmation_token, recovery_token, email_change_token_new, email_change
    ) values (
      '00000000-0000-0000-0000-000000000000', gen_random_uuid(), 'authenticated', 'authenticated',
      lower(replace(c.name, ' ', '.')) || '@sample.laundryhub.test', '', now(),
      '{"provider":"email","providers":["email"]}'::jsonb,
      jsonb_build_object('full_name', c.name, 'mobile', c.mobile),
      now(), now(), '', '', '', ''
    );

    select * into v_soap from public.soaps where name = c.soap;

    insert into public.orders (
      customer_id, service_type, kg, soap_id, soap_name, time_slot,
      lat, lng, address, address_details,
      payment_method, payment_status, gcash_number, gcash_reference,
      rate_per_kg, soap_add_on_per_kg, laundry_cost, soap_cost, delivery_fee, total, status
    )
    select p.id, c.service::public.service_type, c.kg, v_soap.id, v_soap.name, c.slot,
           c.lat, c.lng, c.address, null,
           c.pay::public.payment_method, c.pay_status::public.payment_status,
           case when c.pay = 'GCASH' then c.mobile end,
           case when c.pay = 'GCASH' then '10' || substr(c.mobile, 3) || '00' end,
           s.rate_per_kg, v_soap.add_on_per_kg,
           c.kg * s.rate_per_kg, c.kg * v_soap.add_on_per_kg,
           case when c.service = 'PICKUP_DELIVERY' then s.delivery_fee else 0 end,
           c.kg * s.rate_per_kg + c.kg * v_soap.add_on_per_kg
             + case when c.service = 'PICKUP_DELIVERY' then s.delivery_fee else 0 end,
           c.status::public.order_status
    from public.profiles p
    where p.mobile = c.mobile
    returning id into v_order_id;

    -- Timeline so far.
    insert into public.order_status_history (order_id, event, status)
    values (v_order_id, 'PLACED', 'PENDING');

    if c.service = 'PICKUP_DELIVERY' and c.status <> 'PENDING' then
      insert into public.order_status_history (order_id, event, status)
      values (v_order_id, 'PICKED_UP', 'PENDING');
    end if;
    if c.status in ('WASHING', 'READY', 'OUT_FOR_DELIVERY', 'COMPLETED') then
      insert into public.order_status_history (order_id, event, status) values (v_order_id, 'WASHING', 'WASHING');
    end if;
    if c.status in ('READY', 'OUT_FOR_DELIVERY', 'COMPLETED') then
      insert into public.order_status_history (order_id, event, status) values (v_order_id, 'READY', 'READY');
    end if;
    if c.status in ('OUT_FOR_DELIVERY', 'COMPLETED') and c.service = 'PICKUP_DELIVERY' then
      insert into public.order_status_history (order_id, event, status)
      values (v_order_id, 'OUT_FOR_DELIVERY', 'OUT_FOR_DELIVERY');
    end if;
    if c.status = 'COMPLETED' then
      insert into public.order_status_history (order_id, event, status) values (v_order_id, 'COMPLETED', 'COMPLETED');
    end if;

    -- Rider jobs for delivery orders.
    if c.service = 'PICKUP_DELIVERY' then
      insert into public.rider_jobs (order_id, type, status, current_step, completed_at)
      values (
        v_order_id, 'PICKUP',
        case when c.status = 'PENDING' then 'AVAILABLE' else 'COMPLETED' end::public.job_status,
        case when c.status = 'PENDING' then 0 else 4 end,
        case when c.status = 'PENDING' then null else now() end
      );
      insert into public.rider_jobs (order_id, type, status, current_step, completed_at)
      values (
        v_order_id, 'RETURN_DELIVERY',
        case c.status
          when 'COMPLETED' then 'COMPLETED'
          when 'OUT_FOR_DELIVERY' then 'AVAILABLE'
          when 'READY' then 'AVAILABLE'
          else 'WAITING'
        end::public.job_status,
        case when c.status = 'COMPLETED' then 4 else 0 end,
        case when c.status = 'COMPLETED' then now() end
      );
    end if;
  end loop;
end;
$$;
