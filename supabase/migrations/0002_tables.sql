-- 0002 · Tables.
-- Money is numeric(10,2) in pesos. Weights are whole kilograms (the UI stepper moves by 1 kg).

-- One row per signed-up user (created by the trigger in 0003).
create table public.profiles (
  id          uuid primary key references auth.users (id) on delete cascade,
  full_name   text not null default '',
  mobile      text check (mobile is null or mobile ~ '^09[0-9]{9}$'),
  email       text,
  role        public.app_role not null default 'CUSTOMER',
  job_title   text,                         -- Staff tab "Role" column, e.g. Owner, Counter staff
  active      boolean not null default true, -- Status badge on screen A4
  created_at  timestamptz not null default now()
);

-- Single row (id = 1) with everything the admin edits on screen A3.
create table public.shop_settings (
  id                  smallint primary key default 1 check (id = 1),
  rate_per_kg         numeric(10,2) not null default 30   check (rate_per_kg > 0),
  min_kg              integer       not null default 8    check (min_kg > 0),
  delivery_fee        numeric(10,2) not null default 60   check (delivery_fee >= 0),
  delivery_radius_km  numeric(6,2)  not null default 5    check (delivery_radius_km > 0),
  shop_lat            double precision not null,
  shop_lng            double precision not null,
  shop_address        text not null default '',
  shop_hours          text not null default '',
  gcash_number        text check (gcash_number is null or gcash_number ~ '^09[0-9]{9}$'),
  gcash_account_name  text,
  cash_enabled        boolean not null default true,
  gcash_enabled       boolean not null default true,
  updated_at          timestamptz not null default now()
);

create table public.soaps (
  id             bigint generated always as identity primary key,
  name           text not null unique,
  description    text not null default '',
  add_on_per_kg  numeric(10,2) not null default 0 check (add_on_per_kg >= 0),
  available      boolean not null default true,
  sort_order     integer not null default 0,
  created_at     timestamptz not null default now()
);

-- Order numbers LH-0001, LH-0002, ...
create sequence public.order_no_seq;

create table public.orders (
  id                 bigint generated always as identity primary key,
  order_no           text not null unique
                       default 'LH-' || lpad(nextval('public.order_no_seq')::text, 4, '0'),
  customer_id        uuid not null references public.profiles (id) on delete restrict,
  service_type       public.service_type not null,
  kg                 integer not null check (kg > 0),      -- charged kg (never below min_kg)
  soap_id            bigint not null references public.soaps (id),
  soap_name          text not null,                         -- snapshot for history/reports
  time_slot          text not null,
  lat                double precision,
  lng                double precision,
  address            text,
  address_details    text,
  payment_method     public.payment_method not null,
  payment_status     public.payment_status not null default 'UNPAID',
  gcash_number       text,                                  -- customer's GCash number (screen 05)
  gcash_reference    text check (gcash_reference is null or gcash_reference ~ '^[0-9]{13}$'),
  gcash_proof_path   text,                                  -- path inside bucket gcash-proofs
  rate_per_kg        numeric(10,2) not null,                -- snapshot of the price used
  soap_add_on_per_kg numeric(10,2) not null,
  laundry_cost       numeric(10,2) not null,
  soap_cost          numeric(10,2) not null,
  delivery_fee       numeric(10,2) not null,
  total              numeric(10,2) not null,
  status             public.order_status not null default 'PENDING',
  rider_id           uuid references public.profiles (id) on delete set null,
  created_at         timestamptz not null default now(),
  updated_at         timestamptz not null default now(),
  constraint delivery_needs_pin check (
    service_type = 'WALK_IN' or (lat is not null and lng is not null)
  )
);

create index orders_customer_idx on public.orders (customer_id, created_at desc);
create index orders_status_idx   on public.orders (status);
create index orders_created_idx  on public.orders (created_at desc);
create index orders_rider_idx    on public.orders (rider_id);

create table public.order_status_history (
  id          bigint generated always as identity primary key,
  order_id    bigint not null references public.orders (id) on delete cascade,
  event       public.order_event not null,
  status      public.order_status not null,   -- order status right after the event
  note        text,
  changed_by  uuid references public.profiles (id) on delete set null,
  created_at  timestamptz not null default now()
);

create index order_status_history_order_idx on public.order_status_history (order_id, created_at);

-- One row per rider account (created automatically when a profile becomes RIDER).
create table public.riders (
  profile_id  uuid primary key references public.profiles (id) on delete cascade,
  plate_no    text,
  on_duty     boolean not null default false,
  last_lat    double precision,
  last_lng    double precision,
  last_seen   timestamptz
);

create table public.rider_jobs (
  id            bigint generated always as identity primary key,
  order_id      bigint not null references public.orders (id) on delete cascade,
  type          public.job_type not null,
  status        public.job_status not null,
  rider_id      uuid references public.profiles (id) on delete set null,
  current_step  integer not null default 0 check (current_step >= 0), -- steps completed so far
  accepted_at   timestamptz,
  completed_at  timestamptz,
  version       integer not null default 0,
  created_at    timestamptz not null default now(),
  unique (order_id, type)
);

create index rider_jobs_status_idx on public.rider_jobs (status);
create index rider_jobs_rider_idx  on public.rider_jobs (rider_id);

-- "Decline" hides an available job from that rider only; other riders still see it.
create table public.rider_job_declines (
  job_id      bigint not null references public.rider_jobs (id) on delete cascade,
  rider_id    uuid not null references public.profiles (id) on delete cascade,
  declined_at timestamptz not null default now(),
  primary key (job_id, rider_id)
);
