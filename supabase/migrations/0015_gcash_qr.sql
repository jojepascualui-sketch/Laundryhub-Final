-- 0015 · GCash QR Ph code. The admin uploads the shop's QR (GCash app → Receive → QR → Save)
-- in Admin → Pricing & soaps; customers see it on "Review & pay" and scan/upload it in GCash.
-- Run once in Supabase → SQL Editor (after 0014).

alter table public.shop_settings add column if not exists gcash_qr_path text;

-- Public bucket for shop images (anyone may view; only ADMIN may change). Max 5 MB, images only.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('shop-public', 'shop-public', true, 5242880, array['image/png', 'image/jpeg', 'image/webp'])
on conflict (id) do nothing;

drop policy if exists shop_public_read on storage.objects;
drop policy if exists shop_public_admin_insert on storage.objects;
drop policy if exists shop_public_admin_update on storage.objects;
drop policy if exists shop_public_admin_delete on storage.objects;

create policy shop_public_read on storage.objects
  for select using (bucket_id = 'shop-public');

create policy shop_public_admin_insert on storage.objects
  for insert to authenticated
  with check (bucket_id = 'shop-public' and public.is_admin());

create policy shop_public_admin_update on storage.objects
  for update to authenticated
  using (bucket_id = 'shop-public' and public.is_admin())
  with check (bucket_id = 'shop-public' and public.is_admin());

create policy shop_public_admin_delete on storage.objects
  for delete to authenticated
  using (bucket_id = 'shop-public' and public.is_admin());

-- Check:
select gcash_number, gcash_qr_path from public.shop_settings;
