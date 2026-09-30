package com.mapaurbano.mobile.model

enum class ReportStatus(val label: String) {
  PENDING("Pendiente"),
  IN_PROGRESS("En proceso"),
  RESOLVED("Resuelto"),
}

data class ReportSummary(
  val id: String,
  val title: String,
  val category: String,
  val location: String,
  val status: ReportStatus,
  val createdLabel: String,
)
