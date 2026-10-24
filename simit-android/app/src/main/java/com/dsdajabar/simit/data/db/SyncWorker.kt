package com.dsdajabar.simit.data.db

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dsdajabar.simit.BuildConfig
import com.dsdajabar.simit.data.entity.Pembacaan
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sinkronisasi push (Lampiran B.4): kirim antrean outbox berurutan, satu per permintaan POST,
 * dengan uji idempoten di server. Draf/terkirim yang gagal tetap di antrean; WorkManager
 * akan mencoba ulang dengan backoff eksponensial.
 */
class SyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {

  override suspend fun doWork(): Result {
    val dao = AppDatabase.get(applicationContext).pembacaanDao()
    val antrean = dao.antreanOutbox()
    if (antrean.isEmpty()) return Result.success()

    var gagal = 0
    for (p in antrean) {
      dao.ubahStatus(p.id, Pembacaan.STATUS_TERKIRIM) // mark sebelum kirim
      val sukses = kirim(p)
      if (sukses) {
        dao.ubahStatus(p.id, Pembacaan.STATUS_SINKRON)
      } else {
        gagal++
      }
    }
    return if (gagal == 0) Result.success() else Result.retry() // ulang dari antrean tersisa
  }

  /**
   * POST ke endpoint blangko (Bab 6.2): POST /api/v1/blangko/dokumen
   * TODO Fase integrasi: gunakan OIDC token (Authorization: Bearer ...):
   */
  private suspend fun kirim(p: Pembacaan): Boolean {
    val url = URL(BASE_URL + "/api/v1/blangko/dokumen")
    return runCatching {
      val body = JSONObject().apply {
        put("idempotency_key", p.id)
        put("kode_blangko", p.kodeBlangko)
        put("id_bangunan", p.idBangunan)
        put("id_di", p.diId)
        put("waktu", p.waktuIso)
        put("muka_air_cm", p.mukaAirCm ?: JSONObject.NULL)
        put("debit_lps", p.debitLps ?: JSONObject.NULL)
        put("hujan_mm", p.hujanMm ?: JSONObject.NULL)
        put("pengisi", p.pengisi)
        put("catatan", p.catatan)
      }
      (url.openConnection() as HttpURLConnection).apply {
        requestMethod = "POST"
        connectTimeout = 10_000
        readTimeout = 20_000
        setRequestProperty("Content-Type", "application/json")
        setRequestProperty("Idempotency-Key", p.id) // header idempoten Bab 6.2
        doOutput = true
        outputStream.use { it.write(body.toString().toByteArray()) }
      }.responseCode in 200..299
    }.getOrDefault(false)
  }

  companion object {
    // Endpoint dari BuildConfig (atur via SIMIT_API_BASE di gradle.properties / -P), bukan env device
    var BASE_URL: String = BuildConfig.SIMIT_API_BASE
  }
}
