package com.mapaurbano.mobile.core.data

import com.mapaurbano.mobile.core.network.ApiException
import java.io.IOException

internal fun Throwable.toUserFacingMessage(fallback: String): String {
    val apiError = this as? ApiException
    if (apiError != null) {
        return when (apiError.statusCode) {
            400 -> apiError.details.takeIf { it.isNotEmpty() }?.joinToString("\n") { listOfNotNull(it.field, it.reason).joinToString(": ") } ?: apiError.message.takeIf { it.isNotBlank() } ?: "El servidor rechazó los datos. Revisá los campos e intentá nuevamente."
            401 -> "La sesión no es válida o venció. Iniciá sesión nuevamente."
            403 -> "No tenés permiso para realizar esta acción."
            404 -> "No encontramos la información solicitada."
            409 -> "La operación entra en conflicto con información existente."
            429 -> "Se alcanzó el límite de solicitudes. Esperá unos minutos e intentá nuevamente."
            in 500..599 -> "El servidor tuvo un problema interno. Intentá nuevamente más tarde."
            else -> apiError.message.takeIf(String::isNotBlank) ?: fallback
        }
    }

    val isConnectionFailure = generateSequence(this as Throwable?) { it.cause }
        .any { it is IOException }
    return if (isConnectionFailure) {
        "No pudimos conectar con el servidor. Revisá la red y que el servidor esté activo."
    } else {
        fallback
    }
}
