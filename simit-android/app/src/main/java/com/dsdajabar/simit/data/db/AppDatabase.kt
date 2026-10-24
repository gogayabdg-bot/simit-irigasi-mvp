package com.dsdajabar.simit.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.dsdajabar.simit.data.entity.Bangunan
import com.dsdajabar.simit.data.entity.Di
import com.dsdajabar.simit.data.entity.Pembacaan
import com.dsdajabar.simit.data.entity.Petak

@Database(
  entities = [Di::class, Bangunan::class, Petak::class, Pembacaan::class],
  version = 1,
  exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun masterDao(): MasterDao
  abstract fun pembacaanDao(): PembacaanDao

  companion object {
    @Volatile private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase =
      instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(context, AppDatabase::class.java, "simit.db")
          // Produksi (Bab B.4 keamanan lokal): Room.supportFactory(SQLCipherHelperFactory(...))
          .build()
          .also { instance = it }
      }
  }
}

/** Seed data lokal: sinkron dengan data/seed.js pada web (D.I. pilot contoh) */
object Seeder {
  suspend fun seed(db: AppDatabase) {
    if (db.pembacaanDao().hitung() > 0) return
    db.masterDao().upsertDi(
      listOf(
        Di("DI-PILOT-01", "3201-INV-001", "DI Pilot Citarum Hilir", 2150.0, "UPTD PSDA WS Citarum"),
        Di("DI-02", "3201-INV-002", "DI Sekundang Cimanuk", 1450.0, "UPTD PSDA WS Cimanuk-Cisanggarung"),
      )
    )
    db.masterDao().upsertBangunan(
      listOf(
        Bangunan("BND-001", "DI-PILOT-01", "Intake Induk", "bendung", 6.5),
        Bangunan("BND-002", "DI-PILOT-01", "Bagi Sekunder S-1", "bagi", 3.0),
        Bangunan("BND-003", "DI-PILOT-01", "Sadap Tersier T-12", "sadap", 1.2),
        Bangunan("BND-101", "DI-02", "Intake Sekundang", "bendung", 4.0),
        Bangunan("BND-102", "DI-02", "Bagi S-A", "bagi", 2.2),
      )
    )
    db.masterDao().upsertPetak(
      listOf(
        Petak("T11", "DI-PILOT-01", "Tersier T-11", 320.0, "Padi (generatif)", kc = 1.1),
        Petak("T12", "DI-PILOT-01", "Tersier T-12", 410.0, "Padi (bulir)", kc = 1.15, ft = 1.35),
        Petak("S1", "DI-PILOT-01", "Sekunder S-1", 680.0, "Palawija", kc = 0.9),
        Petak("S2", "DI-PILOT-01", "Sekunder S-2", 740.0, "Padi (pertumbuhan)", kc = 1.05),
        Petak("G-A", "DI-02", "Golongan A", 520.0, "Padi (bulir)", kc = 1.2, ft = 1.4),
        Petak("G-B", "DI-02", "Golongan B", 930.0, "Palawija", kc = 0.95),
      )
    )
    val contoh = Pembacaan(
      id = java.util.UUID.randomUUID().toString(),
      kodeBlangko = "08-O", idBangunan = "BND-001", diId = "DI-PILOT-01",
      waktuIso = java.time.OffsetDateTime.now().withHour(7).withMinute(0).toString(),
      mukaAirCm = 42.0, debitLps = 510.0, pengisi = "POB", status = Pembacaan.STATUS_SINKRON,
    )
    db.pembacaanDao().tambah(contoh)
  }
}
