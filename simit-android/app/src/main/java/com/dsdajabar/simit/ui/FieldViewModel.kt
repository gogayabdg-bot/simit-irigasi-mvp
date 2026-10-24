package com.dsdajabar.simit.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.dsdajabar.simit.data.db.AppDatabase
import com.dsdajabar.simit.logic.FaktorKEngine
import com.dsdajabar.simit.logic.FaktorKEngine.HasilK
import com.dsdajabar.simit.sync.OutboxRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FieldViewModel(app: Application) : AndroidViewModel(app) {

  private val db = AppDatabase.get(app)
  private val repo = OutboxRepository(app, db.pembacaanDao())

  val daftarDi = db.masterDao().semuaDi()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  val antrean = db.pembacaanDao().terbaru()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  private val _diTerpilih = MutableStateFlow<String?>("DI-PILOT-01")
  val diIdTerpilih: StateFlow<String?> = _diTerpilih

  private val _hasilK = MutableStateFlow<HasilK?>(null)
  val hasilK: StateFlow<HasilK?> = _hasilK

  init {
    viewModelScope.launch {
      com.dsdajabar.simit.data.db.Seeder.seed(db)
      repo.jadwalkanSinkronPeriodik()
    }
    viewModelScope.launch {
      combine(db.masterDao().semuaDi(), _diTerpilih) { dis, id -> dis to id }
        .collect { (dis, id) ->
          val di = dis.firstOrNull { it.id == id } ?: dis.firstOrNull() ?: return@collect
          _diTerpilih.value = di.id
          val petak = db.masterDao().petakDi(di.id)
          val intake = db.masterDao().semuaBangunan().firstOrNull { it.diId == di.id && it.jenis == "bendung" }
          // Paritas qsTersediaUntukDI (web): primakan 08-O sinkron; fallback kapasitas intake.
          val qs = db.pembacaanDao().debit08OTerbaruSinkron(di.id)
            ?: intake?.kapasitasM3s?.let { it * 1000.0 }
            ?: return@collect
          _hasilK.value = runCatching { FaktorKEngine.hitungFaktorK(qs, petak.map { FaktorKEngine.Petak(it.nama, it.luasHa, it.komoditas, it.kc, it.ft) }) }.getOrNull()
        }
    }
  }

  fun pilihDi(id: String) { _diTerpilih.value = id }

  fun simpan(
    kode: String,
    idBangunan: String?,
    muka: Double?,
    debit: Double?,
    hujan: Double?,
    catatan: String,
    selesai: (Result<*>) -> Unit,
  ) {
    val diId = _diTerpilih.value ?: return
    viewModelScope.launch {
      val bangunanId = idBangunan ?: return@launch
      val r = repo.tambahPembacaan(
        kodeBlangko = kode, idBangunan = bangunanId, diId = diId,
        mukaAirCm = muka, debitLps = debit, pengisi = "juru",
        catatan = catatan, hujanMm = hujan,
      )
      selesai(r)
    }
  }
}
