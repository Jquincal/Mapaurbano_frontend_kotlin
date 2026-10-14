package com.mapaurbano.mobile.core.data

import com.mapaurbano.mobile.core.model.Category
import com.mapaurbano.mobile.core.model.Report

interface ReportRepository {
    val initialCategories: List<Category>
    val initialReports: List<Report>

    suspend fun getCategories(): List<Category>

    suspend fun getPublicReports(categories: List<Category>): List<Report>

    suspend fun getPublicReport(id: String, categories: List<Category>): Report

    suspend fun getCurrentUserReports(userId: String, categories: List<Category>): List<Report>

    suspend fun createReport(report: Report): Report

    suspend fun getReportByTrackingCode(code: String, categories: List<Category>): Report
}

/** Repositorio determinista para pruebas y vistas previas sin conexión. */
class FakeReportRepository(
    reports: List<Report> = DemoContent.reports,
    categories: List<Category> = DemoContent.categories,
) : ReportRepository {
    private val storedReports = reports.toMutableList()

    override val initialCategories: List<Category> = categories
    override val initialReports: List<Report> = reports

    override suspend fun getCategories(): List<Category> = initialCategories

    override suspend fun getPublicReports(categories: List<Category>): List<Report> = storedReports.toList()

    override suspend fun getPublicReport(id: String, categories: List<Category>): Report =
        storedReports.first { it.id == id }

    override suspend fun getCurrentUserReports(userId: String, categories: List<Category>): List<Report> =
        storedReports.filter { it.ownerId == userId }

    override suspend fun createReport(report: Report): Report {
        storedReports.add(0, report)
        return report
    }

    override suspend fun getReportByTrackingCode(code: String, categories: List<Category>): Report {
        val normalized = code.filter(Char::isLetterOrDigit).uppercase()
        return storedReports.first {
            it.trackingCode?.filter(Char::isLetterOrDigit)?.uppercase() == normalized
        }
    }
}
