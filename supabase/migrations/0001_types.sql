-- 0001 · Enum types used by the Laundry HUB tables.
-- Run order: 0001 → 0010. Paste each file into Supabase → SQL Editor → New query → Run.

create type public.app_role as enum ('CUSTOMER', 'RIDER', 'ADMIN', 'STAFF');

create type public.service_type as enum ('WALK_IN', 'PICKUP_DELIVERY');

create type public.payment_method as enum ('CASH', 'GCASH');

create type public.payment_status as enum ('UNPAID', 'PENDING_VERIFICATION', 'PAID', 'REJECTED');

create type public.order_status as enum (
  'PENDING', 'WASHING', 'READY', 'OUT_FOR_DELIVERY', 'COMPLETED', 'CANCELLED'
);

-- Timeline entries on the Track order screen (07) plus payment events for the admin.
create type public.order_event as enum (
  'PLACED', 'PICKED_UP', 'WASHING', 'READY', 'OUT_FOR_DELIVERY', 'COMPLETED', 'CANCELLED',
  'PENDING', 'PAYMENT_SUBMITTED', 'PAYMENT_VERIFIED', 'PAYMENT_REJECTED', 'PAYMENT_RECEIVED'
);

create type public.job_type as enum ('PICKUP', 'RETURN_DELIVERY');

-- WAITING: return delivery not offered yet (laundry not READY).
-- CANCELLED: the order was cancelled.
create type public.job_status as enum (
  'WAITING', 'AVAILABLE', 'ACCEPTED', 'IN_PROGRESS', 'COMPLETED', 'DECLINED', 'CANCELLED'
);
