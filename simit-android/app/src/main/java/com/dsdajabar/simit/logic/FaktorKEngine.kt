package com.dsdajabar.simit.logic

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Mesin Faktor-K SIMIT — Bab 3.5 dokumen rancangan v2.0.
 * Identik secara numerik dengan js/engine.js pada backend/web MVP sehingga
 * web dan Android menjalankan aturan yang sama (Lampiran B.3 butir 3).
 *
 * Semua parameter disebut "berversi" — saat integrasi server, nilai ini diunduh
 * dari endpoint parameter (Bab C.1), bukan tetap di kode.
 */
object FaktorKEngine {

  data class Params(
    val versi: Int = 1,
    val perkolasiMmHari: Double = 3.0,   // P
    val wlrMmHari: Double = 60.0,        // penggantian lapisan air
    val etoMmHari: Double = 4.5,         // evapotranspirasi acuan (Penman-Monteith FAO)
    val qhPorsi: Double = 0.15,          // Q_h = 15% Q_s (kehilangan primer–sekunder)
    val kcDefault: Double = 1.0,
    val reHujanEfektif: Double = 1.5,    // Re mm/hari
    val ftDefault: Double = 1.3,         // faktor tersier (1,25–1,43)
    val ambangSedang: Double = 0.7,
    val ambangCukup: Double = 1.0,
  )

  val DEFAULT = Params()

  data class Petak(
    val nama: String,
    val luasHa: Double,
    val komoditas: String,
    val kc: Double? = null,   // null -> kcDefault
    val ft: Double? = null,   // null -> ftDefault
  )

  data class Nfr(val nilaiLpsHa: Double, val tcMmHari: Double)

  data class KebutuhanDetail(
    val petak: String,
    val luasHa: Double,
    val komoditas: String,
    val nfrLpsHa: Double,
    val ft: Double,
    val kebutuhanLps: Double,
  )

  data class HasilK(
    val qsLps: Double,
    val qhLps: Double,
    val qTersediaLps: Double,
    val qbutuhTotalLps: Double,
    val detailKebutuhan: List<KebutuhanDetail>,
    val nilaiK: Double,
    val kategori: Kategori,
    val versiParameter: Int,
  )

  data class Kategori(val kode: String, val label: String, val rekomendasi: String)

  /** Ambang K: >= 1.0 Melimpah / 0.7–1.0 Sedang / < 0.7 Kritis */
  fun kategoriK(nilaiK: Double, p: Params = DEFAULT): Kategori = when {
    nilaiK >= p.ambangCukup -> Kategori("melimpah", "Melimpah", "Aliran kontinu sesuai usulan")
    nilaiK >= p.ambangSedang -> Kategori("sedang", "Sedang", "Bukaan proporsional (usulan × K) atau gilir ringan")
    else -> Kategori("kritis", "Kritis", "Gilir air ketat")
  }

  /** NFR (l/dt/ha) = (ETc + P + WLR − Re) / 8,64 ; ETc = Kc × ETo */
  fun nfr(kc: Double? = null, p: Params = DEFAULT): Nfr {
    val kcV = kc ?: p.kcDefault
    val etc = kcV * p.etoMmHari
    val nilai = (etc + p.perkolasiMmHari + p.wlrMmHari - p.reHujanEfektif) / 8.64
    return Nfr(round3(nilai), round3(etc))
  }

  /** Qbutuh = Σ( Aᵢ × NFRᵢ × FTᵢ ) — l/dt */
  fun kebutuhanAir(petak: List<Petak>, p: Params = DEFAULT): Pair<List<KebutuhanDetail>, Double> {
    val detail = petak.map { pt ->
      val n = nfr(pt.kc, p)
      val ft = pt.ft ?: p.ftDefault
      KebutuhanDetail(pt.nama, pt.luasHa, pt.komoditas, n.nilaiLpsHa, ft, round2(pt.luasHa * n.nilaiLpsHa * ft))
    }
    return detail to round2(detail.sumOf { it.kebutuhanLps })
  }

  /** K = (Q_s − Q_h) / Qbutuh */
  fun hitungFaktorK(qsLps: Double, petak: List<Petak>, p: Params = DEFAULT): HasilK {
    require(qsLps > 0) { "Q_s harus > 0 (l/dt)" }
    val (detail, total) = kebutuhanAir(petak, p)
    require(total > 0) { "Q_butuh harus > 0" }
    val qh = round2(qsLps * p.qhPorsi)
    val qTersedia = round2(qsLps - qh)
    val k = round3(qTersedia / total)
    return HasilK(round2(qsLps), qh, qTersedia, total, detail, k, kategoriK(k, p), p.versi)
  }

  /** Lengkung debit sederhana per bangunan: Q = C × (H − H0)^n — l/dt (identik Bab C.1 versi) */
  fun debitDariMukaAir(mukaAirCm: Double, C: Double = 0.025, H0: Double = 10.0, n: Double = 1.6): Double =
    round2(C * max(0.0, mukaAirCm - H0).pow(n) * 1000)

  /** Validasi data (Bab 3.4) — return daftar galat; kosong = lolos */
  fun validasiPembacaan(mukaAirCm: Double?, debitLps: Double?): List<String> {
    val galat = mutableListOf<String>()
    if (debitLps != null && (debitLps < 0 || debitLps > 100000)) galat += "Debit di luar rentang fisik (0–100.000 l/dt)"
    if (mukaAirCm != null && (mukaAirCm < -50 || mukaAirCm > 2000)) galat += "Muka air di luar rentang (-50 cm s/d 20 m)"
    return galat
  }

  /** Jadwal gilir air dasar (Bab 3.5): rotasi per hari, alokasitak melebihi kapasitas */
  data class Golongan(val nama: String, val kebutuhanLps: Double, val prioritas: Int = 1)
  data class Alokasi(val golongan: String, val alokasiLps: Double, val penuh: Boolean)

  fun jadwalGilir(qsLps: Double, golongan: List<Golongan>, p: Params = DEFAULT): Map<Int, List<Alokasi>> {
    val kapasitas = round2(qsLps * (1 - p.qhPorsi))
    require(kapasitas > 0) { "Q_s harus > 0 (l/dt)" }
    val terurut = golongan.sortedWith(compareByDescending<Golongan> { it.prioritas }.thenByDescending { it.kebutuhanLps })
    val hari = mutableMapOf<Int, MutableList<Alokasi>>()
    var idx = 1; var sisa = kapasitas
    val dayList = mutableListOf<Alokasi>()
    // Rollover (Bab 3.5): kebutuhan yang tak terakomodasi hari ini digeser ke hari berikut.
    for (g in terurut) {
      var due = round2(g.kebutuhanLps)
      while (due > 0.005) {
        if (sisa <= 0.005) { hari[idx] = dayList.toList(); idx++; dayList.clear(); sisa = kapasitas }
        val alok = min(due, sisa)
        val sisaDue = round2(due - alok)
        dayList.add(Alokasi(g.nama, round2(alok), sisaDue <= 1e-9))
        due = sisaDue
        sisa = round2(sisa - alok)
      }
    }
    if (dayList.isNotEmpty()) hari[idx] = dayList.toList()
    return hari
  }

  private fun round2(x: Double): Double = (x * 100).roundToInt() / 100.0
  private fun round3(x: Double): Double = (x * 1000).roundToInt() / 1000.0
}
