// SIMIT — Mesin Faktor-K (Bab 3.5 dokumen rancangan v2.0)
// Semua parameter disimpan sebagai "data berversi", bukan angka tertanam di logika bisnis.

export const PARAMS = {
  versi: 1, // versi parameter (Bab C.1)
  perkolasi_mm_hari: 3,     // P
  wlr_mm_hari: 60,          // penggantian lapisan air
  eto_mm_hari: 4.5,         // evapotranspirasi acuan (Penman-Monteith FAO, default stasiun)
  qh_posi: 0.15,           // Q_h = 15% dari Q_s (kehilangan saluran primer-sekunder)
  kc_default: 1.0,          // koefisien tanaman default
  re_hujan_efektif: 1.5,    // hujan efektif mm/hari
  ft_default: 1.3,          // faktor tersier (1,25–1,43)
  ambang_k: { sedang: 0.7, cukup: 1.0 },
};

// Ambang K (Bab 3.5): K>=1.0 Melimpah / 0.7..1.0 Sedang / <0.7 Kritis
export function kategoriK(nilaiK) {
  if (nilaiK >= PARAMS.ambang_k.cukup) return { kode: "melimpah", label: "Melimpah", rekomendasi: "Aliran kontinu sesuai usulan", warna: "#16a34a" };
  if (nilaiK >= PARAMS.ambang_k.sedang) return { kode: "sedang", label: "Sedang", rekomendasi: "Bukaan proporsional (usulan × K) atau gilir ringan", warna: "#d97706" };
  return { kode: "kritis", label: "Kritis", rekomendasi: "Gilir air ketat", warna: "#dc2626" };
}

// NFR (l/dt/ha) = (ETc + P + WLR - Re) / 8,64 ; ETc = Kc x ETo
export function nfr(opsi = {}, param = PARAMS) {
  const kc = opsi.kc ?? param.kc_default;
  const eto = opsi.eto ?? param.eto_mm_hari;
  const P = opsi.perkolasi ?? param.perkolasi_mm_hari;
  const WLR = opsi.wlr ?? param.wlr_mm_hari;
  const Re = opsi.re ?? param.re_hujan_efektif;
  const tc = kc * eto; // mm/hari
  const nilai = (tc + P + WLR - Re) / 8.64;
  return { nilai_lps_ha: r3(nilai), tc_mm_hari: r3(tc), komponen: { kc, eto, P, WLR, Re } };
}

// Qbutuh = jumlah( A_i x NFR_i x FT_i )  (l/dt)
export function kebutuhanAir(petak, param = PARAMS) {
  const detail = petak.map((pt) => {
    const n = nfr({ kc: pt.kc, eto: param.eto_mm_hari }, param);
    const ft = pt.ft ?? param.ft_default;
    const q = pt.luas_ha * n.nilai_lps_ha * ft;
    return { petak: pt.nama, luas_ha: pt.luas_ha, komoditas: pt.komoditas, nfr_lps_ha: n.nilai_lps_ha, ft, kebutuhan_lps: r2(q) };
  });
  const total = detail.reduce((s, d) => s + d.kebutuhan_lps, 0);
  return { detail, total_lps: r2(total) };
}

// K = Q_tersedia / Q_dibutuhkan ; Q_tersedia = Q_s - Q_h
export function hitungFaktorK({ qs_lps, petak }) {
  if (!(qs_lps > 0)) throw new RangeError("Q_s harus > 0 (l/dt)");
  const kebutuhan = kebutuhanAir(petak);
  if (!(kebutuhan.total_lps > 0)) throw new RangeError("Q_butuh harus > 0");
  const qh_lps = r2(qs_lps * PARAMS.qh_porsi);
  const qtersedia_lps = r2(qs_lps - qh_lps);
  const nilai = r3(qtersedia_lps / kebutuhan.total_lps);
  return {
    qs_lps: r2(qs_lps), qh_lps, qtersedia_lps,
    qbutuh_total_lps: kebutuhan.total_lps,
    detail_kebutuhan: kebutuhan.detail,
    nilai_k: nilai,
    kategori: kategoriK(nilai),
    versi_parameter: PARAMS.versi,
  };
}

// Validasi data (Bab 3.4)
export function validasiPembacaan({ muka_air_cm, debit_lps, waktu }) {
  const galat = [];
  if (waktu && isNaN(Date.parse(waktu))) galat.push("Waktu tidak valid");
  if (debit_lps != null && (debit_lps < 0 || debit_lps > 100000)) galat.push("Debit di luar rentang fisik (0–100.000 l/dt)");
  if (muka_air_cm != null && (muka_air_cm < -50 || muka_air_cm > 2000)) galat.push("Muka air di luar rentang (-50 cm s/d 20 m)");
  return { ok: galat.length === 0, galat };
}

// Lengkung debit sederhana per bangunan: Q = C x (H - H0)^n  (dalam l/dt)
export function debitDariMukaAir(muka_air_cm, lengkung = { C: 0.025, H0: 10, n: 1.6 }) {
  const h = Math.max(0, muka_air_cm - lengkung.H0);
  return r2(lengkung.C * Math.pow(h, lengkung.n) * 1000);
}

// Jadwal gilir air dasar (Bab 3.5): rotasi per hari, alokasi tidak melebihi kapasitas
export function jadwalGilir({ qs_lps, golongan }) {
  const kapasitas = r2(qs_lps * (1 - PARAMS.qh_porsi));
  const terurut = golongan
    .map((g) => ({ nama: g.nama, kebutuhan_lps: g.kebutuhan_lps, prioritas: g.prioritas ?? 1 }))
    .sort((a, b) => b.prioritas - a.prioritas || b.kebutuhan_lps - a.kebutuhan_lps);
  const hari = {}; let hariIdx = 1; let sisa = kapasitas; let dayList = [];
  for (const g of terurut) {
    if (sisa <= 0) { hari[`hari-${hariIdx}`] = dayList; hariIdx++; dayList = []; sisa = kapasitas; }
    const alok = Math.min(g.kebutuhan_lps, sisa);
    dayList.push({ golongan: g.nama, alokasi_lps: r2(alok), penuh: alok >= g.kebutuhan_lps });
    sisa = r2(sisa - alok);
  }
  if (dayList.length) hari[`hari-${hariIdx}`] = dayList;
  return { kapasitas_harian_lps: kapasitas, hari };
}

function r2(x) { return Math.round(x * 100) / 100; }
function r3(x) { return Math.round(x * 1000) / 1000; }
