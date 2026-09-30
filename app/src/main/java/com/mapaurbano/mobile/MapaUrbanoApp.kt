package com.mapaurbano.mobile

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.AddLocationAlt
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.mapaurbano.mobile.data.ReportRepository
import com.mapaurbano.mobile.navigation.AppNavigator
import com.mapaurbano.mobile.navigation.CreateReportRoute
import com.mapaurbano.mobile.navigation.MapRoute
import com.mapaurbano.mobile.navigation.MyReportsRoute
import com.mapaurbano.mobile.navigation.TrackingRoute
import com.mapaurbano.mobile.navigation.rememberNavigationState
import com.mapaurbano.mobile.navigation.topLevelRoutes
import com.mapaurbano.mobile.ui.map.MapScreen
import com.mapaurbano.mobile.ui.map.MapViewModel
import com.mapaurbano.mobile.ui.report.CreateReportScreen
import com.mapaurbano.mobile.ui.reports.MyReportsScreen
import com.mapaurbano.mobile.ui.tracking.TrackingScreen

private data class TopLevelDestination(
  val route: NavKey,
  val label: String,
  val icon: ImageVector,
)

private val destinations =
  listOf(
    TopLevelDestination(MapRoute, "Mapa", Icons.Outlined.Map),
    TopLevelDestination(CreateReportRoute, "Reportar", Icons.Outlined.AddLocationAlt),
    TopLevelDestination(MyReportsRoute, "Mis reportes", Icons.Outlined.Person),
    TopLevelDestination(TrackingRoute, "Seguimiento", Icons.AutoMirrored.Outlined.FactCheck),
  )

@Composable
fun MapaUrbanoApp(
  reportRepository: ReportRepository,
) {
  val navigationState =
    rememberNavigationState(startRoute = MapRoute, topLevelRoutes = topLevelRoutes)
  val navigator = remember(navigationState) { AppNavigator(navigationState) }

  val entries =
    navigationState.toDecoratedEntries(
      entryProvider {
        entry<MapRoute> {
          val mapViewModel: MapViewModel = viewModel { MapViewModel(reportRepository) }
          MapScreen(viewModel = mapViewModel, onReportProblem = { navigator.navigate(CreateReportRoute) })
        }
        entry<CreateReportRoute> { CreateReportScreen() }
        entry<MyReportsRoute> { MyReportsScreen() }
        entry<TrackingRoute> { TrackingScreen() }
      },
    )

  Scaffold(
    contentWindowInsets = WindowInsets(0),
    bottomBar = {
      NavigationBar {
        destinations.forEach { destination ->
          NavigationBarItem(
            selected = navigationState.topLevelRoute == destination.route,
            onClick = { navigator.navigate(destination.route) },
            icon = {
              Icon(imageVector = destination.icon, contentDescription = null)
            },
            label = { Text(destination.label) },
          )
        }
      }
    },
  ) { innerPadding ->
    NavDisplay(
      entries,
      onBack = { navigator.goBack() },
      modifier = Modifier.fillMaxSize().padding(innerPadding).consumeWindowInsets(innerPadding),
    )
  }
}
