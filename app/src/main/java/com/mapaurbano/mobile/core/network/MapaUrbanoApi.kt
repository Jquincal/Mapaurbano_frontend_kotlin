package com.mapaurbano.mobile.core.network

import com.mapaurbano.mobile.BuildConfig
import com.mapaurbano.mobile.core.data.SessionStore
import com.mapaurbano.mobile.core.model.Report
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
import io.ktor.http.HttpHeaders
import io.ktor.http.Headers
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
    val details: List<ApiErrorDetailDto> = emptyList(),
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
        return client.get(url("users/me")) { bearerAuth(token) }.decodeAuthorized(token)
    }

    suspend fun getCurrentUserReports(cursor: String? = null, limit: Int = 50): CursorPageDto<ReportSummaryDto> {
        val token = requireToken()
        return client.get(url("users/me/reports")) {
            bearerAuth(token)
            parameter("limit", limit.coerceIn(1, 100))
            cursor?.let { parameter("cursor", it) }
        }.decodeAuthorized(token)
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

    suspend fun getCurrentUserReport(id: String): ReportDetailDto {
        val token = requireToken()
        return client.get(url("users/me/reports/$id")) { bearerAuth(token) }.decodeAuthorized(token)
    }

    suspend fun getCurrentUserReportImage(id: String): ByteArray {
        val token = requireToken()
        return client.get(url("users/me/reports/$id/image")) { bearerAuth(token) }.decodeAuthorized(token)
    }

    suspend fun clearLocalSession() = sessionStore.clearToken()

    suspend fun createReport(report: Report): CreateReportResponseDto {
        val token = requireToken()
        val response = client.post(url("reports")) {
            bearerAuth(token)
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("title", report.title)
                        append("description", report.description)
                        append("categorySlug", report.category.slug)
                        append("latitude", report.point.latitude.toString())
                        append("longitude", report.point.longitude.toString())
                        append("submissionMode", "ACCOUNT")
                        report.photo?.let { photo ->
                            require(photo.bytes.size <= 5 * 1024 * 1024) { "La foto supera 5 MB." }
                            val extension = when (photo.mimeType) { "image/jpeg" -> "jpg"; "image/png" -> "png"; "image/webp" -> "webp"; else -> error("Formato de foto inválido") }
                            append("image", photo.bytes, Headers.build {
                                append(HttpHeaders.ContentType, photo.mimeType)
                                append(HttpHeaders.ContentDisposition, "filename=\"evidencia.$extension\"")
                            })
                        }
                    },
                ),
            )
        }
        return response.decodeAuthorized(token)
    }

    private suspend fun requireToken(): String = sessionStore.readToken()
        ?: throw ApiException(
            statusCode = HttpStatusCode.Unauthorized.value,
            message = "Iniciá sesión para continuar.",
            code = "MISSING_SESSION",
        )

    private suspend inline fun <reified T> HttpResponse.decodeAuthorized(token: String): T = try {
        decodeOrThrow()
    } catch (error: ApiException) {
        if (error.statusCode == HttpStatusCode.Unauthorized.value && sessionStore.readToken() == token) {
            sessionStore.clearToken()
        }
        throw error
    }

    private fun url(path: String): String = "${baseUrl.trimEnd('/')}/${path.trimStart('/')}"


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
        details = apiError?.details.orEmpty(),
    )
}

private suspend fun HttpResponse.ensureSuccessOrThrow() {
    if (status.isSuccess()) return

    val apiError = runCatching { body<ApiErrorEnvelopeDto>() }.getOrNull()?.error
    throw ApiException(
        statusCode = status.value,
        message = apiError?.message ?: defaultMessage(status),
        code = apiError?.code,
        details = apiError?.details.orEmpty(),
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
