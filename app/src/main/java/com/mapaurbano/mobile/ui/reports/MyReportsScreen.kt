package com.mapaurbano.mobile.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mapaurbano.mobile.ui.components.ScreenHeader

@Composable
fun MyReportsScreen() {
  Scaffold(
    topBar = {
      ScreenHeader(
        title = "Mis reportes",
        subtitle = "Consultá los reportes asociados a tu cuenta",
      )
    },
  ) { innerPadding ->
    Column(
      modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
    ) {
      Icon(
        imageVector = Icons.Outlined.Person,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
      )
      Text(
        "Ingresá para ver tus reportes",
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(top = 16.dp),
      )
      Text(
        "Los reportes anónimos se consultan desde Seguimiento usando su código.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(vertical = 12.dp),
      )
      Button(onClick = {}) { Text("Iniciar sesión") }
    }
  }
}
