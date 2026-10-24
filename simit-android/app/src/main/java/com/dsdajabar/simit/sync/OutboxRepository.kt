package com.dsdajabar.simit.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.dsdajabar.simit.data.db.AppDatabase
import com.dsdajabar.simit.data.db.PembacaanDao
import com.dsdajabar.simit.data.entity.Pembacaan
import com.dsdajabar.simit.logic.FaktorKEngine
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * Outbox offline-first (Lampiran B.4):
 * - Simpan ke Room dulu (status draf), lalu jadwalkan sinkron saat ada jaringan.
 * - Id idempoten di sisi klien; duplikat (bangunan+blangko+waktu) ditolak Room (unique index).
 */
class OutboxRepository(private val context: Context, private val dao: PembacaanDao) {

  suspend fun tambahPembacaan(
    kodeBlangko: String,
    idBangunan: String,
    diId: String,
    mukaAirCm: Double?,
    debitLps: Double?,
    pengisi: String,
    catatan: String,
    hujanMm: Double? = null,
  ): Result<Pembacaan> {
    // 1) Validasi sisi klien (Bab 3.4) — sama dengan validasiPembacaan() server/web
    val galat = FaktorKEngine.validasiPembacaan(mukaAirCm, debitLps)
    if (galat.isNotEmpty()) return Result.failure(IllegalArgumentException(galat.joinToString("; ")))

    // 2) Konversi muka air -> debit lewat lengkung debit bila debit kosong
    var debit = debitLps
    if (debit == null && mukaAirCm != null && kodeBlangko != "Hujan") {
      debit = FaktorKEngine.debitDariMukaAir(mukaAirCm)
    }

    val p = Pembacaan(
      id = UUID.randomUUID().toString(),
      kodeBlangko = kodeBlangko,
      idBangunan = idBangunan,
      diId = diId,
      waktuIso = OffsetDateTime.now().toString(),
      mukaAirCm = mukaAirCm,
      debitLps = debit,
      hujanMm = hujanMm,
      pengisi = pengisi,
      catatan = catatan,
      status = Pembacaan.STATUS_DRAF,
    )
    val ok = dao.tambah(p) != -1L
    if (!ok) return Result.failure(IllegalStateException("Duplikat: data dengan waktu sama sudah ada"))
    jadwalkanSinkron()
    return Result.success(p)
  }

  /** Push antrean sekarang (manual "kirim") */
  fun jadwalkanSinkron() {
    val constraint = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    val req = OneTimeWorkRequestBuilder<SyncWorker>()
      .setConstraints(constraint)
      .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS) // backoff eksponensial B.4
      .build()
    WorkManager.getInstance(context).enqueueUniqueWork("simit-sync", ExistingWorkPolicy.APPEND_OR_REPLACE, req)
  }

  /** Pemantau: sinkron periodik ketika jaringan tersedia (fallback tanpa FCM) */
  fun jadwalkanSinkronPeriodik() {
    val constraint = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
    val req = PeriodicWorkRequestBuilder<SyncWorker>(6, TimeUnit.HOURS)
      .setConstraints(constraint)
      .build()
    WorkManager.getInstance(context).enqueueUniquePeriodicWork("simit-sync-periodik", ExistingPeriodicWorkPolicy.KEEP, req)
  }
}
