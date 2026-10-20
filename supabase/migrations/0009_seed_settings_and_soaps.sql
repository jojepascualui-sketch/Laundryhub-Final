-- 0009 · Default shop settings and the four fabric soaps (DESIGN_SPEC.md §1).
-- Change the shop location, address, hours and GCash details in Admin → Pricing & soaps
-- (or here before running). The location below is a placeholder (Manila City Hall).

insert into public.shop_settings (
  id, rate_per_kg, min_kg, delivery_fee, delivery_radius_km,
  shop_lat, shop_lng, shop_address, shop_hours,
  gcash_number, gcash_account_name, cash_enabled, gcash_enabled
) values (
  1, 30, 8, 60, 5,
  14.5896, 120.9811, '[SHOP ADDRESS]', '8:00 AM – 8:00 PM',
  null, null, true, true
)
on conflict (id) do nothing;

insert into public.soaps (name, description, add_on_per_kg, available, sort_order) values
  ('Standard',        'Everyday clean for regular clothes', 0, true, 1),
  ('Hypoallergenic',  'Gentle on sensitive skin',           5, true, 2),
  ('Premium Scented', 'Long-lasting fresh fragrance',       8, true, 3),
  ('Baby-Safe',       'Mild and dye-free formula',          6, true, 4)
on conflict (name) do nothing;
