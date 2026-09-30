package com.mapaurbano.mobile.ui.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapaurbano.mobile.ui.components.ScreenHeader

@Composable
fun CreateReportScreen() {
  var title by rememberSaveable { mutableStateOf("") }
  var description by rememberSaveable { mutableStateOf("") }
  var selectedCategory by rememberSaveable { mutableStateOf("Baches") }
  var anonymous by rememberSaveable { mutableStateOf(true) }

  Scaffold(
    topBar = {
      ScreenHeader(
        title = "Nuevo reporte",
        subtitle = "Contanos qué sucede y dónde",
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
          .padding(horizontal = 20.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text("Modalidad", style = MaterialTheme.typography.titleMedium)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(
          selected = anonymous,
          onClick = { anonymous = true },
          label = { Text("Anónimo") },
        )
        FilterChip(
          selected = !anonymous,
          onClick = { anonymous = false },
          label = { Text("Con mi cuenta") },
        )
      }
      Text(
        if (anonymous) {
          "Recibirás un código para consultar el estado del reporte."
        } else {
          "El reporte aparecerá en Mis reportes después de iniciar sesión."
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )

      OutlinedTextField(
        value = title,
        onValueChange = { title = it.take(150) },
        label = { Text("Título") },
        supportingText = { Text("${title.length}/150") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
      )
      OutlinedTextField(
        value = description,
        onValueChange = { description = it },
        label = { Text("Descripción") },
        minLines = 4,
        modifier = Modifier.fillMaxWidth(),
      )

      Text("Categoría", style = MaterialTheme.typography.titleMedium)
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("Baches", "Alumbrado", "Limpieza").forEach { category ->
          FilterChip(
            selected = selectedCategory == category,
            onClick = { selectedCategory = category },
            label = { Text(category) },
          )
        }
      }

      OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Text("Seleccionar ubicación en el mapa")
      }
      OutlinedButton(onClick = {}, modifier = Modifier.fillMaxWidth()) {
        Text("Agregar una fotografía")
      }
      Button(
        onClick = {},
        enabled = title.isNotBlank() && description.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
      ) {
        Text("Enviar reporte")
      }
      Text(
        "El envío real se habilitará al conectar la API REST.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}
