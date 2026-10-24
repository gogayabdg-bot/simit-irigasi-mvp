# SIMIT Lapangan (Android Native) — MVP per Lampiran B

Klien **Juru / POB** (dan nanti teknisi telemetri) sesuai **Lampiran B** dokumen
*SIMIT DSDA Jabar — Rancangan Sistem Informasi Irigasi Terpadu v2.0*:
**Kotlin + Jetpack Compose + Room (SQLite) + WorkManager**, offline-first di sawah.

## Yang sudah diimplementasikan (B.2, kolom Android)

| Fitur | Status | Catatan |
| --- | --- | --- |
| Input 04-O, 06-O, 08-O, hujan — offline penuh | ✅ | Form di `ui/FieldApp.kt`, tersimpan Room (status draf) |
| Konversi muka air → debit (lengkung `Q=C(H−H0)^n`) | ✅ | `logic/FaktorKEngine.kt` sama persis dengan mesin web (`js/engine.js`) |
| Faktor-K + rekomendasi (draf 09-O) | ✅ | Kartu di tab Dashboard |
| Outbox + idempotensi (UUID, unique index bangunan+blangko+waktu) | ✅ | `data/entity/Entities.kt`, `sync/OutboxRepository.kt` |
| Sinkron push via WorkManager (backoff eksponensial) | ✅ | `data/db/SyncWorker.kt` → `POST /api/v1/blangko/dokumen` header `Idempotency-Key` (Bab 6.2) |
| Workflow status draf → terkirim → sinkron | ✅ | Bab 3.3; pengesahan tetap di Admin UPTD (web) |
| Validasi data Bab 3.4 di klien | ✅ | Sama dengan aturan server — nilai salah ditolak sebelum masuk |
| Dashboard ringkas: K, antrean | ✅ | Bab B.2 (Android: ringkasan, bukan penuh) |
| Belum: foto geotag/CameraX, peta, FCM push, work order, survei PAI | ❌ | Fase R0.3/R1.2 (Lampiran B.2 hanya butir itu) |

## Menjalankan di Android Studio

1. Buka folder `simit-android/` sebagai proyek (Android Studio Koala+).
2. **Sync Gradle** (versi sudah dikunci: AGP 8.6.1, Kotlin 2.0.21, Room 2.6.1, WorkManager 2.9.1, minSdk 24).
3. Jalankan `app` di emulator atau HP Android; isi `ID bangunan` misalnya `BND-001`.
4. Uji unit: gradle task `test` (kelas `EngineTest` menghasilkan angka yang sama dengan `node verify.js` di repo web).
5. Ke server: set `SIMIT_API_BASE` melalui env, atau ubah `SyncWorker.BASE_URL`
   (emulator → `http://10.0.2.2:3000`; di perangkat = URL host/API). Backend saat ini masih
   endpoint MVP; endpoint resmi menyusul di Fase integrasi (Bab 6.2 OpenAPI).

## Struktur

- `app/src/main/java/com/dsdajabar/simit/MainActivity.kt` — entry Compose
- `ui/FieldApp.kt`, `ui/FieldViewModel.kt` — UI & state (tab Lapangan / Dashboard)
- `logic/FaktorKEngine.kt` — mesin Faktor-K (satu sumber aturan, identik dengan web)
- `data/entity/Entities.kt`, `data/db/Daos.kt`, `data/db/AppDatabase.kt` + `Seeder` (seed D.I. pilot sama dgn `data/seed.js`)
- `sync/OutboxRepository.kt`, `data/db/SyncWorker.kt` — outbox offline-first + WorkManager
- `app/src/test/java/com/dsdajabar/simit/EngineTest.kt` — uji unit mesin

## Jalan lanjut (pemetaan Lampiran C)

- R0.2–R0.3: kedalaman form (Revisi/Kembalikan Pengamat), draf 05-O/07-O/09-O berversi server, push FCM + deep link (`simit://pembacaan/...`).
- R1.1: mode teknisi (pipeline telemetri), `SyncWorker.BASE_URL` → key API di env.
- R1.2: survei PAI offline, work order, kamera (foto geotag, CameraX).
- Keamanan: aktifkan SQLCipher pada `AppDatabase` (lihat TODO di kode), Android Keystore + PIN/biometrik, mengikuti Bab B.4.
