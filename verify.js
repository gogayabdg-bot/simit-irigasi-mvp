// Uji mesin SIMIT MVP (Bab 8.2 — pengujian berjenjang, butir inti')
// Jalankan: node verify.js
import { hitungFaktorK, kebutuhanAir, nfr, kategoriK, debitDariMukaAir, validasiPembacaan, jadwalGilir } from "./js/engine.js";
import { DI, seedPembacaan } from "./data/seed.js";
import { muat, tambahPembacaan, setStatus } from "./js/store.js";
import assert from "node:assert";

let pass = 0, fail = 0;
function uji(nama, fn) {
  try { fn(); pass++; console.log(`✔ ${nama}`); }
  catch (e) { fail++; console.error(`✘ ${nama}: ${e.message}`); }
}

// 1. NFR: ETc=Kc(1.1)*ETo(4.5)=4.95; (4.95+3+60-1.5)/8.64 = 7.6875 -> ~7.688
uji("NFR mengikuti rumus l/dt/ha", () => {
  const n = nfr({ kc: 1.1 });
  assert.ok(Math.abs(n.nilai_lps_ha - 7.688) < 0.01, `dapat ${n.nilai_lps_ha}`);
});

// 2. Kebutuhan air = Σ A_i x NFR_i x FT_i
uji("Qbutuh = Σ(A x NFR x FT)", () => {
  const kb = kebutuhanAir([{ nama: "A", luas_ha: 100, kc: 1.0, ft: 1.3 }]);
  const ekspektasi = 100 * nfr({ kc: 1.0 }).nilai_lps_ha * 1.3;
  assert.ok(Math.abs(kb.total_lps - ekspektasi) < 0.05, `dapat ${kb.total_lps}`);
});

// 3. K = (Qs - Qh)/Qbutuh dan ambang
uji("Faktor-K + kategori ambang", () => {
  const h = hitungFaktorK({ qs_lps: 1000, petak: [{ nama: "A", luas_ha: 100 }] });
  assert.strictEqual(h.qh_lps, 150);
  const ekspektasi = (1000 - 150) / kebutuhanAir([{ nama: "A", luas_ha: 100 }]).total_lps;
  assert.ok(Math.abs(h.nilai_k - ekspektasi) < 0.01);
  assert.strictEqual(kategoriK(1.5).kode, "melimpah");
  assert.strictEqual(kategoriK(0.85).kode, "sedang");
  assert.strictEqual(kategoriK(0.5).kode, "kritis");
});

// 4. Validasi (Bab 3.4)
uji("Validasi menolak nilai di luar rentang fisik", () => {
  assert.strictEqual(validasiPembacaan({ debit_lps: -5 }).ok, false);
  assert.strictEqual(validasiPembacaan({ muka_air_cm: 3000 }).ok, false);
  assert.strictEqual(validasiPembacaan({ debit_lps: 120, waktu: "2026-10-24T07:00:00Z" }).ok, true);
});

// 5. Lengkung debit: monoton, non-negatif
uji("Lengkung debit Q=C(H-H0)^n", () => {
  const q1 = debitDariMukaAir(20), q2 = debitDariMukaAir(50);
  assert.ok(q1 >= 0 && q2 >= q1);
});

// 6. Jadwal gilir tidak melebihi kapasitas per hari
uji("Jadwal gilir menghormati kapasitas harian", () => {
  const g = jadwalGilir({ qs_lps: 600, golongan: [
    { nama: "A", kebutuhan_lps: 300 }, { nama: "B", kebutuhan_lps: 320 },
  ]});
  const kapasitas = g.kapasitas_harian_lps;
  for (const [, daftar] of Object.entries(g.hari)) {
    const total = daftar.reduce((s, x) => s + x.alokasi_lps, 0);
    assert.ok(total <= kapasitas + 0.01, `total ${total} > ${kapasitas}`);
  }
});

// 7. Store: tambah + idempotensi + transisi status
uji("Store: tambah pembacaan unik & workflow status", () => {
  const db1 = muat();
  const n0 = db1.pembacaan.length;
  const r = tambahPembacaan({ kode_blangko: "06-O", id_bangunan: "BND-001", waktu: new Date().toISOString(), debit_lps: 250, pengisi: "juru" });
  assert.ok(r.ok);
  const r2 = tambahPembacaan({ kode_blangko: "06-O", id_bangunan: "BND-001", waktu: r.item.waktu, debit_lps: 250, pengisi: "juru" });
  assert.strictEqual(r2.ok, false, "harus ditolak sebagai duplikat");
  const k = setStatus(r.item.id, "verifikasi", "pengamat");
  assert.strictEqual(k.ok, false, "draf tidak boleh langsung verifikasi");
  const k1 = setStatus(r.item.id, "terkirim", "pengisi");
  const k2 = setStatus(r.item.id, "terverifikasi", "pengamat");
  assert.ok(k1.ok && k2.ok);
  const db2 = muat();
  assert.strictEqual(db2.pembacaan.length, n0 + 1);
});

// 8. Konsistensi seed: terverifikasi di intake menghasilkan K
uji("D.I. seed menghasilkan nilai K berarti", () => {
  for (const di of DI) {
    const h = hitungFaktorK({ qs_lps: di.bangunan[0].kapasitas_m3s * 1000, petak: di.petak });
    assert.ok(h.nilai_k > 0 && h.nilai_k < 100);
  }
});

// 9. Seed pembacaan ikut terverifikasi
uji("Seed aktif dengan status beragam", () => {
  const status = new Set(seedPembacaan().map((p) => p.status));
  assert.ok(status.has("terverifikasi") && status.has("terkirim") && status.has("draf"));
});

console.log(`\n${pass} lulus, ${fail} gagal`);
process.exit(fail ? 1 : 0);
