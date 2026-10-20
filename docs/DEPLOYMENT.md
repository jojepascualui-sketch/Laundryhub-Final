# Laundry HUB — Deploying to Vercel

**Live site:** https://laundryhub-final.vercel.app (Vercel project `laundry-hub`).
In the steps below, `YOUR-APP.vercel.app` means `laundryhub-final.vercel.app`.

Vercel only **hosts** the finished files. Its build machines have no Java, so the site is built on
your computer (or on GitHub Actions) with `mvnw clean package`, and the folder **`web-dist/`** is uploaded.

```
src/ (Java) ──mvnw clean package──▶ web-dist/  (index.html, js/app.js, styles.css, config.js, sw.js, icons…)
                                         │
                                         └──vercel deploy --prod──▶ https://YOUR-APP.vercel.app
```

`web-dist/vercel.json` (copied from `src/main/webapp/vercel.json`) tells Vercel: no framework, no build
command, serve the files as they are, never cache `sw.js` / `index.html` / `config.js` / `js/app.js`, cache
fonts and icons for a year, and send unknown paths to `index.html`.

## Before the first deploy

1. Run the database migrations in Supabase (see `docs/DATABASE.md`): `0001`…`0009`, then `0011`.
2. `config.properties` has `SUPABASE_URL`, `SUPABASE_PUBLISHABLE_KEY` and (optional) `GOOGLE_MAPS_KEY`.
   Only public values — never the Supabase **secret** key.
3. Install the Vercel CLI once and sign in (opens the browser):
   ```
   npm install -g vercel
   vercel login
   ```

## First deploy

From the project folder in the VS Code terminal:

```
build-and-deploy.cmd
```

It runs the JUnit tests, builds with TeaVM, then runs `vercel deploy --prod` inside `web-dist`.
The first time, the CLI asks a few questions — answer:

| Question | Answer |
|---|---|
| Set up and deploy? | **Y** |
| Which scope? | your account |
| Link to existing project? | **N** |
| Project name | **laundry-hub** |
| In which directory is your code located? | **./** |
| Want to modify these settings? | **N** (vercel.json already says: no framework, no build command) |

At the end it prints your address, e.g. **https://laundry-hub.vercel.app**. The link is saved in
`web-dist/.vercel/` (kept by `mvnw clean`, ignored by git), so later deploys need no questions.

Try a deploy without touching production: `build-and-deploy.cmd preview`.

## Make the site public (Deployment Protection)

New Vercel projects protect every `*.vercel.app` address with a Vercel login ("Vercel Authentication").
Customers would see a Vercel sign-in page, so it was turned off for this project:

```
vercel project protection disable laundry-hub --sso
```

(`laundry-hub.vercel.app` already belongs to someone else, so the production address is
`laundryhub-final.vercel.app`, set with `vercel alias set <deployment-url> laundryhub-final.vercel.app`.
It is attached to the project, so every `--prod` deploy updates it.)

## After the first deploy: allow the new address

Replace `YOUR-APP` with your real address.

**Google Cloud → APIs & Services → Credentials → your Maps key → Website restrictions → Add**
- `https://YOUR-APP.vercel.app/*`
  (keep `http://127.0.0.1:5500/*` and `http://localhost:5500/*` for local testing)

Without this the map shows Google's error box; the app switches to its simple map + GPS pin automatically,
but the real map and address lookup only work once the address is allowed.

**Supabase → Authentication → URL Configuration**
- **Site URL:** `https://YOUR-APP.vercel.app`
- **Redirect URLs → Add:** `https://YOUR-APP.vercel.app/**` (keep `http://127.0.0.1:5500/**`)

This is needed for "Continue with Google", e-mail confirmation links and password reset links.

**Google Cloud → OAuth consent screen** (for Google sign-in): if the app is still in *Testing*, only the
test users you added can sign in with Google. Add your testers, or publish the app.

## Deploying updates

```
build-and-deploy.cmd
```

If a phone keeps showing an old version: the service worker cached it. Bump `CACHE` in
`src/main/webapp/sw.js` (e.g. `laundryhub-v2`), deploy again, then reload twice (or Ctrl+Shift+R on desktop).

## Optional: deploy automatically from GitHub

`.github/workflows/deploy.yml` builds with Java 21 and deploys `web-dist` on every push to `main`.

1. Push the project to a GitHub repository (`config.properties` and `web-dist/` are git-ignored).
2. Do one manual deploy first (above), then open `web-dist/.vercel/project.json` and copy `orgId` and `projectId`.
3. Vercel → Account Settings → **Tokens** → create a token.
4. GitHub → repository → Settings → Secrets and variables → Actions → **New repository secret**:

| Secret | Value |
|---|---|
| `VERCEL_TOKEN` | the token from step 3 |
| `VERCEL_ORG_ID` | `orgId` from `project.json` |
| `VERCEL_PROJECT_ID` | `projectId` from `project.json` |
| `SUPABASE_URL` | same as `config.properties` |
| `SUPABASE_PUBLISHABLE_KEY` | same as `config.properties` |
| `GOOGLE_MAPS_KEY` | same as `config.properties` |

5. Push to `main` (or run the workflow from the Actions tab). The tests must pass for the deploy to run.

## Final checklist (on https://YOUR-APP.vercel.app)

| # | Check | Expected |
|---|---|---|
| 1 | Open the site | Sign-in screen with the blue header; no errors in the browser console (F12) |
| 2 | Sign up a customer; sign in | Lands on **Home** ("Magandang araw!") |
| 3 | Make yourself ADMIN (Table Editor → profiles) and a second account RIDER | Admin tab → **Dashboard**; Rider tab → **Bookings** |
| 4 | Admin → Pricing & soaps: set the shop location, GCash number and account name → Save | "Changes saved" |
| 5 | Customer: Start laundry order → Pickup & Delivery → 10 kg Hypoallergenic → pin on the map (tap / drag / "Use my current location") → time → GCash with a 13-digit reference + screenshot → Place order | "Order placed!" with LH-0001 and ₱410.00; the address was filled from the map |
| 6 | Pin far outside the radius | Warning and Continue disabled; the database also refuses it |
| 7 | Admin → Orders → open the order → **Verify payment** | Payment shows *Paid*; screenshot visible |
| 8 | Rider on duty (phone) → **Accept booking** | Job opens; a second rider gets "This booking was already taken." |
| 9 | Rider taps through the steps | Order becomes *Washing*; the customer's tracking page updates within 10 s and shows the rider on the map |
| 10 | Admin sets **Ready** → rider accepts the return delivery → steps to *Mark as delivered* | Order *Completed*; rider sees "Job completed" |
| 11 | Admin → Sales reports → **Export CSV** | A `.csv` file downloads |
| 12 | Phone (Chrome/Android): "Install app" card → Install; iPhone (Safari): Share → Add to Home Screen | App icon on the home screen; opens without the browser bar |
| 13 | Continue with Google (customer) | Returns to the site signed in |
| 14 | Before presenting: Supabase → Authentication → turn **Confirm email** back on | New sign-ups get a confirmation e-mail |

## Troubleshooting

| Problem | Usual cause |
|---|---|
| Blank page | Build error — run `mvnw clean package` and read the first error; check the console (F12) |
| "The app is not connected to the database yet" / 401 Invalid API key | Wrong publishable key in `config.properties`; rebuild and redeploy |
| Empty lists although data exists | Row Level Security: the account's role is wrong, or a migration was not run |
| Google's "This page can't load Google Maps correctly" | The Vercel address is not on the Maps key's website list |
| "Use my location" does nothing | Page not on https, or location permission blocked in the browser |
| Google sign-in returns to the wrong page | Site URL / Redirect URLs not set in Supabase |
| Old version keeps showing | Bump `CACHE` in `sw.js`, redeploy, hard refresh |
| No "Install app" card | Must be https, manifest + icons + service worker OK; iPhone shows the Share instructions instead |
