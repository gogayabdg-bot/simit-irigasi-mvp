import { renderFaktorK } from "./app.js";

const diId = new URLSearchParams(location.search).get("di");
renderFaktorK(document.getElementById("slot"), diId);

if ("serviceWorker" in navigator) {
  navigator.serviceWorker.register("sw.js").catch(() => {});
}
