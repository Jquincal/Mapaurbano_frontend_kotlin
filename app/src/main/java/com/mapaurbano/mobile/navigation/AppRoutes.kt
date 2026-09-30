package com.mapaurbano.mobile.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable data object MapRoute : NavKey

@Serializable data object CreateReportRoute : NavKey

@Serializable data object MyReportsRoute : NavKey

@Serializable data object TrackingRoute : NavKey

val topLevelRoutes: Set<NavKey> =
  linkedSetOf(MapRoute, CreateReportRoute, MyReportsRoute, TrackingRoute)
