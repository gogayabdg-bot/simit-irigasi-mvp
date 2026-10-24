import { renderDashboard, wireAksi } from "./app.js";

const slot = document.getElementById("slot");
renderDashboard(slot);
wireAksi(slot);

if ("serviceWorker" in navigator) {
  navigator.serviceWorker.register("sw.js").catch(() => {});
}
