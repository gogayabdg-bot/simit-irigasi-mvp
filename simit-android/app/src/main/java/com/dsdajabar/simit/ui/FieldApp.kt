package com.dsdajabar.simit.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val brand = Color(0xFF065F46)
private val amber = Color(0xFFD97706)
private val green = Color(0xFF16A34A)
private val abu = Color(0xFF6B7280)
private val kertas = Color(0xFFF9FAFB)

@Composable
fun FieldApp(vm: FieldViewModel) {
  var tab by remember { mutableIntStateOf(0) }
  MaterialTheme {
    Column(Modifier.fillMaxSize().background(kertas)) {
      // Header brand (kontras tinggi untuk cahaya terik, Bab 7.2)
      Column(Modifier.fillMaxWidth().background(brand).padding(16.dp)) {
        Text("SIMIT Lapangan", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("Blangko 04-O · 06-O · 08-O · Hujan", color = Color(0xFFD1FAE5), fontSize = 13.sp)
      }
      TabRow(selectedTabIndex = tab, containerColor = brand, contentColor = Color.White) {
        Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Lapangan") })
        Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Dashboard") })
      }
      if (tab == 0) FormLapangan(vm) else Dashboard(vm)
    }
  }
}

@Composable
private fun FormLapangan(vm: FieldViewModel) {
  val daftar by vm.daftarDi.collectAsState()
  var kode by remember { mutableStateOf("08-O") }
  var diId by remember { mutableStateOf<String?>(null) }
  var bangunanId by remember { mutableStateOf("") }
  var muka by remember { mutableStateOf("") }
  var debit by remember { mutableStateOf("") }
  var hujan by remember { mutableStateOf("") }
  var catatan by remember { mutableStateOf("") }
  var pesan by remember { mutableStateOf("") }

  Column(
    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    // Blangko
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      listOf("08-O", "06-O", "04-O", "Hujan").forEach { k ->
        FilterChip(
          selected = kode == k,
          onClick = { kode = k },
          label = { Text(k) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = brand,
            selectedLabelColor = Color.White,
          ),
        )
      }
    }

    // Pilih D.I.
    Text("Daerah Irigasi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      daftar.forEach { item ->
        FilterChip(
          selected = diId == item.id,
          onClick = { diId = item.id; vm.pilihDi(item.id) },
          label = { Text(item.nama.take(18)) },
          colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = brand,
            selectedLabelColor = Color.White,
          ),
        )
      }
    }

    OutlinedTextField(
      value = bangunanId,
      onValueChange = { bangunanId = it },
      label = { Text("ID bangunan — BND-001 dst. (pintu/bendung di tugas)") },
      modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
      value = muka,
      onValueChange = { muka = it },
      label = { Text("Muka air papan duga (cm)") },
      modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
      value = debit,
      onValueChange = { debit = it },
      label = { Text("Debit (l/dt) — kosong: dihitung otomatis dari lengkung") },
      modifier = Modifier.fillMaxWidth(),
    )
    if (kode == "Hujan") {
      OutlinedTextField(
        value = hujan,
        onValueChange = { hujan = it },
        label = { Text("Hujan (mm)") },
        modifier = Modifier.fillMaxWidth(),
      )
    }
    if (kode == "04-O") {
      OutlinedTextField(
        value = catatan,
        onValueChange = { catatan = it },
        label = { Text("Keadaan air dan tanaman") },
        modifier = Modifier.fillMaxWidth(),
      )
    }

    Button(
      onClick = {
        vm.simpan(
          kode = kode,
          idBangunan = bangunanId.takeIf { it.isNotBlank() },
          muka = muka.toDoubleOrNull(),
          debit = debit.toDoubleOrNull(),
          hujan = if (kode == "Hujan") hujan.toDoubleOrNull() else null,
          catatan = catatan,
        ) { r ->
          pesan = r.fold(
            onSuccess = { "Tersimpan — status draf, masuk antrean sinkron" },
            onFailure = { "Gagal: ${it.message}" },
          )
        }
      },
      modifier = Modifier.fillMaxWidth().height(52.dp),
    ) { Text("Simpan (draf → antre kirim)", fontSize = 16.sp) }

    if (pesan.isNotBlank()) Text(pesan, fontSize = 13.sp, color = abu)
    Text(
      "Offline: data tersimpan di perangkat dan dikirim otomatis (WorkManager) begitu jaringan pulih.",
      fontSize = 11.sp,
      color = abu,
    )
  }
}

@Composable
private fun Dashboard(vm: FieldViewModel) {
  val di by vm.daftarDi.collectAsState()
  val hasil by vm.hasilK.collectAsState()
  val antrean by vm.antrean.collectAsState()
  val diId by vm.diIdTerpilih.collectAsState()

  Column(
    Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // Kartu Faktor-K (draf 09-O, Bab 3.5)
    hasil?.let { h ->
      Card {
        Column(Modifier.padding(16.dp)) {
          Text("Draf Faktor-K", fontSize = 12.sp, color = abu)
          Text(
            "%.2f".format(h.nilaiK),
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold,
            color = warnaKategori(h.kategori.kode),
          )
          Text("${h.kategori.label} — ${h.kategori.rekomendasi}", fontSize = 12.sp)
          Spacer(Modifier.height(8.dp))
          Text(
            "Qs ${h.qsLps} · Qh ${h.qhLps} · Tersedia ${h.qtersediaLps} · Butuh ${h.qbutuhTotalLps} (l/dt)",
            fontSize = 12.sp, color = abu,
          )
        }
      }
    }

    Text("Daerah Irigasi", fontSize = 12.sp, fontWeight = FontWeight.Bold)
    daftar.forEach { item ->
      FilterChip(
        selected = diId == item.id,
        onClick = { vm.pilihDi(item.id) },
        label = { Text(item.nama) },
        colors = FilterChipDefaults.filterChipColors(
          selectedContainerColor = brand,
          selectedLabelColor = Color.White,
        ),
      )
    }

    Text("Pengisian terakhir (antrean verifikasi Pengamat)", fontSize = 14.sp, fontWeight = FontWeight.Bold)
    antrean.take(10).forEach { p ->
      Card {
        Row(
          Modifier.padding(12.dp).fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Column(Modifier.weight(1f)) {
            Text("${p.kodeBlangko} · ${p.idBangunan}", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            Text(
              "${p.waktuIso.take(16).replace('T', ' ')} · ${p.debitLps ?: p.mukaAirCm ?: p.hujanMm ?: "-"}",
              fontSize = 11.sp, color = abu,
            )
          }
          Text(p.status, fontSize = 11.sp, color = Color.White)
        }
      }
    }

    Text("Sinkron otomatis via WorkManager; data tersimpan lokal saat offline.", fontSize = 11.sp, color = abu)
  }
}

private fun warnaKategori(k: String): Color = when (k) {
  "melimpah" -> green
  "sedang" -> amber
  else -> Color(0xFFDC2626)
}
