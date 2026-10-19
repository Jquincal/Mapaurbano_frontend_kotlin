package com.mapaurbano.mobile.core.data

import com.mapaurbano.mobile.core.model.Category
import com.mapaurbano.mobile.core.model.Report
import com.mapaurbano.mobile.core.model.ReportPage
import com.mapaurbano.mobile.core.network.ApiException

interface ReportRepository {
    val initialCategories: List<Category>
    val initialReports: List<Report>
    suspend fun getCategories(): List<Category>
    suspend fun getCurrentUserReports(userId: String, categories: List<Category>, cursor: String? = null): ReportPage
    suspend fun getCurrentUserReport(id: String, userId: String, categories: List<Category>): Report
    suspend fun getCurrentUserReportImage(id: String): ByteArray
    suspend fun createReport(report: Report): Report
}

/** Solo para pruebas y previews, nunca se inyecta en AppContainer. */
class FakeReportRepository(reports: List<Report> = DemoContent.reports, categories: List<Category> = DemoContent.categories) : ReportRepository {
    private val storedReports = reports.toMutableList()
    override val initialCategories = categories
    override val initialReports = reports
    override suspend fun getCategories() = initialCategories
    override suspend fun getCurrentUserReports(userId: String, categories: List<Category>, cursor: String?): ReportPage {
        val own = storedReports.filter { it.ownerId == userId }
        val offset = cursor?.toIntOrNull() ?: 0
        return ReportPage(own.drop(offset).take(50), (offset + 50).takeIf { it < own.size }?.toString())
    }
    override suspend fun getCurrentUserReport(id: String, userId: String, categories: List<Category>) =
        storedReports.firstOrNull { it.id == id && it.ownerId == userId } ?: throw ApiException(404, "Reporte no encontrado")
    override suspend fun getCurrentUserReportImage(id: String) = storedReports.first { it.id == id }.imageBytes ?: byteArrayOf()
    override suspend fun createReport(report: Report): Report = report.also { storedReports.add(0, it) }
}
