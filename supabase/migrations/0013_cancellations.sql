-- 0013 · Cancel buttons for customers and riders.
--   rpc cancel_order(p_order_id, p_reason)  — customer cancels their own order while it is still
--       PENDING and the laundry has not been picked up (walk-in: before the shop received it).
--   rpc release_job(p_job_id, p_reason)     — rider gives a job back before taking the laundry
--       (pickup: before "Confirm pickup"; return delivery: before "Picked up at shop").
--       The job becomes AVAILABLE again for other riders and stays hidden from this rider.
-- Run once in Supabase → SQL Editor (after 0001–0012).

alter table public.orders add column if not exists cancel_reason text;
alter table public.orders add column if not exists cancelled_at timestamptz;

create or replace function public.cancel_order(p_order_id bigint, p_reason text default null)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_order public.orders;
  v_reason text := nullif(trim(coalesce(p_reason, '')), '');
begin
  select * into v_order from public.orders where id = p_order_id for update;
  if not found or v_order.customer_id is distinct from auth.uid() then
    raise exception 'Order not found.' using errcode = 'PT404';
  end if;
  if v_order.status = 'CANCELLED' then
    raise exception 'This order is already cancelled.' using errcode = 'PT409';
  end if;
  if v_order.status <> 'PENDING' then
    raise exception 'This order can no longer be cancelled because the shop is already working on it. Please contact the shop.'
      using errcode = 'PT409';
  end if;
  if exists (
    select 1 from public.rider_jobs j
    where j.order_id = v_order.id and j.type = 'PICKUP' and (j.current_step >= 3 or j.status = 'COMPLETED')
  ) then
    raise exception 'The rider already picked up your laundry, so the order can no longer be cancelled. Please contact the shop.'
      using errcode = 'PT409';
  end if;

  update public.orders
     set cancel_reason = 'Cancelled by customer' || coalesce(': ' || v_reason, ''),
         cancelled_at = now(),
         status = 'CANCELLED'          -- trigger writes the history row and closes the rider jobs
   where id = v_order.id
  returning * into v_order;

  return jsonb_build_object('id', v_order.id, 'order_no', v_order.order_no, 'status', v_order.status,
                            'payment_method', v_order.payment_method, 'payment_status', v_order.payment_status);
end;
$$;

create or replace function public.release_job(p_job_id bigint, p_reason text default null)
returns jsonb
language plpgsql
security definer
set search_path = public
as $$
declare
  v_job public.rider_jobs;
begin
  select * into v_job from public.rider_jobs where id = p_job_id for update;
  if not found or v_job.rider_id is distinct from auth.uid() then
    raise exception 'This is not your job.' using errcode = 'PT403';
  end if;
  if v_job.status not in ('ACCEPTED', 'IN_PROGRESS') then
    raise exception 'This job is already finished.' using errcode = 'PT409';
  end if;
  if (v_job.type = 'PICKUP' and v_job.current_step >= 3)
     or (v_job.type = 'RETURN_DELIVERY' and v_job.current_step >= 1) then
    raise exception 'You already have the laundry, so this job can no longer be cancelled. Please contact the shop.'
      using errcode = 'PT409';
  end if;

  update public.rider_jobs
     set status = case
                    when type = 'RETURN_DELIVERY'
                         and (select status from public.orders where id = order_id) not in ('READY', 'OUT_FOR_DELIVERY')
                      then 'WAITING'
                    else 'AVAILABLE'
                  end::public.job_status,
         rider_id = null,
         current_step = 0,
         accepted_at = null,
         version = version + 1
   where id = v_job.id
  returning * into v_job;

  -- Do not offer the same job to this rider again.
  insert into public.rider_job_declines (job_id, rider_id) values (v_job.id, auth.uid())
  on conflict do nothing;

  -- Unassign the rider from the order unless they still hold another open job for it.
  update public.orders o
     set rider_id = null
   where o.id = v_job.order_id
     and o.rider_id = auth.uid()
     and not exists (
       select 1 from public.rider_jobs j
       where j.order_id = o.id and j.rider_id = auth.uid() and j.status in ('ACCEPTED', 'IN_PROGRESS')
     );

  insert into public.order_status_history (order_id, event, status, note, changed_by)
  select o.id, o.status::text::public.order_event, o.status,
         'Rider cancelled the ' || case when v_job.type = 'PICKUP' then 'pickup' else 'delivery' end
           || coalesce(': ' || nullif(trim(coalesce(p_reason, '')), ''), '') || ' — waiting for another rider',
         auth.uid()
    from public.orders o where o.id = v_job.order_id;

  return to_jsonb(v_job);
end;
$$;

revoke execute on function public.cancel_order(bigint, text) from public, anon;
revoke execute on function public.release_job(bigint, text) from public, anon;
grant execute on function public.cancel_order(bigint, text) to authenticated;
grant execute on function public.release_job(bigint, text) to authenticated;
