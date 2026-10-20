-- 0006 · Rider jobs: accept, decline and advance step by step.
--
-- current_step = number of steps completed. Steps (DESIGN_SPEC.md, screens R2/R3):
--   PICKUP           1 Head to customer · 2 Arrived at pickup point ·
--                    3 Laundry picked up & weighed · 4 Dropped off at shop
--   RETURN_DELIVERY  1 Collect laundry at shop · 2 On the way to customer ·
--                    3 Arrived at drop-off · 4 Cash collected (cash orders only) · last Delivered
-- Order effects:
--   PICKUP step 3      → history PICKED_UP
--   PICKUP done        → order WASHING
--   RETURN step 1      → order OUT_FOR_DELIVERY
--   RETURN cash step   → payment PAID (history PAYMENT_RECEIVED)
--   RETURN done        → order COMPLETED

create or replace function public.job_total_steps(
  p_type public.job_type,
  p_payment_method public.payment_method
)
returns integer
language sql
immutable
as $$
  select case
    when p_type = 'PICKUP' then 4
    when p_payment_method = 'CASH' then 5
    else 4
  end;
$$;

-- rpc accept_job: only one rider can win. The UPDATE only matches while the job is
-- still AVAILABLE, so a second rider gets "This booking was already taken." (HTTP 409).
create or replace function public.accept_job(p_job_id bigint)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_job public.rider_jobs;
begin
  if not public.is_on_duty_rider() then
    raise exception 'Go on duty to accept bookings.' using errcode = 'PT403';
  end if;

  update public.rider_jobs
     set status = 'ACCEPTED',
         rider_id = auth.uid(),
         accepted_at = now(),
         current_step = 0,
         version = version + 1
   where id = p_job_id
     and status = 'AVAILABLE'
  returning * into v_job;

  if not found then
    raise exception 'This booking was already taken.' using errcode = 'PT409';
  end if;

  update public.orders set rider_id = auth.uid() where id = v_job.order_id;

  return to_jsonb(v_job);
end;
$$;

-- rpc decline_job: hides an available job from this rider only.
create or replace function public.decline_job(p_job_id bigint)
returns void
language plpgsql
security definer
set search_path = public
as $$
begin
  if public.current_app_role() is distinct from 'RIDER' then
    raise exception 'Only riders can decline bookings.' using errcode = 'PT403';
  end if;
  if not exists (select 1 from public.rider_jobs where id = p_job_id and status = 'AVAILABLE') then
    raise exception 'This booking is no longer available.' using errcode = 'PT409';
  end if;

  insert into public.rider_job_declines (job_id, rider_id)
  values (p_job_id, auth.uid())
  on conflict do nothing;
end;
$$;

-- rpc advance_job: completes the next step of the rider's own job.
-- Returns the job plus total_steps, done and the order's status/payment.
create or replace function public.advance_job(p_job_id bigint)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_job    public.rider_jobs;
  v_order  public.orders;
  v_total  integer;
  v_step   integer;
  v_cash   boolean;
begin
  select * into v_job from public.rider_jobs where id = p_job_id for update;
  if not found or v_job.rider_id is distinct from auth.uid() then
    raise exception 'This is not your job.' using errcode = 'PT403';
  end if;
  if v_job.status not in ('ACCEPTED', 'IN_PROGRESS') then
    raise exception 'This job is already finished.' using errcode = 'PT409';
  end if;

  select * into v_order from public.orders where id = v_job.order_id for update;
  if v_order.status = 'CANCELLED' then
    raise exception 'This order was cancelled.' using errcode = 'PT409';
  end if;
  if v_job.type = 'RETURN_DELIVERY' and v_order.status not in ('READY', 'OUT_FOR_DELIVERY') then
    raise exception 'The laundry is not ready yet.' using errcode = 'PT409';
  end if;

  v_cash  := v_order.payment_method = 'CASH';
  v_total := public.job_total_steps(v_job.type, v_order.payment_method);
  v_step  := v_job.current_step + 1;

  if v_job.type = 'PICKUP' then
    if v_step = 3 then
      insert into public.order_status_history (order_id, event, status, changed_by)
      values (v_order.id, 'PICKED_UP', v_order.status, auth.uid());
    elsif v_step = v_total then
      update public.orders set status = 'WASHING' where id = v_order.id and status = 'PENDING';
    end if;
  else
    if v_step = 1 then
      update public.orders set status = 'OUT_FOR_DELIVERY' where id = v_order.id and status = 'READY';
    elsif v_cash and v_step = 4 then
      update public.orders set payment_status = 'PAID' where id = v_order.id;
      insert into public.order_status_history (order_id, event, status, note, changed_by)
      values (v_order.id, 'PAYMENT_RECEIVED', 'OUT_FOR_DELIVERY', 'Cash collected by rider', auth.uid());
    elsif v_step = v_total then
      update public.orders set status = 'COMPLETED' where id = v_order.id;
    end if;
  end if;

  update public.rider_jobs
     set current_step = v_step,
         status = case when v_step >= v_total then 'COMPLETED' else 'IN_PROGRESS' end::public.job_status,
         completed_at = case when v_step >= v_total then now() end,
         version = version + 1
   where id = v_job.id
  returning * into v_job;

  select * into v_order from public.orders where id = v_job.order_id;

  return to_jsonb(v_job) || jsonb_build_object(
    'total_steps', v_total,
    'done', v_job.status = 'COMPLETED',
    'order_status', v_order.status,
    'payment_status', v_order.payment_status
  );
end;
$$;
