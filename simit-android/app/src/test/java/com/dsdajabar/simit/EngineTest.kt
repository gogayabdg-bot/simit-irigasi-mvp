package com.dsdajabar.simit

import com.dsdajabar.simit.logic.FaktorKEngine
import com.dsdajabar.simit.logic.FaktorKEngine.Golongan
import com.dsdajabar.simit.logic.FaktorKEngine.Petak
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EngineTest {

  @Test fun `nfr sesuai rumus l per dt per ha`() {
    // ETc = Kc(1.1) x ETo(4.5) = 4.95; (4.95 + 3 + 60 - 1.5)/8.64 = 7.6875
    val n = FaktorKEngine.nfr(1.1)
    assertEquals(7.688, n.nilaiLpsHa, 0.01)
  }

  @Test fun `qbutuh = jumlah A x NFR x FT`() {
    val (detail, total) = FaktorKEngine.kebutuhanAir(listOf(Petak("A", 100.0, "padi", null, 1.3)))
    val nfr = FaktorKEngine.nfr(null).nilaiLpsHa
    assertEquals(100.0 * nfr * 1.3, total, 0.05)
    assertEquals(1, detail.size)
  }

  @Test fun `K sesuai rumus dan ambang kategori`() {
    val h = FaktorKEngine.hitungFaktorK(1000.0, listOf(Petak("A", 100.0, "padi")))
    assertEquals(150.0, h.qhLps, 0.01)
    assertEquals(h.kategori.kode, if (h.nilaiK >= 1.0) "melimpah" else if (h.nilaiK >= 0.7) "sedang" else "kritis")
    assertEquals("melimpah", FaktorKEngine.kategoriK(1.5).kode)
    assertEquals("sedang", FaktorKEngine.kategoriK(0.85).kode)
    assertEquals("kritis", FaktorKEngine.kategoriK(0.5).kode)
  }

  @Test fun `lengkung debit monotonic dan non negatif`() {
    val q1 = FaktorKEngine.debitDariMukaAir(20.0)
    val q2 = FaktorKEngine.debitDariMukaAir(50.0)
    assertTrue(q2 >= q1 && q1 >= 0.0)
  }

  @Test fun `jadwal gilir memenuhi kapasitas harian`() {
    val hari = FaktorKEngine.jadwalGilir(600.0, listOf(Golongan("A", 300.0), Golongan("B", 320.0)))
    val kapasitas = 600.0 * (1 - FaktorKEngine.DEFAULT.qhPorsi)
    for ((_, alokasiList) in hari) {
      val total = alokasiList.sumOf { it.alokasiLps }
      assertTrue(total <= kapasitas + 0.01)
    }
  }

  @Test fun `validasi menolak nilai di luar rentang fisik`() {
    assertTrue(FaktorKEngine.validasiPembacaan(null, -5.0).isNotEmpty())
    assertTrue(FaktorKEngine.validasiPembacaan(3000.0, null).isNotEmpty())
    assertTrue(FaktorKEngine.validasiPembacaan(42.0, 510.0).isEmpty())
  }
}
