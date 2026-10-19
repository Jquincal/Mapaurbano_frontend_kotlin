package com.mapaurbano.mobile.core.data

import com.mapaurbano.mobile.core.model.User
import com.mapaurbano.mobile.core.network.MapaUrbanoApi
import com.mapaurbano.mobile.core.network.UserProfileDto

interface AuthRepository {
    suspend fun restoreSession(): User?
    suspend fun login(dni: String, password: String): User
    suspend fun register(displayName: String, dni: String, password: String): User
    suspend fun clearSession() = logout()
    suspend fun logout()
    suspend fun deactivateAccount()
}

class RemoteAuthRepository(
    private val api: MapaUrbanoApi,
) : AuthRepository {
    override suspend fun restoreSession(): User? = api.restoreCurrentUser()?.toModel()

    override suspend fun login(dni: String, password: String): User {
        api.login(dni, password)
        return api.getCurrentUser().toModel()
    }

    override suspend fun register(displayName: String, dni: String, password: String): User {
        api.register(displayName, dni, password)
        return api.getCurrentUser().toModel()
    }

    override suspend fun clearSession() = api.clearLocalSession()

    override suspend fun logout() = api.logout()

    override suspend fun deactivateAccount() = api.deactivateAccount()
}

private fun UserProfileDto.toModel(): User = User(
    id = id,
    displayName = displayName,
    dni = dni.orEmpty(),
)
