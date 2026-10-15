package com.mapaurbano.mobile.core.network

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestDto(
    val dni: String,
    val password: String,
)

@Serializable
data class LoginResponseDto(
    val token: String,
    val userId: String,
)

@Serializable
data class RegisterRequestDto(
    val dni: String,
    val displayName: String,
    val password: String,
)

@Serializable
data class RegisterResponseDto(
    val id: String,
    val token: String,
)

@Serializable
data class UserProfileDto(
    val id: String,
    val displayName: String,
    val dni: String?,
    val createdAt: String,
)

@Serializable
data class CursorPageDto<T>(
    val items: List<T>,
    val nextCursor: String? = null,
    val totalCount: Long? = null,
)

@Serializable
data class CategoryDto(
    val id: String,
    val slug: String,
    val name: String,
    val colorHex: String,
    val isActive: Boolean,
    val sortOrder: Int,
)

@Serializable
data class ReportSummaryDto(
    val id: String,
    val title: String,
    val categorySlug: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val hasImage: Boolean,
    val createdAt: String,
    val updatedAt: String,
)

@Serializable
data class ReportDetailDto(
    val id: String,
    val title: String,
    val description: String,
    val categorySlug: String,
    val categoryName: String,
    val latitude: Double,
    val longitude: Double,
    val status: String,
    val hasImage: Boolean,
    val createdAt: String,
    val updatedAt: String,
    val statusHistory: List<StatusHistoryDto> = emptyList(),
)

@Serializable
data class StatusHistoryDto(
    val fromStatus: String? = null,
    val toStatus: String,
    val changedAt: String,
)

@Serializable
data class CreateReportResponseDto(
    val id: String,
    val status: String,
    val createdAt: String,
)

@Serializable
data class ApiErrorEnvelopeDto(val error: ApiErrorDto)

@Serializable
data class ApiErrorDto(
    val code: String,
    val message: String,
    val details: List<ApiErrorDetailDto> = emptyList(),
    val requestId: String,
)

@Serializable
data class ApiErrorDetailDto(
    val field: String? = null,
    val reason: String,
)
