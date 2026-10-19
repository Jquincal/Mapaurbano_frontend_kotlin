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
import kotlinx.coroutines.test.runCurrent
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
        assertEquals(0, state.reports.size)
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

    @Test
    fun paginationUsesOpaqueCursorAndDeduplicates() = runTest(dispatcher) {
        val template = DemoContent.reports.first().copy(ownerId = "user-1")
        val cursors = mutableListOf<String?>()
        val repository = object : ReportRepository by FakeReportRepository() {
            override suspend fun getCurrentUserReports(userId: String, categories: List<com.mapaurbano.mobile.core.model.Category>, cursor: String?): com.mapaurbano.mobile.core.model.ReportPage {
                cursors += cursor
                return if (cursor == null) com.mapaurbano.mobile.core.model.ReportPage(listOf(template), "opaque/+== cursor")
                else com.mapaurbano.mobile.core.model.ReportPage(listOf(template, template.copy(id = "second")), null)
            }
        }
        val vm = AppViewModel(repository, authRepository)
        advanceUntilIdle()
        assertTrue(vm.uiState.value.reports.isEmpty())
        assertTrue(cursors.isEmpty())
        vm.login("30123456", "password123")
        advanceUntilIdle()
        vm.loadMoreReports()
        advanceUntilIdle()
        vm.loadMoreReports()
        advanceUntilIdle()
        assertEquals(listOf(null, "opaque/+== cursor"), cursors)
        assertEquals(2, vm.uiState.value.reports.size)
        assertEquals(null, vm.uiState.value.nextCursor)
    }

    @Test
    fun staleResponseCannotRepopulateLoggedOutSession() = runTest(dispatcher) {
        val response = kotlinx.coroutines.CompletableDeferred<com.mapaurbano.mobile.core.model.ReportPage>()
        val repository = object : ReportRepository by FakeReportRepository() {
            override suspend fun getCurrentUserReports(userId: String, categories: List<com.mapaurbano.mobile.core.model.Category>, cursor: String?) = response.await()
        }
        val vm = AppViewModel(repository, authRepository)
        advanceUntilIdle()
        vm.login("30123456", "password123")
        runCurrent()
        vm.setQuery("dato privado")
        vm.logout()
        runCurrent()
        response.complete(com.mapaurbano.mobile.core.model.ReportPage(listOf(DemoContent.reports.first().copy(ownerId = "user-1")), null))
        advanceUntilIdle()
        assertEquals(null, vm.uiState.value.user)
        assertTrue(vm.uiState.value.reports.isEmpty())
        assertEquals("", vm.uiState.value.mapFilters.query)
        assertTrue(vm.uiState.value.backStack.isEmpty())
    }

    @Test
    fun unauthorizedListDetailImageAndCreateClearAllPrivateState() = runTest(dispatcher) {
        for (operation in listOf("list", "detail", "image", "create")) {
            val own = DemoContent.reports.first().copy(id = "own", ownerId = "user-1", hasPhoto = true)
            val repository = object : ReportRepository by FakeReportRepository(listOf(own)) {
                override suspend fun getCurrentUserReports(userId: String, categories: List<com.mapaurbano.mobile.core.model.Category>, cursor: String?): com.mapaurbano.mobile.core.model.ReportPage {
                    if (operation == "list") throw ApiException(401, "Sesión vencida")
                    return com.mapaurbano.mobile.core.model.ReportPage(listOf(own), null)
                }
                override suspend fun getCurrentUserReport(id: String, userId: String, categories: List<com.mapaurbano.mobile.core.model.Category>): Report {
                    if (operation == "detail") throw ApiException(401, "Sesión vencida")
                    return own
                }
                override suspend fun getCurrentUserReportImage(id: String): ByteArray = throw ApiException(401, "Sesión vencida")
                override suspend fun createReport(report: Report): Report = throw ApiException(401, "Sesión vencida")
            }
            val vm = AppViewModel(repository, FakeAuthRepository())
            advanceUntilIdle()
            vm.login("30123456", "password123")
            advanceUntilIdle()
            if (operation in listOf("detail", "image")) vm.navigate(AppDestination.ReportDetail("own"))
            if (operation == "create") {
                vm.navigate(AppDestination.CreateReport)
                vm.updateDraft { it.copy(title = "Borrador privado", categoryId = vm.uiState.value.categories.first().id) }
                vm.submitReport()
            }
            advanceUntilIdle()
            assertEquals(operation, null, vm.uiState.value.user)
            assertTrue(operation, vm.uiState.value.reports.isEmpty())
            assertTrue(operation, vm.uiState.value.backStack.isEmpty())
            assertTrue(operation, vm.uiState.value.destination is AppDestination.Login)
            assertEquals(operation, if (operation == "create") "Borrador privado" else "", vm.uiState.value.draft.title)
        }
    }

    @Test
    fun invalidPhotoPreventsCreationAndPreservesFields() = runTest(dispatcher) {
        val vm = AppViewModel(FakeReportRepository(), authRepository)
        advanceUntilIdle()
        vm.login("30123456", "password123")
        advanceUntilIdle()
        vm.navigate(AppDestination.CreateReport)
        vm.updateDraft { it.copy(title = "Bache peligroso", categoryId = vm.uiState.value.categories.first().id) }
        vm.setPhoto(false, com.mapaurbano.mobile.core.model.PhotoIssue.TooLarge)
        vm.submitReport()
        advanceUntilIdle()
        assertEquals("Bache peligroso", vm.uiState.value.draft.title)
        assertTrue(vm.uiState.value.destination is AppDestination.CreateReport)
        assertEquals(null, vm.uiState.value.createdReportId)
    }

    @Test
    fun switchingAccountsNeverKeepsPreviousReportsOrFilters() = runTest(dispatcher) {
        val template = DemoContent.reports.first()
        val vm = AppViewModel(FakeReportRepository(listOf(template.copy(id = "A", ownerId = "user-1"), template.copy(id = "B", ownerId = "user-2"))), authRepository)
        advanceUntilIdle()
        vm.login("30123456", "password123")
        advanceUntilIdle()
        assertEquals(listOf("A"), vm.uiState.value.reports.map { it.id })
        vm.setQuery("privado de A")
        vm.selectReportOnMap(vm.uiState.value.reports.first())
        vm.logout()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.reports.isEmpty())
        assertEquals(null, vm.uiState.value.selectedReportId)
        vm.login("30234567", "password123")
        advanceUntilIdle()
        assertEquals(listOf("B"), vm.uiState.value.visibleReports.map { it.id })
        assertEquals("", vm.uiState.value.mapFilters.query)
    }

    @Test
    fun logoutErasesPrivateDataBeforeTheServerResponds() = runTest(dispatcher) {
        val pendingLogout = kotlinx.coroutines.CompletableDeferred<Unit>()
        val auth = object : AuthRepository by authRepository {
            override suspend fun logout() { pendingLogout.await() }
        }
        val own = DemoContent.reports.first().copy(ownerId = "user-1", imageBytes = byteArrayOf(1, 2))
        val vm = AppViewModel(FakeReportRepository(listOf(own)), auth)
        advanceUntilIdle()
        vm.login("30123456", "password123")
        advanceUntilIdle()
        vm.selectReportOnMap(vm.uiState.value.reports.first())
        vm.setQuery("privado")
        vm.logout()
        assertEquals(null, vm.uiState.value.user)
        assertTrue(vm.uiState.value.reports.isEmpty())
        assertEquals(null, vm.uiState.value.selectedReportId)
        assertEquals("", vm.uiState.value.mapFilters.query)
        assertTrue(vm.uiState.value.destination is AppDestination.Login)
        pendingLogout.complete(Unit)
        advanceUntilIdle()
        assertTrue(!vm.uiState.value.isAuthenticating)
    }

    @Test
    fun unresponsiveLogoutTimesOutAndClearsSavedSession() = runTest(dispatcher) {
        var cleared = false
        val auth = object : AuthRepository by authRepository {
            override suspend fun logout(): Unit = kotlinx.coroutines.awaitCancellation()
            override suspend fun clearSession() { cleared = true }
        }
        val vm = AppViewModel(FakeReportRepository(), auth)
        advanceUntilIdle()
        vm.login("30123456", "password123")
        advanceUntilIdle()
        vm.logout()
        advanceUntilIdle()
        assertTrue(cleared)
        assertTrue(vm.uiState.value.reports.isEmpty())
        assertTrue(!vm.uiState.value.isAuthenticating)
    }

    private class FakeAuthRepository : AuthRepository {
        private var user: User? = null

        override suspend fun restoreSession(): User? = user

        override suspend fun login(dni: String, password: String): User =
            User(id = if (dni == "30123456") "user-1" else "user-2", displayName = "Vecina", dni = dni.filter(Char::isDigit)).also { user = it }

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
