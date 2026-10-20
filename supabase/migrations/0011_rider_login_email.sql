-- 0011 · Riders may sign in with their mobile number (sign-in screen: "Rider email or mobile").
-- Supabase signs in with e-mail + password, so the app first asks for the e-mail that belongs
-- to an active RIDER with that mobile. Only riders are looked up; customers and staff are never
-- returned, and the password is still required.

create or replace function public.rider_login_email(p_mobile text)
returns text
language sql
stable
security definer
set search_path = public
as $$
  select email
  from public.profiles
  where role = 'RIDER'
    and active
    and mobile = regexp_replace(coalesce(p_mobile, ''), '[\s-]', '', 'g')
  limit 1;
$$;

revoke execute on function public.rider_login_email(text) from public;
grant execute on function public.rider_login_email(text) to anon, authenticated;
