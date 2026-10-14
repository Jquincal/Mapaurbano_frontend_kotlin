package com.mapaurbano.mobile.feature.map

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.mapaurbano.mobile.core.data.DemoContent
import com.mapaurbano.mobile.core.designsystem.MapaUrbanoTheme
import com.mapaurbano.mobile.core.model.MapPresentation
import com.mapaurbano.mobile.core.model.ReportFilters
import org.junit.Rule
import org.junit.Test

class MapScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mapShowsPublicReportsAndCreateAction() {
        composeRule.setContent {
            MapaUrbanoTheme {
                MapScreen(
                    reports = DemoContent.reports,
                    categories = DemoContent.categories,
                    filters = ReportFilters(),
                    presentation = MapPresentation.Map,
                    onQueryChange = {},
                    onPresentationChange = {},
                    onStatusFilterChange = {},
                    onCategoryFilterChange = {},
                    onClearFilters = {},
                    onReportClick = {},
                    onCreateReport = {},
                )
            }
        }

        composeRule.onNodeWithText("Mapa urbano").assertExists()
        composeRule.onNodeWithText("Crear un reporte").assertExists()
        composeRule.onNodeWithText("Bache profundo frente al cruce").assertExists()
    }

    @Test
    fun emptyResultsKeepMapAndCreateActionVisible() {
        composeRule.setContent {
            MapaUrbanoTheme {
                MapScreen(
                    reports = emptyList(),
                    categories = DemoContent.categories,
                    filters = ReportFilters(),
                    presentation = MapPresentation.Map,
                    onQueryChange = {},
                    onPresentationChange = {},
                    onStatusFilterChange = {},
                    onCategoryFilterChange = {},
                    onClearFilters = {},
                    onReportClick = {},
                    onCreateReport = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Centrar mapa").assertExists()
        composeRule.onNodeWithText("Todavía no hay reportes").assertExists()
        composeRule.onNodeWithText("Reportar problema").assertExists()
    }
}
