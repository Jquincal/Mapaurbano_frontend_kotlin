package com.mapaurbano.mobile.core.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.sessionDataStore by preferencesDataStore(name = "user_session")

interface SessionStore {
    suspend fun readToken(): String?
    suspend fun saveToken(token: String)
    suspend fun clearToken()
}

class DataStoreSessionStore(context: Context) : SessionStore {
    private val dataStore = context.applicationContext.sessionDataStore

    override suspend fun readToken(): String? = dataStore.data.first()[TOKEN_KEY]

    override suspend fun saveToken(token: String) {
        dataStore.edit { preferences -> preferences[TOKEN_KEY] = token }
    }

    override suspend fun clearToken() {
        dataStore.edit { preferences -> preferences.remove(TOKEN_KEY) }
    }

    private companion object {
        val TOKEN_KEY = stringPreferencesKey("bearer_token")
    }
}
