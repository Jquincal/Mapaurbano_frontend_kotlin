package com.mapaurbano.mobile.core.network

import com.mapaurbano.mobile.BuildConfig
import com.mapaurbano.mobile.core.data.SessionStore
import com.mapaurbano.mobile.core.model.Report
import com.mapaurbano.mobile.core.model.SubmissionMode
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

class ApiException(
    val statusCode: Int,
    override val message: String,
    val code: String? = null,
) : Exception(message)

class MapaUrbanoApi(
    private val sessionStore: SessionStore,
    private val baseUrl: String = BuildConfig.API_BASE_URL,
    private val client: HttpClient = defaultHttpClient(),
) {
    suspend fun login(dni: String, password: String): LoginResponseDto =
        client.post(url("users/login")) {
            contentType(ContentType.Application.Json)
            setBody(LoginRequestDto(dni = dni, password = password))
        }.decodeOrThrow<LoginResponseDto>().also { sessionStore.saveToken(it.token) }

    suspend fun register(displayName: String, dni: String, password: String): RegisterResponseDto =
        client.post(url("users/register")) {
            contentType(ContentType.Application.Json)
            setBody(RegisterRequestDto(dni = dni, displayName = displayName, password = password))
        }.decodeOrThrow<RegisterResponseDto>().also { sessionStore.saveToken(it.token) }

    suspend fun restoreCurrentUser(): UserProfileDto? {
        if (sessionStore.readToken() == null) return null
        return try {
            getCurrentUser()
        } catch (error: ApiException) {
            if (error.statusCode == HttpStatusCode.Unauthorized.value) {
                sessionStore.clearToken()
                null
            } else {
                throw error
            }
        }
    }

    suspend fun getCurrentUser(): UserProfileDto {
        val token = requireToken()
        return client.get(url("users/me")) { bearerAuth(token) }.decodeAuthorized()
    }

    suspend fun getCurrentUserReports(): CursorPageDto<ReportSummaryDto> {
        val token = requireToken()
        return client.get(url("users/me/reports")) {
            bearerAuth(token)
            parameter("limit", 50)
        }.decodeAuthorized()
    }

    suspend fun logout() {
        try {
            val token = requireToken()
            client.post(url("users/logout")) { bearerAuth(token) }.ensureSuccessOrThrow()
        } finally {
            sessionStore.clearToken()
        }
    }

    suspend fun deactivateAccount() {
        try {
            val token = requireToken()
            client.delete(url("users/me")) { bearerAuth(token) }.ensureSuccessOrThrow()
            sessionStore.clearToken()
        } catch (error: ApiException) {
            if (error.statusCode == HttpStatusCode.Unauthorized.value) {
                sessionStore.clearToken()
            }
            throw error
        }
    }

    suspend fun getCategories(): List<CategoryDto> =
        client.get(url("categories")).decodeOrThrow()

    suspend fun getPublicReports(): CursorPageDto<ReportSummaryDto> =
        client.get(url("reports")) {
            parameter("minLat", MENDOZA_MIN_LATITUDE)
            parameter("minLng", MENDOZA_MIN_LONGITUDE)
            parameter("maxLat", MENDOZA_MAX_LATITUDE)
            parameter("maxLng", MENDOZA_MAX_LONGITUDE)
        }.decodeOrThrow()

    suspend fun getPublicReport(id: String): ReportDetailDto =
        client.get(url("reports/$id")).decodeOrThrow()

    suspend fun createReport(report: Report): CreateReportResponseDto {
        val token = if (report.submissionMode == SubmissionMode.Account) requireToken() else null
        val response = client.post(url("reports")) {
            if (token != null) bearerAuth(token)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("title", report.title)
                        append("description", report.description)
                        append("categorySlug", report.category.slug)
                        append("latitude", report.point.latitude.toString())
                        append("longitude", report.point.longitude.toString())
                        append("submissionMode", report.submissionMode.name.uppercase())
                    },
                ),
            )
        }
        return if (report.submissionMode == SubmissionMode.Account) {
            response.decodeAuthorized()
        } else {
            response.decodeOrThrow()
        }
    }

    suspend fun getReportByTrackingCode(code: String): TrackingCodeResponseDto =
        client.post(url("report-status")) {
            contentType(ContentType.Application.Json)
            setBody(TrackingCodeRequestDto(code))
        }.decodeOrThrow()

    private suspend fun requireToken(): String = sessionStore.readToken()
        ?: throw ApiException(
            statusCode = HttpStatusCode.Unauthorized.value,
            message = "Iniciá sesión para continuar.",
            code = "MISSING_SESSION",
        )

    private suspend inline fun <reified T> HttpResponse.decodeAuthorized(): T = try {
        decodeOrThrow()
    } catch (error: ApiException) {
        if (error.statusCode == HttpStatusCode.Unauthorized.value) {
            sessionStore.clearToken()
        }
        throw error
    }

    private fun url(path: String): String = "${baseUrl.trimEnd('/')}/${path.trimStart('/')}"

    private companion object {
        const val MENDOZA_MIN_LATITUDE = -32.96
        const val MENDOZA_MAX_LATITUDE = -32.82
        const val MENDOZA_MIN_LONGITUDE = -68.93
        const val MENDOZA_MAX_LONGITUDE = -68.76
    }
}

private fun defaultHttpClient(): HttpClient = HttpClient(OkHttp) {
    expectSuccess = false
    install(ContentNegotiation) {
        json(
            Json {
                ignoreUnknownKeys = true
                explicitNulls = false
            },
        )
    }
}

private suspend inline fun <reified T> HttpResponse.decodeOrThrow(): T {
    if (status.isSuccess()) return body()

    val apiError = runCatching { body<ApiErrorEnvelopeDto>() }.getOrNull()?.error
    throw ApiException(
        statusCode = status.value,
        message = apiError?.message ?: defaultMessage(status),
        code = apiError?.code,
    )
}

private suspend fun HttpResponse.ensureSuccessOrThrow() {
    if (status.isSuccess()) return

    val apiError = runCatching { body<ApiErrorEnvelopeDto>() }.getOrNull()?.error
    throw ApiException(
        statusCode = status.value,
        message = apiError?.message ?: defaultMessage(status),
        code = apiError?.code,
    )
}

private fun defaultMessage(status: HttpStatusCode): String = when (status.value) {
    400 -> "El servidor rechazó los datos enviados."
    401 -> "La sesión no es válida o venció."
    404 -> "No encontramos la información solicitada."
    409 -> "La operación entra en conflicto con datos existentes."
    429 -> "Hay demasiadas solicitudes. Intentá nuevamente en unos minutos."
    else -> "El servidor no pudo completar la solicitud."
}
