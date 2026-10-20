// Laundry HUB service worker: caches the app shell so the site installs and opens offline.
// Only same-origin files are handled; Supabase and Google requests always go to the network.
// Bump CACHE after a deploy if an old version keeps showing.
const CACHE = "laundryhub-v1";
const SHELL = ["/", "/index.html", "/styles.css", "/config.js", "/js/app.js", "/manifest.webmanifest",
  "/fonts/fonts.css", "/icons/icon-192.png", "/icons/favicon.svg"];

self.addEventListener("install", e => e.waitUntil(caches.open(CACHE).then(c => c.addAll(SHELL)).then(() => self.skipWaiting())));
self.addEventListener("activate", e => e.waitUntil(caches.keys()
  .then(keys => Promise.all(keys.filter(k => k !== CACHE).map(k => caches.delete(k)))).then(() => self.clients.claim())));

self.addEventListener("fetch", e => {
  const url = new URL(e.request.url);
  if (e.request.method !== "GET" || url.origin !== self.location.origin) return; // never Supabase / Google
  const stable = url.pathname.startsWith("/fonts/") || url.pathname.startsWith("/icons/");
  e.respondWith(stable
    ? caches.match(e.request).then(hit => hit || fetch(e.request).then(r => put(e.request, r)))
    : fetch(e.request).then(r => put(e.request, r)).catch(() => caches.match(e.request).then(hit => hit || caches.match("/index.html"))));
});

function put(req, res) {
  if (res.ok) { const copy = res.clone(); caches.open(CACHE).then(c => c.put(req, copy)); }
  return res;
}
