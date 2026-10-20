# Laundry HUB — UI Design Specification

This folder is the **source of truth for every screen** in Laundry HUB.
Anyone building the app (including Claude Code) must match it.

- `screens/*.png` — a screenshot of every screen (phone screens at 2x, 390×844 pt; admin at 1440 px wide).
- `prototype/*.html` — the same screens as clickable HTML. Open `prototype/index.html` in a browser.
  The HTML inline styles give **exact** colors, sizes, radii and spacing — read them when a value is unclear.
- `source/*.dc.html` — the original design files (the HTML prototype is generated from these).
- `screens.json` — machine-readable list of all screens.

Sample names, prices and orders are **placeholder data**. Text in square brackets
(`[SHOP ADDRESS]`, `[RIDER NAME]`, `[PLATE NO.]`) must come from the database.

---

## 1. Design tokens

### Colors
| Token | Hex | Use |
|---|---|---|
| `bg` | `#F3F5F2` | App background |
| `surface` | `#FFFFFF` | Cards, inputs, bottom bars |
| `ink` | `#14212B` | Main text; dark headers (rider, admin sidebar); secondary dark buttons |
| `muted` | `#4A5863` | Secondary text, labels |
| `subtle` | `#6B7780` | Disabled / upcoming text |
| `accent` | `#1E63A8` | Primary buttons, selected states, progress, links |
| `accentTint` | `#E4EEF8` | Icon tiles, selected-card background is `#F2F7FC` |
| `border` | `#DCE2DD` | Unselected card borders; inputs use `#C8D0CA` |
| `divider` | `#E1E6E2` | Lines between rows |
| `warnBg` / `warnText` | `#FFF1E8` / `#8A3A12` | 8 kg minimum notice, "Collect cash", Return delivery badge |
| `pin` | `#E0662A` | Map pin |
| `successBg` / `successText` | `#E6F2EA` / `#1D5E36` | Paid, Completed, Active, Cash icon |
| `onDuty` | `#1D7A4A` | Rider "On duty" switch |
| `disabled` | `#9AA5AD` | Disabled primary button |

Status badge colors (background / text):
Pending `#EEF1EE`/`#3A4651` · Washing `#E4EEF8`/`#174B80` · Ready `#DFF3F0`/`#0B5B54` ·
Out for delivery `#FFF1E8`/`#8A3A12` · Completed `#E6F2EA`/`#1D5E36`.
Badges always show the status **text**, never color alone.

### Typography
- Headings & big numbers: **Bricolage Grotesque** (700–800). Fallback: any bold sans.
- Body: **DM Sans** (400/500/700). Fonts are in `prototype/assets/fonts/files/` (OFL licensed).
- Sizes (phone): screen title 21, hero 30–32, section title 16–17, body 14–15, caption 12–13, totals 22–28, kilo number 48.
- On Android use `res/font/` with these files; in JavaFX load them with `Font.loadFont`.

### Shape & spacing
- Phone screen: 390×844 design size, side padding **22 px**.
- Card radius 18–20, big hero card 24, inputs 14, chips 14, pill buttons fully rounded.
- Touch targets **≥ 44 px** (primary buttons 52–54 px tall).
- Gap between stacked cards 12–14 px; between sections 18–22 px.
- Icons: simple outline (stroke 2) icons, no emoji.

### Money & rules (shown in UI, enforced by the server)
- Price per kg default **₱30**, delivery fee **₱60** (delivery only), **minimum 8 kg**.
- Soap add-ons per kg: Standard ₱0 (shown "Included"), Hypoallergenic ₱5, Premium Scented ₱8, Baby-Safe ₱6.
- Money format: `₱1,234.00` (two decimals, comma thousands).

---

## 2. Navigation map

```
Sign in ─┬─ Customer ─▶ Home ─▶ Service ─▶ Weight & soap ─▶ Location/Schedule ─▶ Review & pay ─▶ Order placed ─▶ Home
         │                 └─ Orders tab ─▶ Track order
         ├─ Rider ────▶ Available bookings ─▶ (Accept) Active job ─▶ Job completed ─▶ Bookings
         └─ Admin ────▶ Admin console (Dashboard · Orders · Pricing & soaps · Customers & riders · Sales reports)
Sign in ◀─▶ Sign up
```

---

## 3. Screens

### Sign in & sign up
**00a-sign-in — Sign in.** Blue header (accent) with logo tile, "Welcome back", subtitle.
Segmented control **Customer / Rider / Admin** (changes heading, email label and where Sign in goes).
Email, Password, Remember me, Forgot password?, primary **Sign in**, divider "or",
**Continue with Google** (customers only), "New to Laundry HUB? Sign up".

**00b-sign-up — Create account.** Full name, Mobile number (09XXXXXXXXX), Email, Password,
Confirm password, Terms checkbox (mentions location use), **Create account**, "Already have an account? Sign in".

**00c-admin-sign-in.** Same screen with Admin selected: "Admin sign in", "Staff email", no Google button.
(In the real build, the admin console is the JavaFX app; this phone version only routes admins there.)

### Customer app
**01-home — Home.** Logo + "Laundry HUB", bell button. Greeting "Magandang araw!", hero "Fresh clothes, zero hassle."
Blue **New order** card with "Start laundry order" (white pill). Two tiles: "Wash · Dry · Fold ₱30 /kg" and
"Minimum load 8 kg" (warn colors). "How it works" 3 numbered rows. Bottom nav: Home (active), Orders, Profile.

**Order wizard (screens 02–05) shared layout:** back button, "Step N of 4", step title, 6 px progress bar
(25/50/75/100 %). Scrollable content. **Sticky bottom bar**: "Estimated total" + live amount on the left,
primary button on the right ("Continue", last step "Place order"). Button is disabled (`#9AA5AD`) until the step is valid.

**02-choose-service.** Two large selectable cards with icon tile, title, description, fee line, radio dot:
Walk-in ("No extra fee") and Pickup & Delivery ("+ ₱60 delivery fee"). Selected = accent border + `#F2F7FC` bg.

**03-weight-and-soap.** Weight card: "Laundry weight", "₱30 per kg", round − / + buttons around a big kilo number,
"kilograms". **− is disabled at 8.** Warn note: "Minimum of 8 kg per order. Below 8 kg is charged as 8 kg."
"Fabric soap": 2×2 grid of soap cards (drop icon, name, description, "+ ₱X /kg" or "Included").

**04a-pin-location-delivery (delivery).** Hint text, **map** (250 px tall, rounded) — tap moves the orange pin;
chip at bottom-left "Tap anywhere to pin" → "Location pinned". "Address details" input
(house no., street, barangay, landmark). "Pickup time": 2×2 time chips. Continue needs a pin **and** a time.
(Real app: Google Maps with draggable marker + "Use my current location" GPS button.)

**04b-drop-off-walk-in (walk-in).** Shop card: "Drop off at Laundry HUB", `[SHOP ADDRESS]`, `Open [SHOP HOURS]`.
"Drop-off time" chips.

**05-review-and-pay.** "Order summary" card: Service; Laundry (kg × ₱rate); soap (kg × add-on); Pickup & delivery fee;
divider; **Total** (big, accent). "Payment method": Cash ("Pay the rider upon delivery" / "Pay at the counter")
and GCash ("Pay instantly from your e-wallet"). GCash shows "GCash mobile number" input.

**06-order-placed.** Big accent check circle, "Order placed!", "Order #LH-0001 · service". Summary card
(Weight, Fabric soap, Schedule, Payment, Total). Dark full-width "Back to home".

**07-track-order.** Back, "Order #…", "Track your laundry". Map with shop marker, rider marker (accent),
customer pin and dashed route; chip "Rider is on the way · live GPS". Rider card (initials, `[RIDER NAME]`,
`[PLATE NO.]`, call button). "Order status" vertical timeline: Order placed → Picked up → Washing & drying →
Out for delivery → Delivered (done = filled accent with check, current = accent ring, future = grey).

### Rider app
**R1-rider-bookings.** Dark header: "Laundry HUB Rider", "Hi, [RIDER NAME]", **On duty / Off duty** switch,
three counters (Available, Accepted, Done today). Tabs **Available (n) / My jobs (n)**.
Booking card: type badge (**Pickup** blue / **Return delivery** orange), "order no. · distance",
pin icon + customer + address + "slot · kg", payment strip ("Cash on return · order total" /
"Collect cash" / "Paid via GCash" + amount), buttons **Decline** (outline) and **Accept booking** (accent).
Accepted cards show "Start job" / "Continue job". Empty and off-duty states exist.

**R2-rider-pickup-job / R3-rider-delivery-cash — Active job.** Top 330 px map with route, back button,
"Navigate in Google Maps" dark pill. Sheet below: type badge + order/distance, customer card with call button,
step checklist, payment box. Bottom: one big accent button whose label follows the step.
- Pickup steps: Head to customer → Arrived at pickup point → Laundry picked up & weighed → Dropped off at shop.
  Buttons: Start trip → I've arrived → Confirm pickup → Drop off at shop.
- Return delivery steps: Collect laundry at shop → On the way to customer → Arrived at drop-off →
  **Cash collected** (cash orders only) → Delivered. Buttons: Picked up at shop → Start trip → I've arrived →
  Cash received → Mark as delivered.

**R4-rider-job-done.** Check circle, "Job completed", job line, summary row, dark "Back to bookings".
**R5-rider-off-duty.** Switch off; list replaced by "You're off duty — Go on duty to receive new pickup and delivery bookings."

### Admin console (desktop, JavaFX)
Layout: **dark sidebar** (`#14212B`, ~230 px): logo, "Admin console", nav (Dashboard, Orders, Pricing & soaps,
Customers & riders, Sales reports; active item has `#24333F` bg + accent dot), admin profile + Log out.
Main area: kicker + page title (30 px), search field, bell.

**A1-admin-dashboard.** 4 summary cards (Orders today, Waiting to process, Kilos in today, Sales today — last one accent).
"Order pipeline": one tile per status with count. "Recent orders" table (Order, Customer, Service, Kg, Payment,
Total, Status badge, Open button) + "View all".

**A2-admin-orders.** Status filter chips with counts. Left: order list cards (id, status badge, customer,
"service · kg · total"; selected = accent border). Right detail panel: id + customer + phone, status badge;
4 info tiles (Service, Weight, Fabric soap, Schedule); **Pinned location** map + address + coordinates +
"Open in Google Maps" (delivery only; walk-in shows a note); **Update status** buttons (current one filled accent);
**Assign rider** dropdown; price breakdown and "Total · payment (paid/unpaid)".

**A3-admin-pricing.** Rates card: Price per kilo, Minimum load (kg) with hint, Pickup & delivery fee,
Delivery radius (km) with hint. Fabric soaps table (Name, Add-on per kg, Available checkbox) + "+ Add soap".
Payment methods: Cash, GCash checkboxes, Shop GCash number. **Save changes**.

**A4-admin-users.** Segmented tabs Customers / Riders / Staff, add button (label changes), table
(Name, Contact, Orders/Vehicle/Role, Status badge, Edit).

**A5-admin-reports.** Cards: Cash sales, GCash sales, Delivery vs walk-in. "Sales by fabric soap" horizontal bars.
"Export CSV".

---

## 4. Rules for whoever builds it
1. Before building a screen, open its PNG **and** its HTML file; copy text, order, colors and sizes.
2. Same labels and wording as the design (English, with "Magandang araw!" on Home).
3. Every interactive element in the prototype must work in the real app (selection, disabled states, live total).
4. Add loading, empty and error states that keep the same style (not shown in the mockups).
5. After building, compare your screen side by side with the PNG and fix differences.
