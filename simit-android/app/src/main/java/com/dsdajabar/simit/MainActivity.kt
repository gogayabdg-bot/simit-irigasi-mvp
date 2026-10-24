package com.dsdajabar.simit

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dsdajabar.simit.ui.FieldApp
import com.dsdajabar.simit.ui.FieldViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContent {
      val vm: FieldViewModel = viewModel()
      FieldApp(vm)
    }
  }
}
