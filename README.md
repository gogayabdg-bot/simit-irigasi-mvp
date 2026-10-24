# SIMIT MVP — Sistem Informasi Irigasi Terpadu (DSDA Jawa Barat)

MVP berbasis dokumen **"SIMIT DSDA Jabar — Rancangan Sistem Informasi Irigasi Terpadu v2.0"** (24 Okt 2026).
Lintasan rilis: setara **R0.1–R0.3** (Fondasi, Blangko Core, Faktor-K).

## Lingkup

- Mesin Faktor-K (Bab 3.5): `NFR=(ETc+P+WLR-Re)/8,64`, `Qbutuh=Σ(A×NFR×FT)`, `K=(Qs-Qh)/Qbutuh`, parameter berversi, ambang K.
- Blangko 04-O / 06-O / 08-O + form hujan (Bab 3.1) dengan workflow Draf → Terkirim → Terverifikasi (Bab 3.3).
- Validasi data (Bab 3.4): rentang fisik, idempotensi/duplikat.
- Lengkung debit `Q=C(H-H0)^n` (Bab 5.3 versi sederhana), jadwal gilir air dasar (draf 07-O).
- Dashboard status K per D.I. + antrean verifikasi (Bab 7.1 versi tabel).
- PWA lapangan offline-first (Bab 2.4): `manifest.json` + `sw.js`, data persisten per perangkat.
- Audit log sederhana.

Belum (roadmap R1.x–R2.x): Android native Kotlin, Blangko Registry JSON-Schema, TTE/PDF/QR, backend API terpusat, auth RBAC, telemetri MQTT, IKSI/PAI, WebGIS, integrasi eksternal.

## Menjalankan

```bash
python3 -m http.server 3000   # buka http://localhost:3000
node verify.js                # 9 uji mesin Faktor-K & store (semua lulus)
```

## Struktur

- `index.html` — dashboard eksekutif
- `lapangan.html` — klien lapangan Juru/POB (blangko input)
- `faktork.html` — rincian K, rekomendasi pembagian air, jadwal gilir
- `js/engine.js` — mesin Faktor-K + validasi
- `js/store.js` — penyimpanan lokal (append-only, idempoten)
- `data/seed.js` — D.I. pilot contoh
- `verify.js` — uji otomatis
- `release/simit-mvp-src.zip` — arsip distribusi

Arsip identik juga tersimpan di Google Drive folder IRIGASI.
