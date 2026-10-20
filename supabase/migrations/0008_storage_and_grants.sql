-- 0008 · GCash screenshot storage rules and function permissions.

-- Private bucket for payment screenshots (skipped if you already created it in the dashboard).
insert into storage.buckets (id, name, public)
values ('gcash-proofs', 'gcash-proofs', false)
on conflict (id) do nothing;

-- Files live at gcash-proofs/<user id>/<order no>.jpg.
-- A customer uploads/reads only inside their own folder; ADMIN/STAFF read everything.
create policy gcash_proofs_insert_own on storage.objects
  for insert to authenticated
  with check (
    bucket_id = 'gcash-proofs'
    and (storage.foldername(name))[1] = auth.uid()::text
  );

create policy gcash_proofs_update_own on storage.objects
  for update to authenticated
  using (
    bucket_id = 'gcash-proofs'
    and (storage.foldername(name))[1] = auth.uid()::text
  )
  with check (
    bucket_id = 'gcash-proofs'
    and (storage.foldername(name))[1] = auth.uid()::text
  );

create policy gcash_proofs_select on storage.objects
  for select to authenticated
  using (
    bucket_id = 'gcash-proofs'
    and (
      (storage.foldername(name))[1] = auth.uid()::text
      or public.is_staff()
    )
  );

-- Functions: nobody signed out may call them; signed-in users may (each function checks
-- the caller's role itself, and RLS policies need the helpers).
revoke execute on all functions in schema public from public, anon;
grant execute on all functions in schema public to authenticated;
