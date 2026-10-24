package com.dsdajabar.simit.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.dsdajabar.simit.data.entity.Bangunan
import com.dsdajabar.simit.data.entity.Di
import com.dsdajabar.simit.data.entity.Pembacaan
import com.dsdajabar.simit.data.entity.Petak
import kotlinx.coroutines.flow.Flow

@Dao
interface MasterDao {
  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertDi(list: List<Di>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertBangunan(list: List<Bangunan>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPetak(list: List<Petak>)

  @Query("SELECT * FROM di ORDER BY nama")
  fun semuaDi(): Flow<List<Di>>

  @Query("SELECT * FROM bangunan WHERE diId = :diId")
  suspend fun bangunanDi(diId: String): List<Bangunan>

  @Query("SELECT * FROM petak WHERE diId = :diId")
  suspend fun petakDi(diId: String): List<Petak>

  @Query("SELECT * FROM bangunan")
  suspend fun semuaBangunan(): List<Bangunan>
}

@Dao
interface PembacaanDao {
  @Insert(onConflict = OnConflictStrategy.IGNORE) // IGNORE = duplikat antrean ditolak (idempoten)
  suspend fun tambah(p: Pembacaan): Long

  @Query("SELECT * FROM pembacaan ORDER BY waktuIso DESC LIMIT 50")
  fun terbaru(): Flow<List<Pembacaan>>

  /** Antrean outbox: semua yang belum diterima server (Bab B.4 push) */
  @Query("SELECT * FROM pembacaan WHERE status != 'sinkron' ORDER BY waktuIso ASC")
  suspend fun antreanOutbox(): List<Pembacaan>

  @Query("UPDATE pembacaan SET status = :status, updatedAt = :waktu WHERE id = :id")
  suspend fun ubahStatus(id: String, status: String, waktu: Long = System.currentTimeMillis())

  @Query("SELECT COUNT(*) FROM pembacaan")
  suspend fun hitung(): Int

  /** Sumber Q_s paritas qsTersediaUntukDI() web: debit 08-O sinkron (terverifikasi) terbaru pada D.I. */
  @Query("SELECT debitLps FROM pembacaan WHERE diId = :diId AND kodeBlangko = '08-O' AND status = 'sinkron' AND debitLps IS NOT NULL ORDER BY waktuIso DESC LIMIT 1")
  suspend fun debit08OTerbaruSinkron(diId: String): Double?
}
