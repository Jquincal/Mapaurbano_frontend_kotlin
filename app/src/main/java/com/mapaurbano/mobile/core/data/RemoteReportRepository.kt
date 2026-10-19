package com.mapaurbano.mobile.core.data

import com.mapaurbano.mobile.core.model.Category
import com.mapaurbano.mobile.core.model.MapPoint
import com.mapaurbano.mobile.core.model.Report
import com.mapaurbano.mobile.core.model.ReportPage
import com.mapaurbano.mobile.core.model.ReportStatus
import com.mapaurbano.mobile.core.model.StatusEvent
import com.mapaurbano.mobile.core.network.CategoryDto
import com.mapaurbano.mobile.core.network.MapaUrbanoApi
import com.mapaurbano.mobile.core.network.ReportDetailDto
import com.mapaurbano.mobile.core.network.ReportSummaryDto
import com.mapaurbano.mobile.core.network.StatusHistoryDto
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeParseException
import java.util.Locale

class RemoteReportRepository(
    private val api: MapaUrbanoApi,
) : ReportRepository {
    override val initialCategories: List<Category> = emptyList()
    override val initialReports: List<Report> = emptyList()

    override suspend fun getCategories(): List<Category> = api.getCategories()
        .filter(CategoryDto::isActive)
        .sortedBy(CategoryDto::sortOrder)
        .map(CategoryDto::toModel)

    override suspend fun getCurrentUserReports(userId: String, categories: List<Category>, cursor: String?): ReportPage {
        val page = api.getCurrentUserReports(cursor)
        return ReportPage(page.items.filter { validCoordinates(it.latitude, it.longitude) }.map { it.toModel(categories).copy(ownerId = userId) }, page.nextCursor)
    }

    override suspend fun getCurrentUserReport(id: String, userId: String, categories: List<Category>): Report {
        val detail = api.getCurrentUserReport(id)
        require(validCoordinates(detail.latitude, detail.longitude)) { "El reporte tiene coordenadas inválidas." }
        return detail.toModel(categories).copy(ownerId = userId)
    }

    override suspend fun getCurrentUserReportImage(id: String) = api.getCurrentUserReportImage(id)

    override suspend fun createReport(report: Report): Report {
        val response = api.createReport(report)
        return report.copy(
            id = response.id,
            status = response.status.toReportStatus(),
            createdAtLabel = response.createdAt.toRelativeLabel(),
            photo = null,
        )
    }
}

private fun CategoryDto.toModel(): Category = Category(
    id = id,
    slug = slug,
    label = name,
    icon = slug.toCategoryIcon(),
)

private fun ReportSummaryDto.toModel(categories: List<Category>): Report {
    val category = categories.findCategory(categorySlug)
    return Report(
        id = id,
        title = title,
        description = "Abrí el reporte para consultar la descripción completa.",
        category = category,
        status = status.toReportStatus(),
        location = coordinateLabel(latitude, longitude),
        point = mapPoint(latitude, longitude),
        createdAtLabel = createdAt.toRelativeLabel(),
        hasPhoto = hasImage,
    )
}

private fun ReportDetailDto.toModel(categories: List<Category>): Report = Report(
    id = id,
    title = title,
    description = description,
    category = categories.findCategory(categorySlug, categoryName),
    status = status.toReportStatus(),
    location = coordinateLabel(latitude, longitude),
    point = mapPoint(latitude, longitude),
    createdAtLabel = createdAt.toRelativeLabel(),
    hasPhoto = hasImage,
    history = statusHistory.map(StatusHistoryDto::toModel),
)

private fun StatusHistoryDto.toModel(): StatusEvent {
    val status = toStatus.toReportStatus()
    return StatusEvent(
        status = status,
        title = status.label,
        description = if (fromStatus == null) {
            "Reporte recibido por el municipio."
        } else {
            "Estado actualizado de ${fromStatus.toReportStatus().label.lowercase()} a ${status.label.lowercase()}."
        },
        dateLabel = changedAt.toRelativeLabel(),
    )
}

private fun List<Category>.findCategory(slug: String, name: String? = null): Category =
    firstOrNull { it.slug == slug }
        ?: Category(id = slug, slug = slug, label = name ?: slug.replace('-', ' ').replaceFirstChar(Char::uppercase), icon = slug.toCategoryIcon())

private fun String.toReportStatus(): ReportStatus = when (uppercase(Locale.ROOT)) {
    "IN_PROGRESS" -> ReportStatus.InProgress
    "RESOLVED" -> ReportStatus.Resolved
    else -> ReportStatus.Pending
}

private fun String.toCategoryIcon(): String = when {
    contains("road") || contains("bache") || contains("calzada") -> "minor_crash"
    contains("light") || contains("alumbr") -> "light"
    contains("waste") || contains("basura") || contains("higiene") -> "delete"
    contains("water") || contains("agua") || contains("desag") -> "water_drop"
    contains("park") || contains("espacio") -> "park"
    contains("sign") || contains("señal") -> "traffic"
    contains("obstruction") || contains("obstru") -> "block"
    else -> "more_horiz"
}

private fun mapPoint(latitude: Double, longitude: Double): MapPoint {
    val x = ((longitude - MENDOZA_MIN_LONGITUDE) / (MENDOZA_MAX_LONGITUDE - MENDOZA_MIN_LONGITUDE))
        .toFloat()
        .coerceIn(0.06f, 0.94f)
    val y = (1.0 - (latitude - MENDOZA_MIN_LATITUDE) / (MENDOZA_MAX_LATITUDE - MENDOZA_MIN_LATITUDE))
        .toFloat()
        .coerceIn(0.08f, 0.92f)
    val label = coordinateLabel(latitude, longitude)
    return MapPoint(x = x, y = y, latitude = latitude, longitude = longitude, label = label)
}

private fun coordinateLabel(latitude: Double, longitude: Double): String =
    String.format(Locale.US, "%.5f, %.5f", latitude, longitude)

private fun String.toRelativeLabel(): String {
    val instant = try {
        Instant.parse(this)
    } catch (_: DateTimeParseException) {
        return this
    }
    val elapsed = Duration.between(instant, Instant.now()).coerceAtLeast(Duration.ZERO)
    return when {
        elapsed.toMinutes() < 1 -> "hace un momento"
        elapsed.toHours() < 1 -> "hace ${elapsed.toMinutes()} min"
        elapsed.toDays() < 1 -> "hace ${elapsed.toHours()} h"
        elapsed.toDays() == 1L -> "ayer"
        else -> "hace ${elapsed.toDays()} días"
    }
}

private const val MENDOZA_MIN_LATITUDE = -32.96
private const val MENDOZA_MAX_LATITUDE = -32.82
private const val MENDOZA_MIN_LONGITUDE = -68.93
private const val MENDOZA_MAX_LONGITUDE = -68.76

private fun validCoordinates(lat: Double, lon: Double) = lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0
