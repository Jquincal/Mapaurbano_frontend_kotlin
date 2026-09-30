package com.mapaurbano.mobile.ui.tracking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.mapaurbano.mobile.ui.components.ScreenHeader

@Composable
fun TrackingScreen() {
  var trackingCode by rememberSaveable { mutableStateOf("") }

  Scaffold(
    topBar = {
      ScreenHeader(
        title = "Seguimiento",
        subtitle = "Consultá un reporte anónimo",
      )
    },
  ) { innerPadding ->
    Column(
      modifier =
        Modifier.fillMaxSize()
          .padding(innerPadding)
          .consumeWindowInsets(innerPadding)
          .imePadding()
          .verticalScroll(rememberScrollState())
          .padding(20.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        "Ingresá el código que recibiste al crear el reporte.",
        style = MaterialTheme.typography.bodyLarge,
      )
      OutlinedTextField(
        value = trackingCode,
        onValueChange = { trackingCode = it.uppercase().take(32) },
        label = { Text("Código de seguimiento") },
        placeholder = { Text("7F2K-9B1M-4X3P") },
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      Button(
        onClick = {},
        enabled = trackingCode.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Consultar estado")
      }
      Text(
        "La consulta real se habilitará al conectar la API REST.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
