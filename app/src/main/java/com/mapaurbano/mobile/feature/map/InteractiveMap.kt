package com.mapaurbano.mobile.feature.map

import android.os.Bundle
import android.view.MotionEvent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.MarkerOptions
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import com.mapaurbano.mobile.core.model.MapPoint
import com.mapaurbano.mobile.core.model.Report

private const val OPEN_FREE_MAP_STYLE = "https://tiles.openfreemap.org/styles/liberty"
private const val DEFAULT_LATITUDE = -32.8895
private const val DEFAULT_LONGITUDE = -68.8458
private const val DEFAULT_ZOOM = 13.5

@Composable
fun InteractiveMap(
    modifier: Modifier = Modifier,
    reports: List<Report> = emptyList(),
    selectedReportId: String? = null,
    selectedPoint: MapPoint? = null,
    userLocation: MapPoint? = null,
    editable: Boolean = false,
    cameraTarget: MapPoint? = null,
    onReportClick: (Report) -> Unit = {},
    onPointSelected: (MapPoint) -> Unit = {},
    onLoadError: (String) -> Unit = {},
    onInteractionChange: (Boolean) -> Unit = {},
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val latestReports by rememberUpdatedState(reports)
    val latestOnReportClick by rememberUpdatedState(onReportClick)
    val latestOnPointSelected by rememberUpdatedState(onPointSelected)
    val latestOnLoadError by rememberUpdatedState(onLoadError)
    val latestOnInteractionChange by rememberUpdatedState(onInteractionChange)
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleReady by remember { mutableStateOf(false) }

    val mapView = remember(context) {
        MapLibre.getInstance(context.applicationContext)
        MapView(context).also { it.onCreate(Bundle()) }
    }

    DisposableEffect(mapView, lifecycle) {
        var started = false
        var resumed = false

        fun startIfNeeded() {
            if (!started) {
                mapView.onStart()
                started = true
            }
        }

        fun resumeIfNeeded() {
            startIfNeeded()
            if (!resumed) {
                mapView.onResume()
                resumed = true
            }
        }

        fun pauseIfNeeded() {
            if (resumed) {
                mapView.onPause()
                resumed = false
            }
        }

        fun stopIfNeeded() {
            pauseIfNeeded()
            if (started) {
                mapView.onStop()
                started = false
            }
        }

        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> startIfNeeded()
                Lifecycle.Event.ON_RESUME -> resumeIfNeeded()
                Lifecycle.Event.ON_PAUSE -> pauseIfNeeded()
                Lifecycle.Event.ON_STOP -> stopIfNeeded()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        when {
            lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) -> resumeIfNeeded()
            lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) -> startIfNeeded()
        }

        mapView.getMapAsync { loadedMap ->
            map = loadedMap
            loadedMap.cameraPosition = CameraPosition.Builder()
                .target(LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE))
                .zoom(DEFAULT_ZOOM)
                .build()
            loadedMap.setStyle(OPEN_FREE_MAP_STYLE) {
                styleReady = true
            }
            loadedMap.setOnMarkerClickListener { marker ->
                val report = latestReports.firstOrNull { it.id == marker.snippet }
                if (report != null) {
                    latestOnReportClick(report)
                    true
                } else {
                    false
                }
            }
        }
        val failureListener = MapView.OnDidFailLoadingMapListener { error ->
            latestOnLoadError(error)
        }
        mapView.addOnDidFailLoadingMapListener(failureListener)
        mapView.setOnTouchListener { view, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                view.parent?.requestDisallowInterceptTouchEvent(true)
                latestOnInteractionChange(true)
            }
            false
        }

        onDispose {
            mapView.setOnTouchListener(null)
            mapView.removeOnDidFailLoadingMapListener(failureListener)
            lifecycle.removeObserver(observer)
            stopIfNeeded()
            mapView.onDestroy()
        }
    }

    DisposableEffect(map, editable) {
        val loadedMap = map
        if (loadedMap == null || !editable) {
            onDispose { }
        } else {
            val listener = MapLibreMap.OnMapClickListener { latLng ->
                latestOnPointSelected(
                    MapPoint(
                        x = 0.5f,
                        y = 0.5f,
                        latitude = latLng.latitude,
                        longitude = latLng.longitude,
                        label = "",
                    ),
                )
                true
            }
            loadedMap.addOnMapClickListener(listener)
            onDispose { loadedMap.removeOnMapClickListener(listener) }
        }
    }

    LaunchedEffect(map, styleReady, reports, selectedReportId, selectedPoint, userLocation) {
        val loadedMap = map ?: return@LaunchedEffect
        if (!styleReady) return@LaunchedEffect

        loadedMap.clear()
        reports.filter { it.point.hasUsableCoordinates() }.forEach { report ->
            loadedMap.addMarker(
                MarkerOptions()
                    .position(LatLng(report.point.latitude, report.point.longitude))
                    .title(report.title)
                    .snippet(report.id),
            )
        }
        selectedPoint?.takeIf(MapPoint::hasUsableCoordinates)?.let { point ->
            loadedMap.addMarker(
                MarkerOptions()
                    .position(LatLng(point.latitude, point.longitude))
                    .title("Ubicación seleccionada"),
            )
        }
        userLocation?.takeIf(MapPoint::hasUsableCoordinates)?.let { point ->
            loadedMap.addMarker(
                MarkerOptions()
                    .position(LatLng(point.latitude, point.longitude))
                    .title("Mi ubicación"),
            )
        }
    }

    LaunchedEffect(map, cameraTarget) {
        val loadedMap = map ?: return@LaunchedEffect
        val target = cameraTarget?.takeIf(MapPoint::hasUsableCoordinates) ?: return@LaunchedEffect
        loadedMap.animateCamera(
            CameraUpdateFactory.newLatLngZoom(
                LatLng(target.latitude, target.longitude),
                16.0,
            ),
        )
    }

    AndroidView(
        factory = { mapView },
        modifier = modifier.semantics {
            contentDescription = if (editable) {
                "Mapa interactivo para elegir una ubicación"
            } else {
                "Mapa urbano interactivo"
            }
        },
    )
}
