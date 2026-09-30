package com.mapaurbano.mobile.ui.map

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mapaurbano.mobile.model.ReportStatus
import com.mapaurbano.mobile.model.ReportSummary
import com.mapaurbano.mobile.ui.components.ScreenHeader

@Composable
fun MapScreen(
  viewModel: MapViewModel,
  onReportProblem: () -> Unit,
) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()

  Scaffold(
    topBar = {
      ScreenHeader(
        title = "MuniReport",
        subtitle = "Reportes ciudadanos cerca tuyo",
      )
    },
  ) { innerPadding ->
    when (val currentState = state) {
      MapUiState.Loading ->
        Box(
          modifier = Modifier.fillMaxSize().padding(innerPadding),
          contentAlignment = Alignment.Center,
        ) {
          CircularProgressIndicator()
        }

      is MapUiState.Error ->
        Box(
          modifier = Modifier.fillMaxSize().padding(innerPadding).padding(24.dp),
          contentAlignment = Alignment.Center,
        ) {
          Text(currentState.message)
        }

      is MapUiState.Success ->
        MapContent(
          reports = currentState.reports,
          onReportProblem = onReportProblem,
          contentPadding = innerPadding,
        )
    }
  }
}

@Composable
private fun MapContent(
  reports: List<ReportSummary>,
  onReportProblem: () -> Unit,
  contentPadding: PaddingValues,
) {
  LazyColumn(
    modifier = Modifier.fillMaxSize(),
    contentPadding =
      PaddingValues(
        start = 16.dp,
        top = contentPadding.calculateTopPadding() + 8.dp,
        end = 16.dp,
        bottom = contentPadding.calculateBottomPadding() + 24.dp,
      ),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    item {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        FilterChip(selected = true, onClick = {}, label = { Text("Todos") })
        FilterChip(selected = false, onClick = {}, label = { Text("Pendientes") })
        FilterChip(selected = false, onClick = {}, label = { Text("En proceso") })
      }
    }

    item {
      MapPlaceholder(reportCount = reports.size)
    }

    item {
      Button(
        onClick = onReportProblem,
        modifier = Modifier.fillMaxWidth().height(52.dp),
      ) {
        Icon(imageVector = Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("Reportar un problema")
      }
    }

    item {
      Text("Actividad en tu zona", style = MaterialTheme.typography.titleMedium)
    }

    items(reports.size, key = { index -> reports[index].id }) { index ->
      ReportCard(reports[index])
    }
  }
}

@Composable
private fun MapPlaceholder(reportCount: Int) {
  Box(
    modifier =
      Modifier.fillMaxWidth()
        .height(260.dp)
        .clip(MaterialTheme.shapes.large)
        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.38f))
        .semantics {
          contentDescription =
            "Vista de mapa de demostración con $reportCount reportes. Los mismos reportes están disponibles en la lista inferior."
        },
    contentAlignment = Alignment.Center,
  ) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
      Surface(
        color = MaterialTheme.colorScheme.primary,
        shape = MaterialTheme.shapes.extraLarge,
      ) {
        Icon(
          imageVector = Icons.Outlined.LocationOn,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onPrimary,
          modifier = Modifier.padding(12.dp).size(32.dp),
        )
      }
      Spacer(Modifier.height(12.dp))
      Text("Mapa disponible al integrar el SDK", style = MaterialTheme.typography.titleSmall)
      Text(
        "$reportCount reportes de demostración",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun ReportCard(report: ReportSummary) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    tonalElevation = 2.dp,
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text(report.category.uppercase(), style = MaterialTheme.typography.labelSmall)
        StatusLabel(report.status)
      }
      Text(report.title, style = MaterialTheme.typography.titleMedium)
      Text(
        "${report.location} · ${report.createdLabel}",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun StatusLabel(status: ReportStatus) {
  val color =
    when (status) {
      ReportStatus.PENDING -> Color(0xFFFF8A34)
      ReportStatus.IN_PROGRESS -> MaterialTheme.colorScheme.primary
      ReportStatus.RESOLVED -> Color(0xFF1A9C55)
    }
  Surface(color = color.copy(alpha = 0.14f), shape = MaterialTheme.shapes.extraLarge) {
    Text(
      text = status.label,
      color = color,
      style = MaterialTheme.typography.labelMedium,
      modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
    )
  }
}
