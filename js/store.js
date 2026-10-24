// Penyimpanan SIMIT MVP
// - Browser: localStorage (fallback in-memory)
// - Node/uji: adapter in-memory
// Pembacaan append-only (Bab 2.4); dokumen punya status workflow (Bab 3.3).

import { DI, seedPembacaan } from "../data/seed.js";

const KEY = "simit_mvp_v1";
const mem = { db: null };

function lsGet() {
  try { return JSON.parse(localStorage.getItem(KEY) || "null"); } catch { return null; }
}
function lsSet(db) {
  try { localStorage.setItem(KEY, JSON.stringify(db)); } catch { /* mode privat */ }
}

export function muat() {
  if (typeof localStorage !== "undefined") {
    const db = lsGet();
    if (db && Array.isArray(db.di) && Array.isArray(db.pembacaan)) return db;
    return seed();
  }
  if (!mem.db) mem.db = seed();
  return mem.db;
}

export function seed() {
  const db = {
    di: JSON.parse(JSON.stringify(DI)),
    pembacaan: JSON.parse(JSON.stringify(seedPembacaan())),
    audit: [{ waktu: new Date().toISOString(), aksi: "seed", catatan: "Data contoh dimuat" }],
  };
  return simpan(db);
}

export function simpan(db) {
  if (typeof localStorage !== "undefined") lsSet(db); else mem.db = db;
  return db;
}

// Tambah pembacaan (append-only). Idempoten: bangunan + blangko + waktu harus unik.
export function tambahPembacaan({ kode_blangko, id_bangunan, waktu, muka_air_cm, debit_lps, hujan_mm, pengisi, catatan }) {
  const db = muat();
  const df = JSON.parse(JSON.stringify(db));
  const wkt = waktu || new Date().toISOString();
  const dup = df.pembacaan.find((p) => p.id_bangunan === id_bangunan && p.kode_blangko === kode_blangko && p.waktu === wkt);
  if (dup) return { ok: false, alasan: "Duplikat: pembacaan untuk bangunan, blangko, dan waktu yang sama sudah ada" };
  const id = "P-" + String(df.pembacaan.length + 1).padStart(3, "0");
  const item = {
    id, kode_blangko, id_bangunan, waktu: wkt,
    muka_air_cm: muka_air_cm ?? null, debit_lps: debit_lps ?? null, hujan_mm: hujan_mm ?? null,
    pengisi: pengisi || "anonim", catatan: catatan || "", status: "draf",
    audit: [{ waktu: new Date().toISOString(), aksi: "tambah" }],
  };
  df.pembacaan.push(item);
  df.audit.push({ waktu: new Date().toISOString(), aksi: "tambah-pembacaan", catatan: id });
  simpan(df);
  return { ok: true, item };
}

// Transisi status (Bab 3.3): draf -> terkirim -> terverifikasi
export function setStatus(id, statusBaru, pejabat) {
  const db = muat();
  const df = JSON.parse(JSON.stringify(db));
  const item = df.pembacaan.find((p) => p.id === id);
  if (!item) return { ok: false, alasan: "Data tidak ditemukan" };
  const diizinkan = { draf: ["terkirim"], terkirim: ["terverifikasi"], terverifikasi: [] };
  if (!diizinkan[item.status]?.includes(statusBaru)) {
    return { ok: false, alasan: `Transisi ${item.status} → ${statusBaru} tidak diizinkan` };
  }
  item.status = statusBaru;
  item.audit.push({ waktu: new Date().toISOString(), aksi: statusBaru, pelaku: pejabat || "anonim" });
  df.audit.push({ waktu: new Date().toISOString(), aksi: "ubah-status", catatan: `${id}: ${statusBaru}` });
  simpan(df);
  return { ok: true, item };
}
