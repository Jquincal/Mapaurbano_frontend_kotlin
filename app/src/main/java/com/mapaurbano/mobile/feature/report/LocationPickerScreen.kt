package com.mapaurbano.mobile.feature.report

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import com.mapaurbano.mobile.core.location.LocationRequestResult
import com.mapaurbano.mobile.core.location.errorMessageOrNull
import com.mapaurbano.mobile.core.location.rememberLocationRequest
import com.mapaurbano.mobile.core.model.MapPoint
import com.mapaurbano.mobile.feature.common.NoticeCard
import com.mapaurbano.mobile.feature.common.ScrollableFeatureScreen
import com.mapaurbano.mobile.feature.map.InteractiveMap
import com.mapaurbano.mobile.feature.map.coordinateLabel

@Composable
fun LocationPickerScreen(
    initialPoint: MapPoint,
    initialLocation: String,
    modifier: Modifier = Modifier,
    onConfirm: (point: MapPoint, location: String) -> Unit,
    onCancel: () -> Unit,
) {
    var selectedPoint by remember(initialPoint) { mutableStateOf(initialPoint) }
    var location by remember(initialLocation) { mutableStateOf(initialLocation) }
    var cameraTarget by remember(initialPoint) { mutableStateOf<MapPoint?>(initialPoint) }
    var locationMessage by remember { mutableStateOf<String?>(null) }
    var mapInteractionActive by remember { mutableStateOf(false) }
    val locationRequest = rememberLocationRequest { result ->
        when (result) {
            is LocationRequestResult.Success -> {
                selectedPoint = result.point
                cameraTarget = result.point
                location = result.point.coordinateLabel()
                locationMessage = null
            }
            else -> locationMessage = result.errorMessageOrNull()
        }
    }

    ScrollableFeatureScreen(
        title = "Elegir ubicación",
        modifier = modifier.pointerInput(mapInteractionActive) {
            if (mapInteractionActive) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        if (event.type == PointerEventType.Press) {
                            mapInteractionActive = false
                        }
                    }
                }
            }
        },
        onBack = onCancel,
        scrollEnabled = !mapInteractionActive,
    ) {
        OutlinedButton(
            onClick = locationRequest.request,
            enabled = !locationRequest.isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 48.dp),
        ) {
            Icon(Icons.Rounded.MyLocation, contentDescription = null)
            androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
            Text(if (locationRequest.isLoading) "Buscando ubicación…" else "Usar mi ubicación")
        }
        locationMessage?.let { NoticeCard(it) }
        InteractiveMap(
            selectedPoint = selectedPoint,
            editable = true,
            cameraTarget = cameraTarget,
            onPointSelected = { point ->
                selectedPoint = point
                location = point.coordinateLabel()
                locationMessage = null
            },
            onLoadError = {
                locationMessage = "No se pudo cargar el mapa. Revisá la conexión e intentá nuevamente."
            },
            onInteractionChange = { mapInteractionActive = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(330.dp),
        )
        NoticeCard("Tocá el mapa para marcar el punto exacto o usá la ubicación del teléfono.")

        OutlinedTextField(
            value = location,
            onValueChange = { location = it.take(120) },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Referencia de la ubicación") },
            supportingText = { Text("Ejemplo: esquina, plaza o altura aproximada") },
            singleLine = false,
            maxLines = 2,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .sizeIn(minHeight = 48.dp),
            ) {
                Text("Cancelar")
            }
            Button(
                onClick = {
                    val cleanLocation = location.trim()
                    onConfirm(selectedPoint.copy(label = cleanLocation), cleanLocation)
                },
                enabled = location.isNotBlank(),
                modifier = Modifier
                    .weight(1f)
                    .sizeIn(minHeight = 48.dp),
            ) {
                Icon(Icons.Rounded.MyLocation, contentDescription = null)
                androidx.compose.foundation.layout.Spacer(Modifier.width(8.dp))
                Text("Confirmar")
            }
        }
    }
}
