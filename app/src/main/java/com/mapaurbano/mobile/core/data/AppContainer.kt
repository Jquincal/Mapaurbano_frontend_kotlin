package com.mapaurbano.mobile.core.data

import android.content.Context
import com.mapaurbano.mobile.core.network.MapaUrbanoApi

object AppContainer {
    lateinit var reportRepository: ReportRepository
        private set
    lateinit var authRepository: AuthRepository
        private set

    fun initialize(context: Context) {
        if (::reportRepository.isInitialized) return

        val api = MapaUrbanoApi(
            sessionStore = DataStoreSessionStore(context.applicationContext),
        )
        reportRepository = RemoteReportRepository(api)
        authRepository = RemoteAuthRepository(api)
    }
}
