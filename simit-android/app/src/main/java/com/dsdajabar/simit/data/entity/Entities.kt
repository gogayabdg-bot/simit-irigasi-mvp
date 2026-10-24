package com.dsdajabar.simit.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Master: daerah irigasi kewenangan provinsi (Bab 6.1) */
@Entity(tableName = "di")
data class Di(
  @PrimaryKey val id: String,
  val kode: String,
  val nama: String,
  val luasFungsionalHa: Double,
  val uptd: String,
)

@Entity(tableName = "bangunan", indices = [Index("diId")])
data class Bangunan(
  @PrimaryKey val id: String,
  val diId: String,
  val nama: String,
  val jenis: String, // bendung | bagi | sadap (Bab 6.1)
  val kapasitasM3s: Double,
)

@Entity(tableName = "petak", indices = [Index("diId")])
data class Petak(
  @PrimaryKey val id: String,
  val diId: String,
  val nama: String,
  val luasHa: Double,
  val komoditas: String,
  val kc: Double? = null,
  val ft: Double? = null,
)

/**
 * Pembacaan lapangan — append-only (Bab 2.4), status workflow (Bab 3.3):
 * draf -> terkirim -> sinkron.
 * id dibuat di klien (UUID) agar pengiriman ulang idempoten (uuidv7 saat naik API level).
 */
@Entity(tableName = "pembacaan", indices = [Index(value = ["idBangunan", "kodeBlangko", "waktuIso"], unique = true)])
data class Pembacaan(
  @PrimaryKey val id: String,
  val kodeBlangko: String,       // "04-O","06-O","08-O","Hujan"
  val idBangunan: String,
  val diId: String,
  val waktuIso: String,
  val mukaAirCm: Double? = null,
  val debitLps: Double? = null,  // dihitung dari lengkung bila kosong
  val hujanMm: Double? = null,
  val pengisi: String,           // juru | pob
  val catatan: String = "",      // keadaan air & tanaman untuk 04-O
  val status: String = STATUS_DRAF,
  val updatedAt: Long = System.currentTimeMillis(),
) {
  companion object {
    const val STATUS_DRAF = "draf"
    const val STATUS_TERKIRIM = "terkirim"
    const val STATUS_SINKRON = "sinkron" // diterima server
  }
}
