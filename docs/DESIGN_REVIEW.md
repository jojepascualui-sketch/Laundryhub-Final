# Laundry HUB — Design review

Every screen in `docs/design/screens.json` was built, opened in Edge (headless) at **390 px** (phone),
**768 px** (tablet) and **1280 / 1440 px** (desktop), and compared side by side with its PNG.
An automated sweep also checked every page at the three widths for horizontal scrolling and page
errors: **all pages pass** (tables scroll inside their own container, as allowed).

Test data came from a local mock of Supabase and a stand-in for Google Maps, so the map tiles in
screenshots are a plain green area; with a real Maps key the map renders normally.

| Screen | Matches? | Differences found | Fixed? |
|---|---|---|---|
| 00a Sign in | Yes | "Remember me" started ticked; the gap under Password was too large (empty error line); Google logo is the real "G" instead of the design's placeholder glyph | Yes (glyph kept on purpose) |
| 00b Sign up | Yes | — | — |
| 00c Admin sign in | Yes | "Continue with Google" showed on the Admin tab (`hidden` was overridden by `display:flex`) | Yes — global `[hidden]` rule |
| 01 Home | Yes | Desktop was squeezed into the 640 px tablet column | Yes |
| 02 Choose service | Yes | Selected radio had a white inner ring; design is fully filled | Yes |
| 03 Weight & soap | Yes | — | — |
| 04a Pin location | Yes | Prototype tap-to-pin is a drawing; real build uses Google Maps (tap / drag / GPS + address lookup); fallback drawing when Maps is unavailable; Google's own error box replaced by the fallback when the key is rejected | Yes |
| 04b Drop-off (walk-in) | Yes | Mockup shows a sample slot selected; the real page starts with none selected (Continue disabled until chosen, as in the prototype logic) | Intended |
| 05 Review & pay | Yes | Added GCash details card (shop number, account name, amount), reference number and optional screenshot (required by the guide); desktop showed the order summary twice | Yes |
| 06 Order placed | Yes | Added a short note for GCash ("We'll check your GCash payment soon") | Intended |
| 07 Track order | Yes | Added a status badge under the title and a Payment card (with "Resend reference" when rejected); times shown next to finished steps | Intended |
| R1 Rider bookings | Yes | Added a small "Sign out" link in the header (riders need a way out) | Intended |
| R2 Pickup job | Yes | — | — |
| R3 Return delivery (cash) | Yes | — | — |
| R4 Job completed | Yes | Card and button shrank to their content inside the flex page | Yes |
| R5 Off duty | Yes | Install card was cramped on phones | Yes — button on its own row |
| A1 Dashboard | Yes | Sidebar background stopped at the window height on long pages | Yes |
| A2 Orders | Yes | Added "Cancel order", "Mark cash as paid", GCash screenshot preview and Verify / Reject; phones show the detail instead of the list (with a back button) | Intended |
| A3 Pricing & soaps | Yes | Added GCash account name and a "Shop location" card (address, hours, pin) needed for the delivery radius; soap price showed "₱ 0" with a gap; page scrolled sideways at 390 px | Yes |
| A4 Customers & riders | Yes | "+ Add rider/staff" finds an account that signed up (accounts can't be created from the browser without the secret key) | Intended |
| A5 Sales reports | Yes | Added a period switch (Today / 7 days / 30 days / This month) and a summary line | Intended |

## Responsive layout (CLAUDE.md breakpoints)

| Width | Customer | Rider | Admin |
|---|---|---|---|
| 390 px | Exactly the phone mockups; bottom nav; sticky bottom bar in the wizard | Mockups | Sidebar becomes a top bar with a menu button; orders list → detail |
| 768 px | Phone layout centered in a 640 px column | Same | Same as phone, wider |
| ≥ 1200 px | Top nav; Home in two columns; wizard with a sticky Order summary card; tracking map + timeline side by side | Bookings left, active job map right; job page map left, details right | Sidebar + list / detail like A1–A5 |

## States not in the mockups (same style)

Loading spinners, empty states ("No orders yet", "No new bookings right now…"), error cards with
"Try again", inline field errors, toasts for success/failure, offline page from the service worker,
and an "Install Laundry HUB" card (Chrome/Edge/Android install button, or iPhone "Share → Add to Home Screen").
