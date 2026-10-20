# Laundry HUB — website

Responsive laundry ordering website (desktop + mobile), installable as an app (PWA).
Written in **Java** (OOP) and compiled to JavaScript with **TeaVM**. **Supabase** is the
database, login and file storage; **Vercel** hosts the static files.

Customers order walk-in or pickup & delivery laundry (minimum 8 kg, choice of fabric soap,
Cash or GCash). Riders accept pickup and return-delivery jobs. Admins manage orders,
prices, users and sales reports.

## Requirements

- JDK 17 or newer (the guide targets 21; change `java.version` in `pom.xml` after installing it)
- VS Code with *Extension Pack for Java* and *Live Server*
- Maven is **not** required: use the included wrapper `mvnw.cmd`

## Setup

1. Copy `config.example.properties` to `config.properties` and fill in `SUPABASE_URL`,
   `SUPABASE_PUBLISHABLE_KEY` and `GOOGLE_MAPS_KEY` (public values only).
2. Build:
   ```
   .\mvnw.cmd clean package
   ```
   Output goes to `web-dist/` (`js/app.js` from TeaVM, `config.js` from your properties,
   plus `index.html`, `styles.css` and fonts).
3. Preview: in VS Code, click **Go Live** (Live Server serves `web-dist/` at
   http://127.0.0.1:5500).
4. Test:
   ```
   .\mvnw.cmd test
   ```

VS Code tasks **Build** and **Test** (Terminal → Run Task) run the same commands.

## Deploy

```
npm install -g vercel     (once)
vercel login              (once)
build-and-deploy.cmd      (tests + TeaVM build, then uploads web-dist to Vercel)
```

Then add the Vercel address to the Google Maps key and to Supabase's URL settings — see
`docs/DEPLOYMENT.md` (also covers the optional GitHub Actions auto-deploy and a final checklist).

## Project docs

- `CLAUDE.md` — project rules, design tokens, breakpoints, screen list
- `docs/design/` — UI design package (source of truth; do not edit)
- `supabase/migrations/` — SQL to paste into the Supabase SQL Editor (`docs/DATABASE.md`)
- `docs/DEPLOYMENT.md` — Vercel deploy, URLs to allow, final checklist
- `docs/DESIGN_REVIEW.md` — every screen compared with its design
- `docs/OOP.md` — OOP pillars with class/line references and UML
- `Laundry_HUB_Web_Java_Supabase_Vercel_Guide.pdf` — the phase-by-phase build guide
