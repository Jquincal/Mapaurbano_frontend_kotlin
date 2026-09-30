package com.mapaurbano.mobile

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.mapaurbano.mobile.data.FakeReportRepository
import com.mapaurbano.mobile.ui.theme.MapaUrbanoTheme
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MapaUrbanoAppTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Before
  fun setUp() {
    composeTestRule.setContent {
      MapaUrbanoTheme {
        MapaUrbanoApp(reportRepository = FakeReportRepository())
      }
    }
  }

  @Test
  fun bottomNavigation_opensReportForm() {
    composeTestRule.onNodeWithText("Mapa").assertIsDisplayed()
    composeTestRule.onNodeWithText("Reportar").performClick()
    composeTestRule.onNodeWithText("Nuevo reporte").assertIsDisplayed()
  }
}
