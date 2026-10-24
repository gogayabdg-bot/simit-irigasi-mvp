// Seed data contoh — D.I. pilot (mirip konteks UPTD PSDA WS Citarum, untuk demo)
export const DI = [
  {
    id: "DI-PILOT-01",
    kode: "3201-INV-001",
    nama: "DI Pilot Citarum Hilir",
    luas_fungsional_ha: 2150,
    uptd: "UPTD PSDA WS Citarum",
    bangunan: [
      { id: "BND-001", nama: "Intake Induk", jenis: "bendung", kapasitas_m3s: 6.5 },
      { id: "BND-002", nama: "Bagi Sekunder S-1", jenis: "bagi", kapasitas_m3s: 3.0 },
      { id: "BND-003", nama: "Sadap Tersier T-12", jenis: "sadap", kapasitas_m3s: 1.2 },
    ],
    petak: [
      { nama: "Tersier T-11", luas_ha: 320, komoditas: "Padi (generatif)", kc: 1.1 },
      { nama: "Tersier T-12", luas_ha: 410, komoditas: "Padi (bulir)", kc: 1.15, ft: 1.35 },
      { nama: "Sekunder S-1", luas_ha: 680, komoditas: "Palawija", kc: 0.9 },
      { nama: "Sekunder S-2", luas_ha: 740, komoditas: "Padi (pertumbuhan)", kc: 1.05 },
    ],
  },
  {
    id: "DI-02",
    kode: "3201-INV-002",
    nama: "DI Sekundang Cimanuk",
    luas_fungsional_ha: 1450,
    uptd: "UPTD PSDA WS Cimanuk-Cisanggarung",
    bangunan: [
      { id: "BND-101", nama: "Intake Sekundang", jenis: "bendung", kapasitas_m3s: 4.0 },
      { id: "BND-102", nama: "Bagi S-A", jenis: "bagi", kapasitas_m3s: 2.2 },
    ],
    petak: [
      { nama: "Golongan A", luas_ha: 520, komoditas: "Padi (bulir)", kc: 1.2, ft: 1.4 },
      { nama: "Golongan B", luas_ha: 930, komoditas: "Palawija", kc: 0.95 },
    ],
  },
];

export function seedPembacaan() {
  return [
    { id: "P-001", kode_blangko: "08-O", id_bangunan: "BND-001", waktu: tanggal(-2, 7), muka_air_cm: 42, debit_lps: 510, pengisi: "POB", status: "terverifikasi" },
    { id: "P-002", kode_blangko: "08-O", id_bangunan: "BND-001", waktu: tanggal(-1, 7), muka_air_cm: 40, debit_lps: 470, pengisi: "POB", status: "terverifikasi" },
    { id: "P-003", kode_blangko: "06-O", id_bangunan: "BND-002", waktu: tanggal(-1, 12), debit_lps: 330, pengisi: "Juru", status: "terverifikasi" },
    { id: "P-004", kode_blangko: "06-O", id_bangunan: "BND-003", waktu: tanggal(0, 7), muka_air_cm: 28, debit_lps: null, pengisi: "POB", status: "terkirim" },
    { id: "P-005", kode_blangko: "04-O", id_bangunan: "BND-003", waktu: tanggal(0, 8), pengisi: "Juru", catatan: "Air masuk ke petak kecuali petak pinggir; tanaman fase bulir", status: "draf" },
    { id: "P-006", kode_blangko: "Hujan", id_bangunan: "BND-001", waktu: tanggal(0, 7), hujan_mm: 12, pengisi: "Juru", status: "terkirim" },
  ];
}

function tanggal(deltaHari, jam) {
  const d = new Date();
  d.setDate(d.getDate() + deltaHari);
  d.setHours(jam, 0, 0, 0);
  return d.toISOString();
}
