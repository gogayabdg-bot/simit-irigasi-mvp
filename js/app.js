// SIMIT MVP — logika antarmuka (dipakai bersama index.html, lapangan.html, faktork.html)
import { muat, tambahPembacaan, setStatus } from "./store.js";
import { hitungFaktorK, debitDariMukaAir, validasiPembacaan, jadwalGilir } from "./engine.js";

export const fmt = (d) => new Date(d).toLocaleString("id-ID", { dateStyle: "medium", timeStyle: "short" });

let _diTerpilih = null; // untuk qs di dashboard saat refresh

// ===== Dashboard =====
export function renderDashboard(el) {
  const db = muat();
  const rows = db.di.map((di) => {
    const qs = qsTersediaUntukDI(db, di);
    let hasil = null;
    try { hasil = hitungFaktorK({ qs_lps: qs, petak: di.petak }); } catch {}
    const kat = hasil?.kategori ?? { label: "—", warna: "#9ca3af", rekomendasi: "Q_s belum tercatat" };
    return `<tr>
      <td><b>${di.nama}</b><div class="muted">${di.kode} · ${di.uptd}</div></td>
      <td>${di.luas_fungsional_ha} ha</td>
      <td>${di.bangunan.length}</td>
      <td>${qs ?? "—"} l/dt</td>
      <td><span class="badge" style="background:${kat.warna}">${hasil ? hasil.nilai_k.toFixed(2) : "—"}</span> ${kat.label}</td>
      <td class="muted">${kat.rekomendasi}</td>
      <td><a href="faktork.html?di=${di.id}">Rincian K</a></td>
    </tr>`;
  }).join("");
  el.innerHTML = `
  <table id="tbl-di">
    <thead><tr><th>Daerah Irigasi</th><th>Luas fungsional</th><th>Bangunan</th><th>Q_s tercatat</th><th>Faktor-K</th><th>Rekomendasi</th><th></th></tr></thead>
    <tbody>${rows}</tbody>
  </table>
  <h3>Antrean verifikasi</h3>
  ${renderAntrean()}
  <h3>Audit terakhir</h3>
  <ul class="muted small">${db.audit.slice(-6).reverse().map((a) => `<li>${fmt(a.waktu)} — ${a.aksi} ${a.catatan || ""}</li>`).join("")}</ul>`;
}

function qsTersediaUntukDI(db, di) {
  const intake = di.bangunan.filter((b) => b.jenis === "bendung").map((b) => b.id);
  const terverifikasi = db.pembacaan
    .filter((p) => intake.includes(p.id_bangunan) && p.kode_blangko === "08-O" && p.status === "terverifikasi" && p.debit_lps != null)
    .sort((a, b) => b.waktu.localeCompare(a.waktu));
  const terbaru = terverifikasi[0] || db.pembacaan
    .filter((p) => intake.includes(p.id_bangunan) && p.kode_blangko === "08-O" && p.debit_lps != null)
    .sort((a, b) => b.waktu.localeCompare(a.waktu))[0];
  if (!terbaru) {
    const b = di.bangunan.find((x) => x.jenis === "bendung");
    return b ? (b.kapasitas_m3s * 1000) : null;
  }
  return terbaru.debit_lps;
}

export function renderAntrean() {
  const db = muat();
  const rows = db.pembacaan.slice().sort((a, b) => b.waktu.localeCompare(a.waktu)).slice(0, 10).map((p) => {
    const di = db.di.find((d) => d.bangunan.some((b) => b.id === p.id_bangunan));
    const bn = di?.bangunan.find((b) => b.id === p.id_bangunan);
    const st = { draf: "#6b7280", terkirim: "#d97706", terverifikasi: "#16a34a" }[p.status] || "#6b7280";
    return `<tr>
      <td><b>${p.kode_blangko}</b></td>
      <td>${bn ? bn.nama : p.id_bangunan}</td>
      <td class="muted">${fmt(p.waktu)}</td>
      <td>${p.debit_lps ?? (p.muka_air_cm != null ? `${p.muka_air_cm} cm → ${debitDariMukaAir(p.muka_air_cm)} l/dt' : "—")}${p.hujan_mm != null ? `${p.hujan_mm} mm` : ""}</td>
      <td><span class="badge" style="background:${st}">${p.status}</span></td>
      <td>
        ${p.status === "draf" ? `<button data-aksi="kirim" data-id="${p.id}">Kirim</button>` : ""}
        ${p.status === "terkirim" ? `<button data-aksi="verifikasi" data-id="${p.id}">Verifikasi</button>` : ""}
      </td>
    </tr>`;
  }).join("");
  return `<table><thead><tr><th>Blangko</th><th>Bangunan</th><th>Waktu</th><th>Nilai</th><th>Status</th><th></th></tr></thead><tbody>${rows}</tbody></table>`;
}

export function wireAksi(el) {
  el.addEventListener("click", (e) => {
    const btn = e.target.closest("button[data-aksi]");
    if (!btn) return;
    const id = btn.dataset.id;
    const hasil = btn.dataset.aksi === "kirim" ? setStatus(id, "terkirim", "pengisi")
      : setStatus(id, "terverifikasi", "pengamat");
    if (!hasil.ok) alert(hasil.alasan);
    renderDashboard(document.getElementById("slot"));
  });
}

// ===== Lapangan =====
export function renderLapangan(el) {
  const db = muat();
  const opsiBangunan = db.di.map((di) =>
    `<optgroup label="${di.nama}">${di.bangunan.map((b) => `<option value="${b.id}">${b.nama}</option>`).join("")}</optgroup>`
  ).join("");
  el.innerHTML = `
  <form id="frm">
    <label>Blangko
      <select name="kode">
        <option value="08-O">08-O — Pencatatan debit sungai/bangunan pengambilan (POB)</option>
        <option value="06-O">06-O — Pencatatan debit saluran (PPA/Juru)</option>
        <option value="04-O">04-O — Laporan keadaan air dan tanaman (Juru)</option>
        <option value="Hujan">Hujan harian (ombrometer)</option>
      </select>
    </label>
    <label>Bangunan <select name="bangunan">${opsiBangunan}</select></label>
    <label>Waktu <input type="datetime-local" name="waktu" /></label>
    <label>Muka air (cm) <input type="number" name="muka" step="0.1" placeholder="opsional" /></label>
    <label>Debit (l/dt) <input type="number" name="debit" step="0.1" placeholder="otomatis dari muka air bila kosong" /></label>
    <label>Hujan (mm) <input type="number" name="hujan" step="0.1" placeholder="blangko hujan saja" /></label>
    <label>Catatan <input type="text" name="catatan" placeholder="keadaan air dan tanaman (04-O)" /></label>
    <button type="submit">Simpan (draf)</button>
    <div id="pesan" class="muted"></div>
  </form>
  <h3>Data hari ini</h3>
  ${renderDaftarLapangan()}
  <p class="muted small">Offline: data disimpan lokal di perangkat dan tetap tersedia tanpa internet.</p>`;
  pasangFormLapangan();
}

function renderDaftarLapangan() {
  const db = muat();
  const rows = db.pembacaan.slice().sort((a, b) => b.waktu.localeCompare(a.waktu)).slice(0, 8).map((p) =>
    `<tr><td>${p.kode_blangko}</td><td>${p.id_bangunan}</td><td class="muted">${fmt(p.waktu)}</td><td>${p.debit_lps ?? p.muka_air_cm ?? p.hujan_mm ?? "—"}</td><td>${p.status}</td></tr>`
  ).join("");
  return `<table><thead><tr><th>Blangko</th><th>Bangunan</th><th>Waktu</th><th>Nilai</th><th>Status</th></tr></thead><tbody>${rows}</tbody></table>`;
}

function pasangFormLapangan() {
  const frm = document.getElementById("frm");
  frm.addEventListener("submit", (e) => {
    e.preventDefault();
    const f = new FormData(frm);
    const kode = f.get("kode");
    const id_bangunan = f.get("bangunan");
    const waktu = f.get("waktu") ? new Date(f.get("waktu")).toISOString() : new Date().toISOString();
    const muka = f.get("muka") ? Number(f.get("muka")) : null;
    let debit = f.get("debit") ? Number(f.get("debit")) : null;
    if (debit == null && muka != null && kode !== "Hujan") debit = debitDariMukaAir(muka);
    const hujan = f.get("hujan") ? Number(f.get("hujan")) : null;
    const catatan = f.get("catatan") || "";
    const v = validasiPembacaan({ muka_air_cm: muka, debit_lps: debit, waktu });
    const pesan = document.getElementById("pesan");
    if (!v.ok) { pesan.textContent = "Ditolak: " + v.galat.join("; "); return; }
    const r = tambahPembacaan({ kode_blangko: kode, id_bangunan, waktu, muka_air_cm: muka, debit_lps: debit, hujan_mm: hujan, pengisi: "juru", catatan });
    if (!r.ok) { pesan.textContent = r.alasan; return; }
    setStatus(r.item.id, "terkirim", "lapangan");
    pesan.textContent = `Tersimpan (${r.item.id}) — status terkirim.`;
    location.reload();
  });
}

// ===== Faktor-K =====
export function renderFaktorK(el, diId) {
  const db = muat();
  const di = db.di.find((d) => d.id === diId) || db.di[0];
  const qs = qsTersediaUntukDI(db, di);
  let hasil;
  try { hasil = hitungFaktorK({ qs_lps: qs, petak: di.petak }); } catch (e) { el.innerHTML = `<p class="error">${e.message}</p>`; return; }
  const gilir = jadwalGilir({ qs_lps: hasil.qs_lps, golongan: hasil.detail_kebutuhan });
  el.innerHTML = `
  <h2>${di.nama}</h2>
  <p class="muted">${di.kode} · ${di.uptd} · ${di.luas_fungsional_ha} ha</p>
  <div class="kpis">
    <div class="kpi"><div class="kpi-n">${hasil.qs_lps}</div><div class="muted">Q_s (l/dt)</div></div>
    <div class="kpi"><div class="kpi-n">${hasil.qh_lps}</div><div class="muted">Q_h (kehilangan)</div></div>
    <div class="kpi"><div class="kpi-n">${hasil.qtersedia_lps}</div><div class="muted">Q tersedia</div></div>
    <div class="kpi"><div class="kpi-n">${hasil.qbutuh_total_lps}</div><div class="muted">Q butuh (l/dt)</div></div>
    <div class="kpi"><div class="kpi-n" style="color:${hasil.kategori.warna}">${hasil.nilai_k.toFixed(2)}</div><div class="muted">Faktor-K — ${hasil.kategori.label}</div></div>
  </div>
  <p><b>Rekomendasi:</b> ${hasil.kategori.rekomendasi} <span class="muted">(versi parameter ${hasil.versi_parameter})</span></p>
  <h3>Rincian kebutuhan per petak</h3>
  <table><thead><tr><th>Petak</th><th>Luas (ha)</th><th>Komoditas</th><th>NFR (l/dt/ha)</th><th>FT</th><th>Kebutuhan (l/dt)</th></tr></thead>
  <tbody>${hasil.detail_kebutuhan.map((d) => `<tr><td>${d.petak}</td><td>${d.luas_ha}</td><td>${d.komoditas}</td><td>${d.nfr_lps_ha}</td><td>${d.ft}</td><td>${d.kebutuhan_lps}</td></tr>`).join("")}
  <tr><td colspan="5"><b>Total</b></td><td><b>${hasil.qbutuh_total_lps}</b></td></tr></tbody></table>
  <h3>Rumus</h3>
  <pre>NFR = (ETc + P + WLR − Re) / 8,64     ETc = Kc × ETo
Qbutuh = Σ( Aᵢ × NFRᵢ × FTᵢ )
K = (Q_s − Q_h) / Qbutuh</pre>
  <h3>Jadwal gilir air (draf 07-O)</h3>
  <pre>${JSON.stringify(gilir, null, 2)}</pre>
  <p class="muted small">Pengesahan pembagian air tetap menjadi keputusan Pengamat/UPTD (Bab 3.3).</p>`;
}
