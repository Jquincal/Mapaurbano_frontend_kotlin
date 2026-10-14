package com.mapaurbano.mobile.core.data

import com.mapaurbano.mobile.core.network.ApiException
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

class UserFacingErrorsTest {
    @Test
    fun `network failure identifies an unreachable server`() {
        assertEquals(
            "No pudimos conectar con el servidor. Revisá la red y que el servidor esté activo.",
            IOException("Connection refused").toUserFacingMessage("Error"),
        )
    }

    @Test
    fun `http failures are explained by status`() {
        val expectedMessages = mapOf(
            400 to "El servidor rechazó los datos. Revisá los campos e intentá nuevamente.",
            401 to "La sesión no es válida o venció. Iniciá sesión nuevamente.",
            409 to "La operación entra en conflicto con información existente.",
            429 to "Se alcanzó el límite de solicitudes. Esperá unos minutos e intentá nuevamente.",
            500 to "El servidor tuvo un problema interno. Intentá nuevamente más tarde.",
        )

        expectedMessages.forEach { (status, message) ->
            assertEquals(
                message,
                ApiException(status, "Respuesta del servidor").toUserFacingMessage("Error"),
            )
        }
    }

    @Test
    fun `unknown failure uses operation fallback`() {
        assertEquals("No pudimos completar la operación.", IllegalStateException().toUserFacingMessage("No pudimos completar la operación."))
    }
}
