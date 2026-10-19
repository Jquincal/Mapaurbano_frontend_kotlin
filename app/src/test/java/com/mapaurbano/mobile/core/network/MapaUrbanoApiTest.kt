package com.mapaurbano.mobile.core.network

import com.mapaurbano.mobile.core.data.SessionStore
import com.mapaurbano.mobile.core.data.DemoContent
import com.mapaurbano.mobile.core.model.PhotoAttachment
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class MapaUrbanoApiTest {
    private class MemorySession(var token: String? = "private-token") : SessionStore {
        override suspend fun readToken() = token
        override suspend fun saveToken(token: String) { this.token = token }
        override suspend fun clearToken() { token = null }
    }

    @Test
    fun privateRoutesSendBearerAndOpaqueCursor() = runTest {
        val paths = mutableListOf<String>()
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Get, request.method)
            assertEquals("Bearer private-token", request.headers[HttpHeaders.Authorization])
            paths += request.url.encodedPath
            when (request.url.encodedPath) {
                "/api/v1/users/me/reports" -> {
                    assertEquals("opaque /+=&", request.url.parameters["cursor"])
                    assertEquals("100", request.url.parameters["limit"])
                    respond("""{"items":[],"nextCursor":null}""", headers = headersOf(HttpHeaders.ContentType, "application/json"))
                }
                "/api/v1/users/me/reports/own" -> respond("""{"id":"own","title":"Bache","description":"Detalle real","categorySlug":"bache","categoryName":"Calzada","latitude":-32.8,"longitude":-68.8,"status":"PENDING","hasImage":true,"createdAt":"2026-10-18T00:00:00Z","updatedAt":"2026-10-18T00:00:00Z","statusHistory":[{"toStatus":"PENDING","changedAt":"2026-10-18T00:00:00Z"}]}""", headers = headersOf(HttpHeaders.ContentType,"application/json"))
                "/api/v1/users/me/reports/own/image" -> respond(byteArrayOf(1,2,3), headers = headersOf(HttpHeaders.ContentType,"image/png"))
                else -> error("Ruta inesperada ${request.url}")
            }
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json() } }
        try {
            val api = MapaUrbanoApi(MemorySession(), "http://local/api/v1/", client)
            assertEquals(null, api.getCurrentUserReports("opaque /+=&", 500).nextCursor)
            assertEquals("PENDING", api.getCurrentUserReport("own").statusHistory.single().toStatus)
            assertArrayEquals(byteArrayOf(1,2,3), api.getCurrentUserReportImage("own"))
            assertEquals(3, paths.size)
        } finally { client.close() }
    }

    @Test
    fun multipartSendsRealImageAndAccountFields() = runTest {
        val bytes = byteArrayOf(1, 2, 3, 4)
        val engine = MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/api/v1/reports", request.url.encodedPath)
            assertEquals("Bearer private-token", request.headers[HttpHeaders.Authorization])
            val channel = ByteChannel(autoFlush = true)
            (request.body as OutgoingContent.WriteChannelContent).writeTo(channel)
            channel.close()
            val multipart = channel.readRemaining().readText()
            assertTrue(multipart.contains("name=\"image\""))
            assertTrue(multipart.contains("image/png"))
            assertTrue(multipart.contains("evidencia.png"))
            assertTrue(multipart.contains("ACCOUNT"))
            assertTrue(multipart.contains("categorySlug"))
            assertTrue(multipart.contains(bytes.decodeToString()))
            respond("""{"id":"created","status":"PENDING","createdAt":"2026-10-18T00:00:00Z"}""", HttpStatusCode.Created, headersOf(HttpHeaders.ContentType,"application/json"))
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json() } }
        try {
            val api = MapaUrbanoApi(MemorySession(), "http://local/api/v1/", client)
            val report = DemoContent.reports.first().copy(photo = PhotoAttachment("content://test/1", "image/png", bytes))
            assertEquals("created", api.createReport(report).id)
        } finally { client.close() }
    }

    @Test
    fun unauthorizedResponseClearsTokenAndPreservesValidationDetails() = runTest {
        val store = MemorySession()
        val engine = MockEngine {
            respond("""{"error":{"code":"INVALID_SESSION","message":"Venció","requestId":"test","details":[]}}""", HttpStatusCode.Unauthorized, headersOf(HttpHeaders.ContentType,"application/json"))
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json() } }
        try {
            val api = MapaUrbanoApi(store, "http://local/api/v1/", client)
            try { api.getCurrentUserReports(); fail("Debía rechazar la sesión") }
            catch (error: ApiException) { assertEquals(401, error.statusCode); assertEquals("INVALID_SESSION", error.code) }
            assertEquals(null, store.token)
        } finally { client.close() }
    }
    @Test
    fun logoutDeletesSavedTokenBeforeTheNetworkRequest() = runTest {
        val store = MemorySession()
        val engine = MockEngine { request ->
            assertEquals(null, store.token)
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("/api/v1/users/logout", request.url.encodedPath)
            assertEquals("Bearer private-token", request.headers[HttpHeaders.Authorization])
            respond("", HttpStatusCode.InternalServerError)
        }
        val client = HttpClient(engine)
        try {
            val api = MapaUrbanoApi(store, "http://local/api/v1/", client)
            try { api.logout(); fail("Debe informar el fallo del servidor") }
            catch (error: ApiException) { assertEquals(500, error.statusCode) }
            assertEquals(null, store.token)
        } finally { client.close() }
    }

}
