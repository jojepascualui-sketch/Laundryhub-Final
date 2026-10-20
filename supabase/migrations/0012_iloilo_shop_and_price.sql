-- 0012 · Real shop: The Laundry Hub Iloilo, and the new price of ₱20 per kilo.
-- Location from the plus code PHJ6+X5C (full code 7Q24PHJ6+X5C) = 10.732437, 122.560391,
-- Cubay, Jaro, Iloilo City 5000. Deliveries are accepted within delivery_radius_km (5 km) of this point.
-- Run once in Supabase → SQL Editor. (The same values can later be changed in Admin → Pricing & soaps.)

update public.shop_settings
   set rate_per_kg  = 20,
       shop_lat     = 10.732437,
       shop_lng     = 122.560391,
       shop_address = 'The Laundry Hub Iloilo, Cubay, Jaro, Iloilo City 5000 (PHJ6+X5C)'
 where id = 1;

-- New tables created later will also start at ₱20.
alter table public.shop_settings alter column rate_per_kg set default 20;

-- Check:
select rate_per_kg, min_kg, delivery_fee, delivery_radius_km, shop_lat, shop_lng, shop_address
  from public.shop_settings;
