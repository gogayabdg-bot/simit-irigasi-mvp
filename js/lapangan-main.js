import { renderLapangan } from "./app.js";

const slot = document.getElementById("slot");
renderLapangan(slot);

if ("serviceWorker" in navigator) {
  navigator.serviceWorker.register("sw.js").catch(() => {});
}
