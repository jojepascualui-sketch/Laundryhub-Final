# Laundry HUB — rules for every Claude Code session

Laundry HUB is a school OOP Java project: **one responsive website** (desktop + tablet + phone),
installable as a PWA, written in Java and compiled to JavaScript with **TeaVM**. Data, login and
files are in **Supabase** (called over its REST API). **Vercel** hosts the static files only.
Development is VS Code on Windows. The build guide is `Laundry_HUB_Web_Java_Supabase_Vercel_Guide.pdf`.

## Rules

1. **JAVA ONLY.** All application logic (models, validation, prices, pages, routing, Supabase
   calls, maps) is Java compiled by TeaVM. The only non-Java files allowed:
   `index.html` (shell), `styles.css`, `manifest.webmanifest`, `sw.js`, icons, fonts,
   `config.js` (generated) and SQL migrations. No JS frameworks, no npm packages in the app.
   Browser APIs are reached through TeaVM JSO interfaces / `@JSBody` in Java.
2. **DESIGN RULE.** `docs/design` is the source of truth and must **not** be edited.
   Before building or changing a screen: open its PNG in `docs/design/screens/`, read its HTML in
   `docs/design/prototype/` (inline styles = exact values; the generated markup lives in
   `docs/design/prototype/assets/sources.js` and `docs/design/source/*.dc.html`), and read its
   section in `docs/design/DESIGN_SPEC.md`. Same text, order, colors, sizes and behavior.
   Follow the **Responsive rules** below. After building, compare with the PNG at 390 px and at
   desktop width, list the differences and fix them.
3. **OOP.** Show Encapsulation, Inheritance, Polymorphism and Abstraction clearly, marked with
   `// OOP: <pillar>` comments. Private fields, validation in constructors, no magic numbers
   (named constants), Javadoc on public methods.
4. **SECURITY.** Only the Supabase **publishable** key (`sb_publishable_…`) goes in the site.
   Every table has Row Level Security. Prices and rider job acceptance are decided in SQL
   functions, never trusted from the browser. Never commit `config.properties`. Never put the
   Supabase secret key, Google OAuth client secret or database password in the project.
5. **Product rules.**
   - Service: **Walk-in** or **Pickup & Delivery**.
   - **Minimum 8 kg** (below 8 kg is charged as 8 kg).
   - Soaps (add-on per kg): Standard +₱0 ("Included"), Hypoallergenic +₱5, Premium Scented +₱8, Baby-Safe +₱6.
   - Price = kg × rate (₱20 at The Laundry Hub Iloilo; design sample ₱30) + kg × soap add-on + delivery fee (₱60, delivery only).
   - Shop: The Laundry Hub Iloilo, Cubay, Jaro, Iloilo City (plus code PHJ6+X5C = 10.732437, 122.560391), see migration 0012.
   - Payment: **Cash** or **GCash** (manual 13-digit reference number; admin verifies).
   - Roles: `CUSTOMER`, `RIDER`, `ADMIN`, `STAFF`.
   - Order statuses: `PENDING`, `WASHING`, `READY`, `OUT_FOR_DELIVERY`, `COMPLETED`, `CANCELLED`.
   - Each delivery order creates a `PICKUP` and a `RETURN_DELIVERY` rider job that on-duty riders
     accept or decline.
   - Money format: `₱1,234.00` (use `com.laundryhub.util.MoneyFormatter`).
6. **Testability.** `model`, `service` and `util` code must not touch browser APIs so JUnit can
   run it on the normal JVM. Browser/JSO code lives in `repository`, `ui`, `ui.pages`, `web`.
7. **End every phase** with the exact commands to build and preview:
   ```
   .\mvnw.cmd clean package      # builds web-dist/
   # open web-dist with Live Server → http://127.0.0.1:5500
   .\mvnw.cmd test               # JUnit
   ```

## Project layout

```
pom.xml                         Maven + TeaVM build (Java ${java.version}, TeaVM 0.16.0)
config.properties               real public keys (gitignored) → web-dist/config.js
config.example.properties       committed template
src/main/java/com/laundryhub/
  web/          App (TeaVM main class)
  model/        domain classes (pure Java)
  service/      business logic (pure Java)
  repository/   Supabase access (JSO)
  ui/           components     ui/pages/  pages
  util/         helpers        exception/ custom exceptions
src/main/webapp/                index.html, styles.css, fonts/ (copied to web-dist/)
src/main/config/config.js       template filled from config.properties at build time
src/test/java/                  JUnit 5
supabase/migrations/            numbered SQL files (paste into Supabase SQL Editor)
docs/design/                    UI design package (read-only)
web-dist/                       build output that Vercel serves (gitignored)
```

Build: `mvnw clean package` runs TeaVM → `web-dist/js/app.js`, copies `src/main/webapp/`,
and generates `web-dist/config.js`. `index.html` calls the exported `main()` on load.

> Note: the machine currently has JDK 17, so `pom.xml` uses `<java.version>17</java.version>`.
> Switch it to 21 after installing JDK 21.

## Java architecture (Phase 2)

- `model` — User → Customer/Rider/Admin(→Staff); ServiceType → WalkInService/PickupDeliveryService;
  Payable + PaymentMethod → CashPayment/GCashPayment; RiderJob → PickupJob/DeliveryJob; Order (Builder),
  LaundryLoad, Location, ShopSettings, Soap, PriceBreakdown, OrderRequest, Booking. Money = `long` centavos.
- `service.PriceCalculator` — live estimate only; the DB price is the real one.
- `repository` — `SupabaseClient` (pure Java on the `HttpClient` interface; auto token refresh, retry on 401),
  `Query` (PostgREST), `ModelMapper`, and `XxxRepository` interfaces + `SupabaseXxxRepository`.
  Browser-only adapters: `XhrHttpClient`, `BrowserSessionStore`.
- `ui` — `Component` (render/mount/unmount/rerender), `Page` (title + allowed roles), `Router` (hash routes,
  role guard), `Responsive` (matchMedia → body `bp-phone|bp-tablet|bp-desktop`), components
  (Button, IconButton, Card, SelectableCard, TextInput, Stepper, Chip, Badge, SegmentedControl, BottomNav,
  TopNav, Sidebar, Toast, Spinner, EmptyState, WizardHeader, BottomBar), `Icons` (SVG from the prototype).
- `web` — `App` (main, routes), `AppContext` (client, repositories, router, current user), `BrowserConfig`.
- Async results use `util.Callback<T>`; `Callbacks.map` chains them.
- `#/dev/components` is the component gallery; unbuilt routes show `PlaceholderPage`.
- Auth (Phase 3): `service.AuthService` (sign in with tab/role check, rider mobile → e-mail via rpc,
  sign up, reset); pages `SignInPage` (`#/signin`, `#/signin/admin`, `#/signin/rider`), `SignUpPage`,
  `ResetPasswordPage`. Google and e-mail links return to `Browser.appUrl()`; `App` finishes the redirect.

- Customer (Phase 4): `CustomerShell` (top nav desktop / bottom nav phone), `HomePage` (`#/home`),
  `OrderWizardPage` (`#/order/1…4`, state in `AppContext.getOrderDraft()` = `service.OrderDraft`),
  `OrderPlacedPage` (`#/order/placed`), `ProfilePage`. Settings + soaps via `service.CatalogCache`.
  Step 3 delivery uses `ui.LocationPicker` (Phase 5).
- Maps (Phase 5): `ui.GoogleMaps` = JSO bindings (Map, Marker, Circle, Polyline, LatLng, fitPoints, click/dragend
  listeners, Geocoder) via `@JSBody`; `GoogleMaps.whenReady(ok, fail)` waits for the async script from index.html
  (fails fast on `gm_authFailure` / script error). `LocationPicker` = tap/drag pin, GPS (`GeoLocator`), reverse
  geocode, radius warning; falls back to the drawn map + GPS when Maps is unavailable. Markers use the classic
  `google.maps.Marker` (works without a Map ID; Google shows a deprecation notice in the console).
- Tracking (Phase 6): `MyOrdersPage` (`#/orders`), `TrackOrderPage` (`#/orders/:id`, map + rider card + timeline,
  `Window.setInterval` 10 s refresh started in onMount, cleared in onUnmount; GCash resend when REJECTED).
  Timeline rules are pure Java in `service.OrderTimeline` (delivery 5 steps, walk-in 4 steps).
- Rider (Phase 6B): `RiderHomePage` (`#/rider`, R1/R5: duty switch, counters, Available/My jobs, 15 s refresh,
  desktop shows the active job's `JobMap` on the right), `RiderJobPage` (`#/rider/jobs/:id`, R2/R3 steps via
  rpc advance_job, R4 done screen). Logic in `service.RiderBoard` (counters, active job, destination per step,
  directions URL). `ui.RiderLocationSharer` watches GPS and PATCHes riders.last_lat/lng/last_seen every 10 s
  while on duty and a rider page is open. `ui.BookingCard`, `ui.JobMap`, `ui.MapArt` are shared.
- Admin (Phase 7): `ui.AdminShell` (sidebar + kicker/title/search/bell). Pages: `AdminDashboardPage` (`#/admin`),
  `AdminOrdersPage` (`#/admin/orders`: chips, list, detail with map, status buttons, cancel, assign rider,
  GCash verify/reject with screenshot, mark cash paid; 30 s refresh), `AdminPricingPage` (`#/admin/pricing`:
  rates, soaps table, payment methods, shop location; ADMIN only saves), `AdminUsersPage` (`#/admin/users`:
  tabs, edit role/status/plate; "+ Add" promotes an existing account found by e-mail), `AdminReportsPage`
  (`#/admin/reports`: periods, KPIs, soap bars, CSV via `service.CsvExporter` + `Browser.download`).
  Pure logic: `service.AdminOrderFilter`, `CsvExporter`, `ReportPeriod`. Search text and selected order live
  in `AppContext` (`adminSearch`, `adminSelectedOrderId`).
- Maps key rejected after load (gm_authFailure): `GoogleMaps.watchAuthFailure` swaps every map to the fallback.
- PWA (Phase 8): `src/main/webapp/manifest.webmanifest`, `sw.js` (network-first app shell, cache-first fonts/icons,
  never cross-origin; bump `CACHE` if an old version sticks), `icons/` (192/512, maskable, apple-touch, favicon).
  `ui.PwaInstall` registers the SW and keeps `beforeinstallprompt`; `ui.InstallCard` shows "Install app"
  (or iPhone "Share → Add to Home Screen") on Home, Sign in, Profile, Rider and Admin dashboard.
  Docs: `docs/DESIGN_REVIEW.md` (screen-by-screen review), `docs/OOP.md` (pillars + UML).
- Deploy (Phase 9): `build-and-deploy.cmd` (mvnw clean package → `vercel deploy --prod` in web-dist; `preview`
  arg for a preview URL). `.github/workflows/deploy.yml` (Java 21, secrets → config.properties, deploy web-dist).
  `.gitattributes` keeps `mvnw` LF. Steps + checklist in `docs/DEPLOYMENT.md`.
- **@JSBody gotcha:** TeaVM renames JSBody parameters to short names (a, b, c…). Local variables inside a
  JSBody script must use an `lh` prefix (e.g. `var lhBounds`) so they never collide with a parameter.
- `Component.rerender()` during the first render is deferred (marks dirty) — safe for cached sync callbacks.

## Database (see docs/DATABASE.md)

- Migrations `supabase/migrations/0001…0011` (0010 = optional sample data). Never edit an
  applied file; add a new numbered file instead.
- Role helper is `current_app_role()` (`current_role` is a Postgres keyword).
- Orders are created only via `rpc place_order`; jobs move only via `accept_job` /
  `decline_job` / `advance_job`. `rider_jobs.current_step` = steps completed.
- Return-delivery jobs start `WAITING` and become `AVAILABLE` when the order is `READY`.
- RPC errors: `{"code":"PT4xx","message":"…"}`, where the HTTP status equals the code. Show `message` to the user.
- Money from the DB is numeric pesos (e.g. `410.00`); convert to centavos for `MoneyFormatter`.

## Responsive rules

`DESIGN_SPEC.md` section 4 contains build rules, not breakpoints, so these are the project's
breakpoints (phone mockups are 390 px wide; admin mockups are 1440 px wide):

| Name | Width | Layout |
|---|---|---|
| phone | < 768 px | Exactly the phone mockups: 22 px side padding, bottom nav, sticky bottom bar in the wizard. |
| tablet | 768–1199 px | Phone layout centered in a column max 640 px wide; admin sidebar collapses to a top bar with a menu button. |
| desktop | ≥ 1200 px | Customer/rider: top nav instead of bottom nav, content max 1120 px; wizard in two columns with a sticky Order summary card on the right; rider bookings list left, active job map right. Admin: dark sidebar 230 px + main area, like A1–A5. Sign-in card centered, max 440 px. |

Nothing may scroll sideways except tables inside their own scroll container.

## Design tokens (DESIGN_SPEC.md §1) — CSS variables in styles.css

| Token | Hex | CSS variable | Use |
|---|---|---|---|
| bg | `#F3F5F2` | `--bg` | App background |
| surface | `#FFFFFF` | `--surface` | Cards, inputs, bottom bars |
| ink | `#14212B` | `--ink` | Main text; dark headers; secondary dark buttons |
| muted | `#4A5863` | `--muted` | Secondary text, labels |
| subtle | `#6B7780` | `--subtle` | Disabled / upcoming text |
| accent | `#1E63A8` | `--accent` | Primary buttons, selected states, progress, links |
| accentTint | `#E4EEF8` | `--accent-tint` | Icon tiles |
| selected card bg | `#F2F7FC` | `--accent-selected-bg` | Selected card background |
| border | `#DCE2DD` | `--border` | Unselected card borders |
| input border | `#C8D0CA` | `--input-border` | Inputs |
| divider | `#E1E6E2` | `--divider` | Lines between rows |
| warnBg / warnText | `#FFF1E8` / `#8A3A12` | `--warn-bg` / `--warn-text` | 8 kg notice, "Collect cash", Return delivery badge |
| pin | `#E0662A` | `--pin` | Map pin |
| successBg / successText | `#E6F2EA` / `#1D5E36` | `--success-bg` / `--success-text` | Paid, Completed, Active, Cash icon |
| onDuty | `#1D7A4A` | `--on-duty` | Rider "On duty" switch |
| disabled | `#9AA5AD` | `--disabled` | Disabled primary button |
| admin nav active | `#24333F` | `--admin-nav-active` | Active sidebar item |

Status badges (bg / text), always with text, never color alone:
Pending `#EEF1EE`/`#3A4651` · Washing `#E4EEF8`/`#174B80` · Ready `#DFF3F0`/`#0B5B54` ·
Out for delivery `#FFF1E8`/`#8A3A12` · Completed `#E6F2EA`/`#1D5E36`.

Typography: headings & big numbers **Bricolage Grotesque** 700–800; body **DM Sans** 400/500/700
(files in `src/main/webapp/fonts/`). Phone sizes: screen title 21, hero 30–32, section title 16–17,
body 14–15, caption 12–13, totals 22–28, kilo number 48.

Shape: side padding 22 px; card radius 18–20; hero card 24; inputs 14; chips 14; pill buttons
fully rounded; touch targets ≥ 44 px (primary buttons 52–54 px); 12–14 px between stacked
cards, 18–22 px between sections. Icons: outline, stroke 2, no emoji.

## Screens (docs/design/screens.json)

| PNG | Prototype | Component | Title | Size | Group |
|---|---|---|---|---|---|
| 00a-sign-in.png | 00a-sign-in.html | Auth | Customer / Rider / Admin sign in | phone | Sign in & sign up |
| 00b-sign-up.png | 00b-sign-up.html | Signup | Create account | phone | Sign in & sign up |
| 00c-admin-sign-in.png | 00c-admin-sign-in.html | AdminLogin | Admin sign in | phone | Sign in & sign up |
| 01-home.png | 01-home.html | Main | Home (start of interactive order flow) | phone | Customer app |
| 02-choose-service.png | 02-choose-service.html | Service | Choose service | phone | Customer app |
| 03-weight-and-soap.png | 03-weight-and-soap.html | Load | Weight & fabric soap | phone | Customer app |
| 04a-pin-location-delivery.png | 04a-pin-location-delivery.html | LocationDelivery | Pin location (delivery) | phone | Customer app |
| 04b-drop-off-walk-in.png | 04b-drop-off-walk-in.html | LocationWalkin | Drop-off schedule (walk-in) | phone | Customer app |
| 05-review-and-pay.png | 05-review-and-pay.html | Payment | Review & pay | phone | Customer app |
| 06-order-placed.png | 06-order-placed.html | Confirmation | Order placed | phone | Customer app |
| 07-track-order.png | 07-track-order.html | Tracking | Track order (live GPS) | phone | Customer app |
| R1-rider-bookings.png | R1-rider-bookings.html | Rider | Available bookings — accept / decline | phone | Rider app |
| R2-rider-pickup-job.png | R2-rider-pickup-job.html | RiderPickup | Pickup job in progress | phone | Rider app |
| R3-rider-delivery-cash.png | R3-rider-delivery-cash.html | RiderDelivery | Return delivery — collect cash | phone | Rider app |
| R4-rider-job-done.png | R4-rider-job-done.html | RiderDone | Job completed | phone | Rider app |
| R5-rider-off-duty.png | R5-rider-off-duty.html | RiderOffDuty | Off duty | phone | Rider app |
| A1-admin-dashboard.png | A1-admin-dashboard.html | AdminDashboard | Admin dashboard | desktop | Admin console |
| A2-admin-orders.png | A2-admin-orders.html | AdminOrders | Orders & status update | desktop | Admin console |
| A3-admin-pricing.png | A3-admin-pricing.html | AdminPricing | Pricing & soaps | desktop | Admin console |
| A4-admin-users.png | A4-admin-users.html | AdminUsers | Customers, riders & staff | desktop | Admin console |
| A5-admin-reports.png | A5-admin-reports.html | AdminReports | Sales reports | desktop | Admin console |

Design notes that differ from the web build: DESIGN_SPEC mentions JavaFX for the admin console and
Android fonts. In this project **everything is the one TeaVM website**; the admin pages are web
pages at `#/admin/...`.
