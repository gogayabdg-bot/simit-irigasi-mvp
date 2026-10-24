// Service worker SIMIT MVP — cache shell agar bisa dipakai offline di sawah (Bab 2.4)
const CACHE = "simit-mvp-v1";
const ASSETS = [
  "./", "./index.html", "./lapangan.html", "./faktork.html",
  "./style.css", "./manifest.json",
  "./js/app.js", "./js/index-main.js", "./js/lapangan-main.js", "./js/faktork-main.js",
  "./js/store.js", "./js/engine.js", "./data/seed.js",
];

self.addEventListener("install", (e) => {
  e.waitUntil(caches.open(CACHE).then((c) => c.addAll(ASSETS)).then(() => self.skipWaiting()));
});

self.addEventListener("activate", (e) => {
  e.waitUntil(caches.keys().then((keys) => Promise.all(keys.filter((k) => k !== CACHE).map((k) => caches.delete(k)))).then(() => self.clients.claim()));
});

self.addEventListener("fetch", (e) => {
  e.respondWith(
    caches.match(e.request).then((hit) => hit || fetch(e.request).then((res) => {
      const clone = res.clone();
      caches.open(CACHE).then((c) => c.put(e.request, clone));
      return res;
    }).catch(() => caches.match("./index.html")))
  );
});
