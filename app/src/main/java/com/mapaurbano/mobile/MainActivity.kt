package com.mapaurbano.mobile

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mapaurbano.mobile.data.AppContainer
import com.mapaurbano.mobile.ui.theme.MapaUrbanoTheme

class MainActivity : ComponentActivity() {
  private val appContainer = AppContainer()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      window.isNavigationBarContrastEnforced = false
    }

    setContent {
      MapaUrbanoTheme {
        MapaUrbanoApp(reportRepository = appContainer.reportRepository)
      }
    }
  }
}
