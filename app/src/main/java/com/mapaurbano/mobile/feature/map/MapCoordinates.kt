package com.mapaurbano.mobile.feature.map

import com.mapaurbano.mobile.core.model.MapPoint
import java.util.Locale

fun MapPoint.hasUsableCoordinates(): Boolean =
    latitude.isFinite() &&
        longitude.isFinite() &&
        latitude in -90.0..90.0 &&
        longitude in -180.0..180.0 &&
        !(latitude == 0.0 && longitude == 0.0)

fun MapPoint.coordinateLabel(): String = String.format(
    Locale.US,
    "%.5f, %.5f",
    latitude,
    longitude,
)
