package com.mapaurbano.mobile.feature.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mapaurbano.mobile.core.designsystem.MuniColors
import com.mapaurbano.mobile.core.model.Report
import com.mapaurbano.mobile.feature.common.MiniMapIllustration
import com.mapaurbano.mobile.feature.common.ScrollableFeatureScreen
import com.mapaurbano.mobile.feature.common.StitchCard
import com.mapaurbano.mobile.feature.common.StitchPrimaryButton

@Composable
fun ConfirmationScreen(
    report: Report,
    modifier: Modifier = Modifier,
    onOpenReport: () -> Unit,
    onCreateAnother: () -> Unit,
    onGoToMap: () -> Unit,
) {
    ScrollableFeatureScreen(
        title = "Confirmación",
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(MuniColors.Green100, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(46.dp),
                    tint = ColorTokens.SuccessInk,
                )
            }
            Text(
                text = "¡Reporte enviado con éxito!",
                modifier = Modifier.semantics { heading() },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "La incidencia quedo registrada como ${report.status.label.lowercase()}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MuniColors.OnSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }

        StitchCard(contentPadding = PaddingValues(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("COMPROBANTE MUNICIPAL", style = MaterialTheme.typography.labelSmall, color = MuniColors.OnSurfaceVariant)
                Text(report.createdAtLabel, style = MaterialTheme.typography.labelSmall, color = MuniColors.OnSurfaceVariant)
            }
            StitchCard(containerColor = MuniColors.PrimaryFixed, contentPadding = PaddingValues(12.dp)) {
                Text("Vinculado a tu perfil cívico", style = MaterialTheme.typography.labelLarge, color = MuniColors.Primary)
                Text("Podrás consultar los avances desde Mis reportes.", style = MaterialTheme.typography.bodySmall)
            }
            DetailLine("Numero de reporte", report.id)
            DetailLine("Categoría", report.category.label)
            DetailLine("Ubicacion", report.location)
            MiniMapIllustration(modifier = Modifier.height(106.dp))
        }

        StitchPrimaryButton(
            text = "Ver detalle del reporte",
            onClick = onOpenReport,
            icon = Icons.Rounded.OpenInNew,
        )
        OutlinedButton(
            onClick = onGoToMap,
            modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
        ) {
            Icon(Icons.Rounded.Home, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Volver al mapa")
        }
        TextButton(
            onClick = onCreateAnother,
            modifier = Modifier.fillMaxWidth().sizeIn(minHeight = 48.dp),
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("Crear otro reporte")
        }
    }
}

@Composable
private fun DetailLine(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MuniColors.OnSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
    }
}

private object ColorTokens {
    val SuccessInk = androidx.compose.ui.graphics.Color(0xFF005323)
}
