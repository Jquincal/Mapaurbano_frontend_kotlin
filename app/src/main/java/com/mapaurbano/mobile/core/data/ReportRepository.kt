package com.mapaurbano.mobile.core.data

import com.mapaurbano.mobile.core.model.Report

interface ReportRepository {
    fun getReports(): List<Report>

    fun save(report: Report)
}

/**
 * Repositorio en memoria para desarrollar y demostrar la interfaz sin depender del backend.
 * Puede reemplazarse por una implementación REST sin acoplar las pantallas a la red.
 */
class FakeReportRepository(
    initialReports: List<Report> = DemoContent.reports,
) : ReportRepository {
    private val reports = initialReports.toMutableList()

    override fun getReports(): List<Report> = reports.toList()

    override fun save(report: Report) {
        reports.add(0, report)
    }
}
