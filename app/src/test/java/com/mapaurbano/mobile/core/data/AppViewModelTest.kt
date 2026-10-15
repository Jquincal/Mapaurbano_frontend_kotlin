package com.mapaurbano.mobile.core.data

import com.mapaurbano.mobile.core.model.User
import com.mapaurbano.mobile.core.model.Report
import com.mapaurbano.mobile.core.network.ApiException
import com.mapaurbano.mobile.core.navigation.AppDestination
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private lateinit var authRepository: FakeAuthRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        authRepository = FakeAuthRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun unauthenticatedUserStartsOnLoginAndCannotOpenTheMap() = runTest(dispatcher) {
        val viewModel = AppViewModel(FakeReportRepository(), authRepository)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.destination is AppDestination.Login)

        viewModel.navigate(AppDestination.Map)

        assertTrue(viewModel.uiState.value.destination is AppDestination.Login)
    }

    @Test
    fun loginUnlocksTheMap() = runTest(dispatcher) {
        val viewModel = AppViewModel(FakeReportRepository(), authRepository)
        advanceUntilIdle()

        viewModel.login("30123456", "password123")
        advanceUntilIdle()

        assertEquals(AppDestination.Map, viewModel.uiState.value.destination)
        assertEquals("user-1", viewModel.uiState.value.user?.id)
    }

    @Test
    fun signedInSubmissionCreatesAnOwnedReport() = runTest(dispatcher) {
        val viewModel = AppViewModel(FakeReportRepository(), authRepository)
        viewModel.login("30123456", "password123")
        advanceUntilIdle()
        val category = viewModel.uiState.value.categories.first()
        viewModel.navigate(AppDestination.CreateReport)

        viewModel.updateDraft {
            it.copy(
                title = "Bache peligroso",
                description = "Ocupa media calzada",
                categoryId = category.id,
            )
        }
        viewModel.submitReport()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.destination is AppDestination.Confirmation)
        assertEquals("user-1", state.report(state.createdReportId)?.ownerId)
    }

    @Test
    fun logoutReturnsToLoginAndClearsTheDraft() = runTest(dispatcher) {
        val viewModel = AppViewModel(FakeReportRepository(), authRepository)
        viewModel.login("30123456", "password123")
        advanceUntilIdle()
        viewModel.updateDraft { it.copy(title = "Luminaria apagada") }

        viewModel.logout()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.destination is AppDestination.Login)
        assertEquals("", viewModel.uiState.value.draft.title)
    }

    @Test
    fun sessionExpiryDuringAccountSubmissionDoesNotCreateOrphanReport() = runTest(dispatcher) {
        val viewModel = AppViewModel(FakeReportRepository(), authRepository)
        viewModel.login("30123456", "password123")
        advanceUntilIdle()
        val initialCount = viewModel.uiState.value.reports.size
        val category = viewModel.uiState.value.categories.first()
        viewModel.navigate(AppDestination.CreateReport)
        viewModel.updateDraft {
            it.copy(title = "Semáforo intermitente", categoryId = category.id)
        }

        viewModel.submitReport()
        viewModel.simulateSessionExpiry()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(state.destination is AppDestination.Login)
        assertEquals(initialCount, state.reports.size)
        assertEquals("Semáforo intermitente", state.draft.title)
    }

    @Test
    fun repeatedSubmitWhilePendingCreatesOnlyOneReport() = runTest(dispatcher) {
        val viewModel = AppViewModel(FakeReportRepository(), authRepository)
        viewModel.login("30123456", "password123")
        advanceUntilIdle()
        val initialCount = viewModel.uiState.value.reports.size
        val category = viewModel.uiState.value.categories.first()
        viewModel.navigate(AppDestination.CreateReport)
        viewModel.updateDraft {
            it.copy(title = "Árbol caído", categoryId = category.id)
        }

        viewModel.submitReport()
        viewModel.submitReport()
        advanceUntilIdle()

        assertEquals(initialCount + 1, viewModel.uiState.value.reports.size)
    }

    @Test
    fun serverFailureExplainsTheErrorAndKeepsTheDraft() = runTest(dispatcher) {
        val baseRepository = FakeReportRepository()
        val failingRepository = object : ReportRepository by baseRepository {
            override suspend fun createReport(report: Report): Report {
                throw ApiException(500, "Fallo de persistencia")
            }
        }
        val viewModel = AppViewModel(failingRepository, authRepository)
        viewModel.login("30123456", "password123")
        advanceUntilIdle()
        val category = viewModel.uiState.value.categories.first()
        viewModel.navigate(AppDestination.CreateReport)
        viewModel.updateDraft {
            it.copy(
                title = "Bache que debe conservarse",
                description = "Detalle del reporte",
                categoryId = category.id,
            )
        }

        viewModel.submitReport()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Bache que debe conservarse", state.draft.title)
        assertEquals(
            "El servidor tuvo un problema interno. Intentá nuevamente más tarde.",
            state.formError,
        )
        assertTrue(state.destination is AppDestination.CreateReport)
    }

    private class FakeAuthRepository : AuthRepository {
        private var user: User? = null

        override suspend fun restoreSession(): User? = user

        override suspend fun login(dni: String, password: String): User =
            User(id = "user-1", displayName = "Vecina", dni = dni.filter(Char::isDigit)).also { user = it }

        override suspend fun register(displayName: String, dni: String, password: String): User =
            User(id = "user-1", displayName = displayName, dni = dni.filter(Char::isDigit)).also { user = it }

        override suspend fun logout() {
            user = null
        }

        override suspend fun deactivateAccount() {
            user = null
        }
    }
}
