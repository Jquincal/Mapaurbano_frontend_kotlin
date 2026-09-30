package com.mapaurbano.mobile.ui.map

import com.mapaurbano.mobile.data.ReportRepository
import com.mapaurbano.mobile.model.ReportStatus
import com.mapaurbano.mobile.model.ReportSummary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MapViewModelTest {
  @Test
  fun `reports from repository are exposed as success state`() = runTest {
    val expected =
      listOf(
        ReportSummary(
          id = "1",
          title = "Luminaria apagada",
          category = "Alumbrado",
          location = "Plaza Central",
          status = ReportStatus.PENDING,
          createdLabel = "Ahora",
        ),
      )
    val viewModel = MapViewModel(FakeReportRepository(expected))

    assertTrue(viewModel.uiState.value is MapUiState.Loading)
    // Collecting starts the WhileSubscribed stream used by the production UI.
    val result = viewModel.uiState.first { it is MapUiState.Success }

    assertEquals(expected, (result as MapUiState.Success).reports)
  }
}

private class FakeReportRepository(private val reports: List<ReportSummary>) : ReportRepository {
  override fun getPublicReportsStream() = flowOf(reports)
}
