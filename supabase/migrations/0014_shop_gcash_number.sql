-- 0014 · The shop's GCash number (customers send their payment here, then enter the reference number).
-- Run once in Supabase → SQL Editor. (Can later be changed in Admin → Pricing & soaps.)

update public.shop_settings
   set gcash_number       = '09242850309',
       gcash_account_name = coalesce(gcash_account_name, 'The Laundry Hub Iloilo'),
       gcash_enabled      = true
 where id = 1;

-- Check:
select gcash_number, gcash_account_name, gcash_enabled from public.shop_settings;
