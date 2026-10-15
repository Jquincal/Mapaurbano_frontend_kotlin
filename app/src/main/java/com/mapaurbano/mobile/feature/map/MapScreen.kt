package com.mapaurbano.mobile.feature.map

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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.List
import androidx.compose.material.icons.rounded.Map
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mapaurbano.mobile.core.designsystem.MuniColors
import com.mapaurbano.mobile.core.location.LocationRequestResult
import com.mapaurbano.mobile.core.location.errorMessageOrNull
import com.mapaurbano.mobile.core.location.rememberLocationRequest
import com.mapaurbano.mobile.core.model.Category
import com.mapaurbano.mobile.core.model.ContentLoadState
import com.mapaurbano.mobile.core.model.MapPresentation
import com.mapaurbano.mobile.core.model.Report
import com.mapaurbano.mobile.core.model.ReportFilters
import com.mapaurbano.mobile.core.model.ReportStatus
import com.mapaurbano.mobile.feature.common.ContentState
import com.mapaurbano.mobile.feature.common.ContentStatePanel
import com.mapaurbano.mobile.feature.common.DefaultTopBarActions
import com.mapaurbano.mobile.feature.common.FeatureTopBar
import com.mapaurbano.mobile.feature.common.NoticeCard
import com.mapaurbano.mobile.feature.common.StitchCard
import com.mapaurbano.mobile.feature.common.StitchChip
import com.mapaurbano.mobile.feature.common.StitchReportCard
import com.mapaurbano.mobile.feature.common.StitchSearchField
import com.mapaurbano.mobile.feature.common.StitchStatusPill

@Composable
fun MapScreen(
    reports: List<Report>,
    categories: List<Category>,
    filters: ReportFilters,
    presentation: MapPresentation,
    modifier: Modifier = Modifier,
    contentLoadState: ContentLoadState = ContentLoadState.Ready,
    contentErrorMessage: String? = null,
    onQueryChange: (String) -> Unit,
    onPresentationChange: (MapPresentation) -> Unit,
    onStatusFilterChange: (ReportStatus?) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onReportClick: (Report) -> Unit,
    onReportSelect: (Report) -> Unit = onReportClick,
    onCreateReport: () -> Unit,
    onRetry: () -> Unit = {},
    selectedReportId: String? = null,
    onProfile: () -> Unit = {},
    onNotifications: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            FeatureTopBar(
                title = "Mapa",
                actions = {
                    DefaultTopBarActions(
                        onProfile = onProfile,
                        onNotifications = onNotifications,
                    )
                },
            )
        },
    ) { innerPadding ->
        when (contentLoadState) {
            ContentLoadState.Loading -> ContentStatePanel(
                state = ContentState.Loading,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
            )

            ContentLoadState.Error -> ContentStatePanel(
                state = ContentState.Error(
                    contentErrorMessage ?: "No pudimos cargar el mapa. Intentá nuevamente.",
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                onRetry = onRetry,
            )

            ContentLoadState.Ready -> MapReadyContent(
                reports = reports,
                categories = categories,
                filters = filters,
                presentation = presentation,
                onQueryChange = onQueryChange,
                onPresentationChange = onPresentationChange,
                onStatusFilterChange = onStatusFilterChange,
                onCategoryFilterChange = onCategoryFilterChange,
                onClearFilters = onClearFilters,
                onReportClick = onReportClick,
                onReportSelect = onReportSelect,
                onCreateReport = onCreateReport,
                selectedReportId = selectedReportId,
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun MapReadyContent(
    reports: List<Report>,
    categories: List<Category>,
    filters: ReportFilters,
    presentation: MapPresentation,
    onQueryChange: (String) -> Unit,
    onPresentationChange: (MapPresentation) -> Unit,
    onStatusFilterChange: (ReportStatus?) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
    onReportClick: (Report) -> Unit,
    onReportSelect: (Report) -> Unit,
    onCreateReport: () -> Unit,
    selectedReportId: String?,
    modifier: Modifier = Modifier,
) {
    val selectedReport = reports.firstOrNull { it.id == selectedReportId } ?: reports.firstOrNull()
    var cameraTarget by remember { mutableStateOf<com.mapaurbano.mobile.core.model.MapPoint?>(null) }
    var userLocation by remember { mutableStateOf<com.mapaurbano.mobile.core.model.MapPoint?>(null) }
    var mapMessage by remember { mutableStateOf<String?>(null) }
    var mapInteractionActive by remember { mutableStateOf(false) }
    val screenScrollState = rememberScrollState()
    val locationRequest = rememberLocationRequest { result ->
        when (result) {
            is LocationRequestResult.Success -> {
                userLocation = result.point
                cameraTarget = result.point
                mapMessage = null
            }
            else -> mapMessage = result.errorMessageOrNull()
        }
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(mapInteractionActive) {
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
            }
            .verticalScroll(screenScrollState, enabled = !mapInteractionActive)
            .padding(horizontal = 14.dp, vertical = 10.dp)
            .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StitchSearchField(
            value = filters.query,
            onValueChange = onQueryChange,
            placeholder = "Buscar calle, barrio o zona...",
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StitchChip(
                label = "Mapa",
                selected = presentation == MapPresentation.Map,
                icon = Icons.Rounded.Map,
                onClick = { onPresentationChange(MapPresentation.Map) },
            )
            StitchChip(
                label = "Lista",
                selected = presentation == MapPresentation.List,
                icon = Icons.Rounded.List,
                onClick = { onPresentationChange(MapPresentation.List) },
            )
            Spacer(Modifier.weight(1f))
            FilterMenu(
                filters = filters,
                categories = categories,
                onStatusFilterChange = onStatusFilterChange,
                onCategoryFilterChange = onCategoryFilterChange,
                onClearFilters = onClearFilters,
            )
        }

        if (presentation == MapPresentation.Map) {
            Box {
                InteractiveMap(
                    reports = reports,
                    selectedReportId = selectedReportId,
                    onReportClick = onReportSelect,
                    userLocation = userLocation,
                    cameraTarget = cameraTarget,
                    onLoadError = {
                        mapMessage = "No se pudo cargar el mapa. Revisá la conexión e intentá nuevamente."
                    },
                    onInteractionChange = { mapInteractionActive = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(430.dp),
                )
                FilledTonalIconButton(
                    onClick = locationRequest.request,
                    enabled = !locationRequest.isLoading,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp, bottom = 14.dp)
                        .size(52.dp),
                ) {
                    if (locationRequest.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Rounded.MyLocation, contentDescription = "Centrar mapa")
                    }
                }
                Button(
                    onClick = onCreateReport,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 14.dp, bottom = 72.dp)
                        .sizeIn(minHeight = 52.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = ButtonDefaults.buttonColors(containerColor = MuniColors.PrimaryBright),
                    contentPadding = PaddingValues(horizontal = 18.dp),
                ) {
                    Icon(Icons.Rounded.AddLocationAlt, contentDescription = null)
                    Spacer(Modifier.size(8.dp))
                    Text("Reportar problema", style = MaterialTheme.typography.labelLarge)
                }
            }
            mapMessage?.let { NoticeCard(it) }
            if (reports.isEmpty()) {
                EmptyMapResults(
                    hasActiveFilters = filters.hasActiveFilters,
                    onClearFilters = onClearFilters,
                    onCreateReport = onCreateReport,
                )
            } else {
                selectedReport?.let {
                    StitchMapSheet(report = it, onClick = { onReportClick(it) })
                }
                CivicStats(reports = reports)
            }
        } else {
            if (reports.isEmpty()) {
                EmptyMapResults(
                    hasActiveFilters = filters.hasActiveFilters,
                    onClearFilters = onClearFilters,
                    onCreateReport = onCreateReport,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "${reports.size} reportes registrados",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.semantics { heading() },
                    )
                    reports.forEach { report ->
                        StitchReportCard(report = report, onClick = { onReportClick(report) })
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterMenu(
    filters: ReportFilters,
    categories: List<Category>,
    onStatusFilterChange: (ReportStatus?) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onClearFilters: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val activeFilterCount = listOfNotNull(filters.status, filters.categoryId).size

    Box {
        StitchChip(
            label = if (activeFilterCount == 0) "Filtros" else "Filtros ($activeFilterCount)",
            selected = expanded || activeFilterCount > 0,
            icon = Icons.Rounded.Tune,
            selectedColor = MuniColors.Orange,
            onClick = { expanded = !expanded },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.widthIn(min = 250.dp, max = 320.dp),
        ) {
            FilterMenuHeading("Estado")
            FilterMenuItem(
                label = "Todos los reportes",
                selected = filters.status == null,
                onClick = { onStatusFilterChange(null) },
            )
            ReportStatus.entries.forEach { status ->
                FilterMenuItem(
                    label = status.label,
                    selected = filters.status == status,
                    onClick = { onStatusFilterChange(status) },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
            FilterMenuHeading("Categoría")
            FilterMenuItem(
                label = "Todas las categorías",
                selected = filters.categoryId == null,
                onClick = { onCategoryFilterChange(null) },
            )
            categories.forEach { category ->
                FilterMenuItem(
                    label = category.label,
                    selected = filters.categoryId == category.id,
                    onClick = { onCategoryFilterChange(category.id) },
                )
            }

            if (activeFilterCount > 0) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Limpiar filtros",
                            color = MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.SemiBold,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.ClearAll,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    },
                    onClick = {
                        onClearFilters()
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun FilterMenuHeading(label: String) {
    Text(
        text = label.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    )
}

@Composable
private fun FilterMenuItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label) },
        leadingIcon = {
            if (selected) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            } else {
                Spacer(Modifier.size(24.dp))
            }
        },
        onClick = onClick,
        modifier = Modifier.semantics { this.selected = selected },
    )
}

@Composable
private fun StitchMapSheet(report: Report, onClick: () -> Unit) {
    StitchCard {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(report.category.label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(report.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(report.location, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            StitchStatusPill(report.status)
        }
        Text(report.description, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
        Button(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 46.dp),
            shape = MaterialTheme.shapes.extraLarge,
        ) {
            Text("Ver detalle completo")
        }
    }
}

@Composable
private fun CivicStats(reports: List<Report>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        listOf(
            "Activos" to reports.count { it.status != ReportStatus.Resolved }.toString(),
            "Resueltos" to reports.count { it.status == ReportStatus.Resolved }.toString(),
            "Zonas" to reports.map { it.location }.distinct().size.toString(),
        ).forEach { (label, value) ->
            StitchCard(modifier = Modifier.weight(1f), contentPadding = PaddingValues(10.dp), containerColor = MuniColors.SurfaceContainerLow) {
                Text(value, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Text(label.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun MapReportCard(report: Report, onClick: () -> Unit, modifier: Modifier = Modifier) {
    StitchReportCard(report = report, onClick = onClick, modifier = modifier)
}

@Composable
fun ReportStatusBadge(status: ReportStatus, modifier: Modifier = Modifier) {
    StitchStatusPill(status = status, modifier = modifier)
}

@Composable
private fun EmptyMapResults(
    hasActiveFilters: Boolean,
    onClearFilters: () -> Unit,
    onCreateReport: () -> Unit,
) {
    StitchCard(containerColor = MuniColors.SurfaceContainerLow) {
        Text(
            if (hasActiveFilters) "No encontramos coincidencias" else "Todavía no hay reportes",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            if (hasActiveFilters) {
                "Probá cambiando la búsqueda o limpiando los filtros."
            } else {
                "El mapa está listo. Podés crear el primer reporte vecinal."
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (hasActiveFilters) {
                Button(onClick = onClearFilters, modifier = Modifier.weight(1f)) { Text("Limpiar") }
            }
            Button(onClick = onCreateReport, modifier = Modifier.weight(1f)) { Text("Reportar") }
        }
    }
}

private val ReportFilters.hasActiveFilters: Boolean
    get() = query.isNotBlank() || status != null || categoryId != null
