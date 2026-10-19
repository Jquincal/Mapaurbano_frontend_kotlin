package com.mapaurbano.mobile.core.model

enum class ReportStatus(val label: String) {
    Pending("Pendiente"),
    InProgress("En proceso"),
    Resolved("Resuelto"),
}

data class Category(
    val id: String,
    val label: String,
    val icon: String,
    val slug: String = id,
)

data class MapPoint(
    val x: Float,
    val y: Float,
    val latitude: Double,
    val longitude: Double,
    val label: String,
)

data class User(
    val id: String,
    val displayName: String,
    val dni: String,
)

data class Report(
    val id: String,
    val title: String,
    val description: String,
    val category: Category,
    val status: ReportStatus,
    val location: String,
    val point: MapPoint,
    val createdAtLabel: String,
    val ownerId: String? = null,
    val hasPhoto: Boolean = false,
    val history: List<StatusEvent> = emptyList(),
    val photo: PhotoAttachment? = null,
    val imageBytes: ByteArray? = null,
)

data class StatusEvent(
    val status: ReportStatus,
    val title: String,
    val description: String,
    val dateLabel: String,
)

data class ReportDraft(
    val title: String = "",
    val description: String = "",
    val categoryId: String? = null,
    val location: String = "Av. San Martín 1420",
    val point: MapPoint = DefaultDraftPoint,
    val hasPhoto: Boolean = false,
    val photoIssue: PhotoIssue? = null,
    val photo: PhotoAttachment? = null,
) {
    val isDirty: Boolean
        get() = title.isNotBlank() ||
            description.isNotBlank() ||
            categoryId != null ||
            hasPhoto ||
            photoIssue != null ||
            location != "Av. San Martín 1420" ||
            point != DefaultDraftPoint

    val canSubmit: Boolean
        get() = title.trim().length in 5..150 && categoryId != null && location.isNotBlank() && photoIssue == null && point.latitude.isFinite() && point.longitude.isFinite() && point.latitude in -90.0..90.0 && point.longitude in -180.0..180.0
}

val DefaultDraftPoint = MapPoint(
    x = 0.50f,
    y = 0.52f,
    latitude = -32.8895,
    longitude = -68.8458,
    label = "Av. San Martín 1420",
)

data class ReportFilters(
    val query: String = "",
    val status: ReportStatus? = null,
    val categoryId: String? = null,
)

enum class MapPresentation { Map, List }

enum class ContentLoadState { Ready, Loading, Error }

enum class PhotoIssue(val message: String) {
    InvalidType("El archivo debe ser JPG, PNG o WebP."),
    InvalidDimensions("La foto no debe superar 12000 píxeles por lado."),
    TooLarge("La foto supera el máximo de 5 MB."),
    PermissionDenied("No pudimos acceder a la foto. Volvé a elegirla o usá la cámara."),
}

/** Adjunto temporal: nunca se persiste ni se comparte entre sesiones. */
data class PhotoAttachment(val uri: String, val mimeType: String, val bytes: ByteArray)
data class ReportPage(val items: List<Report>, val nextCursor: String?)
