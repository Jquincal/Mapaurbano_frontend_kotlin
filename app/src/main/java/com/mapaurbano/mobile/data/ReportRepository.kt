package com.mapaurbano.mobile.data

import com.mapaurbano.mobile.model.ReportSummary
import com.mapaurbano.mobile.model.ReportStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface ReportRepository {
  fun getPublicReportsStream(): Flow<List<ReportSummary>>
}

/**
 * Datos locales para desarrollar y demostrar la aplicación sin depender del backend.
 * Se reemplazará por una implementación remota cuando el contrato REST quede publicado.
 */
class FakeReportRepository : ReportRepository {
  private val reports =
    MutableStateFlow(
      listOf(
        ReportSummary(
          id = "demo-1",
          title = "Bache peligroso en carril derecho",
          category = "Baches y calzada",
          location = "Av. San Martín 1420",
          status = ReportStatus.PENDING,
          createdLabel = "Hace 2 horas",
        ),
        ReportSummary(
          id = "demo-2",
          title = "Luminaria apagada en la plaza",
          category = "Alumbrado",
          location = "Plaza Central",
          status = ReportStatus.IN_PROGRESS,
          createdLabel = "Ayer",
        ),
      ),
    )

  override fun getPublicReportsStream(): Flow<List<ReportSummary>> = reports
}

class AppContainer {
  val reportRepository: ReportRepository = FakeReportRepository()
}
