package com.mapaurbano.mobile.core.data

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapaurbano.mobile.core.model.Category
import com.mapaurbano.mobile.core.model.ContentLoadState
import com.mapaurbano.mobile.core.model.MapPoint
import com.mapaurbano.mobile.core.model.MapPresentation
import com.mapaurbano.mobile.core.model.PhotoIssue
import com.mapaurbano.mobile.core.model.Report
import com.mapaurbano.mobile.core.model.ReportDraft
import com.mapaurbano.mobile.core.model.ReportFilters
import com.mapaurbano.mobile.core.model.ReportStatus
import com.mapaurbano.mobile.core.model.StatusEvent
import com.mapaurbano.mobile.core.model.User
import com.mapaurbano.mobile.core.network.ApiException
import com.mapaurbano.mobile.core.navigation.AppDestination
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface AppDialog {
    data object Help : AppDialog
    data object Filters : AppDialog
    data class FutureFeature(val title: String) : AppDialog
    data class DiscardDraft(val target: AppDestination) : AppDialog
    data object DeactivateAccount : AppDialog
    data object PhotoPicker : AppDialog
}

data class AppUiState(
    val destination: AppDestination = AppDestination.Login(AppDestination.Map),
    val backStack: List<AppDestination> = emptyList(),
    val reports: List<Report> = emptyList(),
    val categories: List<Category> = emptyList(),
    val user: User? = null,
    val draft: ReportDraft = ReportDraft(),
    val mapFilters: ReportFilters = ReportFilters(),
    val myReportsFilters: ReportFilters = ReportFilters(),
    val mapPresentation: MapPresentation = MapPresentation.Map,
    val createdReportId: String? = null,
    val contentLoadState: ContentLoadState = ContentLoadState.Ready,
    val contentLoadError: String? = null,
    val myReportsLoadState: ContentLoadState = ContentLoadState.Ready,
    val myReportsLoadError: String? = null,
    val detailError: String? = null,
    val nextCursor: String? = null,
    val isLoadingMore: Boolean = false,
    val paginationError: String? = null,
    val isReadingPhoto: Boolean = false,
    val isSubmitting: Boolean = false,
    val isAuthenticating: Boolean = false,
    val isRestoringSession: Boolean = true,
    val failNextSubmission: Boolean = false,
    val formError: String? = null,
    val authError: String? = null,
    val selectedReportId: String? = null,
    val dialog: AppDialog? = null,
    val message: String? = null,
) {
    val visibleReports: List<Report>
        get() {
            val normalized = mapFilters.query.trim().lowercase()
            return ownedReports.filter { report ->
                (mapFilters.status == null || report.status == mapFilters.status) &&
                    (mapFilters.categoryId == null || report.category.id == mapFilters.categoryId) &&
                    (normalized.isBlank() || listOf(
                        report.title,
                        report.description,
                        report.location,
                        report.category.label,
                    ).any { normalized in it.lowercase() })
            }
        }

    val ownedReports: List<Report>
        get() = user?.let { current -> reports.filter { it.ownerId == current.id } }.orEmpty()

    fun report(id: String?): Report? = reports.firstOrNull { it.id == id }
}

class AppViewModel(
    private val reportRepository: ReportRepository = AppContainer.reportRepository,
    private val authRepository: AuthRepository = AppContainer.authRepository,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AppUiState(
            reports = emptyList(),
            categories = reportRepository.initialCategories,
            contentLoadState = if (reportRepository.initialCategories.isEmpty()) {
                ContentLoadState.Loading
            } else {
                ContentLoadState.Ready
            },
        ),
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    private var submissionGeneration = 0L
    private var sessionGeneration = 0L
    private var listGeneration = 0L
    private var detailGeneration = 0L
    private var photoGeneration = 0L
    private val seenCursors = mutableSetOf<String>()

    init {
        restoreSession()
    }

    fun navigate(destination: AppDestination, replace: Boolean = false) {
        val state = _uiState.value
        if (state.isSubmitting) {
            _uiState.update { it.copy(message = "Esperá a que termine el envío antes de salir.") }
            return
        }
        if (state.user == null &&
            destination !is AppDestination.Login &&
            destination !is AppDestination.Register
        ) {
            navigate(AppDestination.Login(destination))
            return
        }
        if (state.destination == AppDestination.CreateReport &&
            state.draft.isDirty &&
            destination != AppDestination.CreateReport &&
            destination !is AppDestination.Login &&
            destination != AppDestination.LocationPicker
        ) {
            _uiState.update { it.copy(dialog = AppDialog.DiscardDraft(destination)) }
            return
        }
        if (destination !is AppDestination.ReportDetail) detailGeneration++
        _uiState.update {
            it.copy(
                reports = if (destination is AppDestination.ReportDetail) it.reports else it.reports.map { report -> report.copy(imageBytes = null) },
                detailError = null,
                destination = destination,
                backStack = when {
                    replace -> it.backStack
                    destination == AppDestination.Map -> emptyList()
                    destination == AppDestination.CreateReport ||
                        destination == AppDestination.MyReports -> emptyList()
                    else -> it.backStack + it.destination
                },
                dialog = null,
                message = null,
                authError = null,
                selectedReportId = if (destination == AppDestination.Map) null else it.selectedReportId,
            )
        }
        if (destination is AppDestination.ReportDetail) {
            refreshReportDetail(destination.reportId)
        }
        if (destination == AppDestination.MyReports) {
            refreshMyReports()
        }
    }

    fun back() {
        val state = _uiState.value
        if (state.dialog != null) {
            dismissDialog()
            return
        }
        if (state.isSubmitting) {
            _uiState.update { it.copy(message = "Esperá a que termine el envío antes de salir.") }
            return
        }
        if (state.user == null && state.destination is AppDestination.Login) {
            return
        }
        if (state.user == null && state.destination is AppDestination.Register) {
            _uiState.update {
                it.copy(
                    destination = AppDestination.Login(state.destination.returnTo),
                    backStack = emptyList(),
                    authError = null,
                )
            }
            return
        }
        if (state.destination == AppDestination.CreateReport && state.draft.isDirty) {
            _uiState.update { it.copy(dialog = AppDialog.DiscardDraft(AppDestination.Map)) }
            return
        }
        if (state.destination is AppDestination.Confirmation) {
            _uiState.update { it.copy(destination = AppDestination.Map, backStack = emptyList()) }
            return
        }
        val previous = state.backStack.lastOrNull() ?: AppDestination.Map
        _uiState.update { it.copy(destination = previous, backStack = it.backStack.dropLast(1)) }
    }

    fun setMapPresentation(value: MapPresentation) = _uiState.update { it.copy(mapPresentation = value) }
    fun setQuery(value: String) = _uiState.update { it.copy(mapFilters = it.mapFilters.copy(query = value)) }
    fun setStatusFilter(value: ReportStatus?) = _uiState.update { it.copy(mapFilters = it.mapFilters.copy(status = value)) }
    fun setCategoryFilter(value: String?) = _uiState.update { it.copy(mapFilters = it.mapFilters.copy(categoryId = value)) }
    fun clearFilters() = _uiState.update { it.copy(mapFilters = ReportFilters(), dialog = null) }

    fun selectReportOnMap(report: Report) = _uiState.update {
        it.copy(selectedReportId = report.id)
    }

    fun setMyReportsQuery(value: String) = _uiState.update {
        it.copy(myReportsFilters = it.myReportsFilters.copy(query = value))
    }

    fun setMyReportsStatus(value: ReportStatus?) = _uiState.update {
        it.copy(myReportsFilters = it.myReportsFilters.copy(status = value))
    }

    fun setMyReportsCategory(value: String?) = _uiState.update {
        it.copy(myReportsFilters = it.myReportsFilters.copy(categoryId = value))
    }

    fun clearMyReportsFilters() = _uiState.update { it.copy(myReportsFilters = ReportFilters()) }

    fun updateDraft(transform: (ReportDraft) -> ReportDraft) = _uiState.update {
        if (it.isSubmitting) it else it.copy(draft = transform(it.draft), formError = null)
    }

    fun confirmLocation(point: MapPoint, label: String) {
        _uiState.update {
            it.copy(
                draft = it.draft.copy(point = point.copy(label = label), location = label),
                destination = AppDestination.CreateReport,
                backStack = it.backStack.dropLast(1),
            )
        }
    }

    fun submitReport() {
        val state = _uiState.value
        if (state.isSubmitting || state.isReadingPhoto) return
        if (!state.draft.canSubmit) {
            _uiState.update { it.copy(formError = "Completá el título, la categoría y la ubicación.") }
            return
        }
        val submittedUser = state.user ?: run {
            navigate(AppDestination.Login(AppDestination.CreateReport))
            return
        }
        val submittedDraft = state.draft
        val submittedCategories = state.categories
        val submissionId = ++submissionGeneration
        _uiState.update { it.copy(isSubmitting = true, formError = null) }
        viewModelScope.launch {
            if (submissionId != submissionGeneration) return@launch
            val latest = _uiState.value
            if (latest.failNextSubmission) {
                _uiState.update {
                    it.copy(
                        isSubmitting = false,
                        failNextSubmission = false,
                        formError = "No pudimos confirmar si el reporte se envió. El borrador sigue guardado: revisá la conexión antes de reintentar.",
                    )
                }
                return@launch
            }
            val sessionIsStillValid = latest.user?.id == submittedUser.id
            if (latest.destination != AppDestination.CreateReport || !sessionIsStillValid) {
                _uiState.update { it.copy(isSubmitting = false) }
                return@launch
            }
            val category = submittedCategories.firstOrNull { it.id == submittedDraft.categoryId }
                ?: run {
                    _uiState.update { it.copy(isSubmitting = false) }
                    return@launch
                }
            val sequence = latest.reports.size + 2050
            val report = Report(
                id = "REP-$sequence",
                title = submittedDraft.title.trim(),
                description = submittedDraft.description.trim().ifBlank { "Sin descripción adicional." },
                category = category,
                status = ReportStatus.Pending,
                location = submittedDraft.location,
                point = submittedDraft.point,
                createdAtLabel = "hace un momento",
                ownerId = submittedUser.id,
                hasPhoto = submittedDraft.photo != null,
                photo = submittedDraft.photo,
                history = listOf(
                    StatusEvent(
                        status = ReportStatus.Pending,
                        title = "Reporte recibido",
                        description = "El municipio recibió la incidencia y la revisará.",
                        dateLabel = "Ahora",
                    ),
                ),
            )
            try {
                val createdReport = reportRepository.createReport(report)
                if (submissionId != submissionGeneration) return@launch
                _uiState.update {
                    it.copy(
                        reports = listOf(createdReport) + it.reports.filterNot { current -> current.id == createdReport.id },
                        createdReportId = createdReport.id,
                        draft = ReportDraft(),
                        destination = AppDestination.Confirmation(createdReport.id),
                        backStack = emptyList(),
                        isSubmitting = false,
                        dialog = null,
                        message = null,
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (submissionId == submissionGeneration) {
                    if (error.isUnauthorized()) {
                        expireSession(preserveDraft = true)
                    } else {
                        _uiState.update {
                            it.copy(
                                isSubmitting = false,
                                formError = error.userFacingMessage(
                                    "No pudimos enviar el reporte. El borrador sigue guardado.",
                                ),
                            )
                        }
                    }
                }
            }
        }
    }

    fun login(dni: String, password: String) {
        if (!dni.hasValidDniFormat() || password.isBlank() || password.toByteArray().size > 72) {
            _uiState.update { it.copy(authError = "Revisá el DNI y la contraseña.") }
            return
        }
        if (_uiState.value.isAuthenticating) return
        _uiState.update { it.copy(isAuthenticating = true, authError = null) }
        viewModelScope.launch {
            try {
                finishAuthentication(authRepository.login(dni.trim(), password))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isAuthenticating = false,
                        authError = error.userFacingMessage("No pudimos iniciar sesión."),
                    )
                }
            }
        }
    }

    fun register(displayName: String, dni: String, password: String) {
        if (displayName.isBlank() || !dni.hasValidDniFormat() || password.length < 8 || password.toByteArray().size > 72) {
            _uiState.update {
                it.copy(authError = "Ingresá un nombre, un DNI válido y una contraseña de al menos 8 caracteres.")
            }
            return
        }
        if (_uiState.value.isAuthenticating) return
        _uiState.update { it.copy(isAuthenticating = true, authError = null) }
        viewModelScope.launch {
            try {
                finishAuthentication(authRepository.register(displayName.trim(), dni.trim(), password))
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _uiState.update {
                    it.copy(
                        isAuthenticating = false,
                        authError = error.userFacingMessage("No pudimos crear la cuenta."),
                    )
                }
            }
        }
    }

    private fun finishAuthentication(user: User) {
        sessionGeneration++
        val current = _uiState.value.destination
        val target = when (current) {
            is AppDestination.Login -> current.returnTo
            is AppDestination.Register -> current.returnTo
            else -> AppDestination.Map
        }
        val draft = if (_uiState.value.user == null && target == AppDestination.CreateReport) _uiState.value.draft else ReportDraft()
        _uiState.value = AppUiState(
            user = user, destination = target, draft = draft,
            isRestoringSession = false, message = "Sesión iniciada.",
        )
        refreshMyReports()
    }

    fun logout() {
        if (_uiState.value.isAuthenticating) return
        submissionGeneration++
        _uiState.update { it.copy(isAuthenticating = true) }
        viewModelScope.launch {
            try { authRepository.logout() }
            catch (error: CancellationException) { throw error }
            catch (_: Throwable) { authRepository.clearSession() }
            clearLocalSession("Sesión cerrada.")
        }
    }

    private fun clearLocalSession(message: String) {
        sessionGeneration++
        listGeneration++
        detailGeneration++
        photoGeneration++
        submissionGeneration++
        seenCursors.clear()
        _uiState.value = AppUiState(isRestoringSession = false, message = message)
    }

    private suspend fun expireSession(preserveDraft: Boolean = false) {
        val draft = if (preserveDraft) _uiState.value.draft else ReportDraft()
        authRepository.clearSession()
        clearLocalSession("La sesión venció. Iniciá sesión nuevamente.")
        _uiState.update { it.copy(
            draft = draft,
            destination = AppDestination.Login(if (preserveDraft) AppDestination.CreateReport else AppDestination.Map),
            authError = if (preserveDraft) "La sesión venció. Iniciá sesión nuevamente; el borrador se conservó." else "La sesión venció. Iniciá sesión nuevamente.",
        ) }
    }

    fun setPhoto(hasPhoto: Boolean, issue: PhotoIssue? = null) {
        photoGeneration++
        _uiState.update {
            if (it.isSubmitting) it.copy(dialog = null) else it.copy(
                draft = it.draft.copy(hasPhoto = hasPhoto && it.draft.photo != null, photo = if (hasPhoto) it.draft.photo else null, photoIssue = issue),
                isReadingPhoto = false, dialog = null, formError = null,
            )
        }
    }

    fun attachPhoto(uri: String) {
        if (_uiState.value.user == null || _uiState.value.isSubmitting) return
        val generation = ++photoGeneration
        val session = sessionGeneration
        _uiState.update { it.copy(isReadingPhoto = true, dialog = null) }
        viewModelScope.launch {
            try {
                val photo = AppContainer.photoRepository.read(uri)
                if (generation != photoGeneration || session != sessionGeneration) return@launch
                _uiState.update { it.copy(isReadingPhoto = false, draft = it.draft.copy(photo = photo, hasPhoto = true, photoIssue = null)) }
            } catch (error: CancellationException) { throw error
            } catch (error: Throwable) {
                if (generation != photoGeneration || session != sessionGeneration) return@launch
                _uiState.update { it.copy(isReadingPhoto = false, draft = it.draft.copy(photoIssue = (error as? PhotoReadException)?.issue ?: PhotoIssue.PermissionDenied)) }
            }
        }
    }

    fun simulateContentError() = _uiState.update {
        it.copy(
            contentLoadState = ContentLoadState.Error,
            contentLoadError = "No pudimos conectar con el servidor.",
            myReportsLoadState = ContentLoadState.Error,
            myReportsLoadError = "No pudimos conectar con el servidor.",
            dialog = null,
        )
    }

    fun retryContent() {
        refreshMyReports()
    }

    fun failNextSubmission() = _uiState.update {
        it.copy(
            failNextSubmission = true,
            dialog = null,
            message = "El próximo envío mostrará un error recuperable de conexión.",
        )
    }

    fun simulateSessionExpiry() {
        submissionGeneration++
        viewModelScope.launch { expireSession(preserveDraft = _uiState.value.destination == AppDestination.CreateReport) }
    }

    fun focusReportOnMap(report: Report) = _uiState.update {
        it.copy(
            selectedReportId = report.id,
            mapFilters = ReportFilters(),
            mapPresentation = MapPresentation.Map,
            destination = AppDestination.Map,
            backStack = emptyList(),
        )
    }

    private fun restoreSession() {
        val generation = sessionGeneration
        viewModelScope.launch {
            try {
                val user = authRepository.restoreSession()
                if (generation != sessionGeneration || _uiState.value.isAuthenticating) return@launch
                if (user == null) _uiState.update { it.copy(isRestoringSession = false) }
                else finishAuthentication(user)
            } catch (error: CancellationException) { throw error
            } catch (error: Throwable) {
                if (generation == sessionGeneration) {
                    authRepository.clearSession()
                    clearLocalSession("No pudimos validar la sesión guardada. Iniciá sesión nuevamente.")
                }
            }
        }
    }

    private fun refreshMyReports() = loadReports(append = false)
    fun loadMoreReports() = loadReports(append = true)

    private fun loadReports(append: Boolean) {
        val state = _uiState.value
        val user = state.user ?: return
        if (append && (state.nextCursor == null || state.isLoadingMore || state.myReportsLoadState == ContentLoadState.Loading)) return
        val cursor = if (append) state.nextCursor else null
        val session = sessionGeneration
        val request = ++listGeneration
        if (!append) seenCursors.clear()
        _uiState.update { it.copy(
            isLoadingMore = append, paginationError = null,
            myReportsLoadState = if (append) it.myReportsLoadState else ContentLoadState.Loading,
            contentLoadState = if (append) it.contentLoadState else ContentLoadState.Loading,
            myReportsLoadError = null, contentLoadError = null,
        ) }
        viewModelScope.launch {
            try {
                val categories = if (append) state.categories else reportRepository.getCategories()
                val page = reportRepository.getCurrentUserReports(user.id, categories, cursor)
                if (session != sessionGeneration || request != listGeneration) return@launch
                cursor?.let { seenCursors.add(it) }
                val repeated = page.nextCursor != null && page.nextCursor in seenCursors
                _uiState.update { it.copy(
                    categories = categories,
                    reports = ((if (append) it.reports else emptyList()) + page.items.filter { report -> report.ownerId == user.id }).distinctBy { report -> report.id },
                    nextCursor = if (repeated) null else page.nextCursor,
                    isLoadingMore = false,
                    paginationError = if (repeated) "El servidor repitió una página. Actualizá tus reportes." else null,
                    myReportsLoadState = ContentLoadState.Ready, contentLoadState = ContentLoadState.Ready,
                ) }
            } catch (error: CancellationException) { throw error
            } catch (error: Throwable) {
                if (session != sessionGeneration || request != listGeneration) return@launch
                if (error.isUnauthorized()) expireSession()
                else _uiState.update { it.copy(
                    isLoadingMore = false,
                    paginationError = if (append) error.userFacingMessage("No pudimos cargar más reportes.") else null,
                    myReportsLoadState = if (append) it.myReportsLoadState else ContentLoadState.Error,
                    contentLoadState = if (append) it.contentLoadState else ContentLoadState.Error,
                    myReportsLoadError = error.userFacingMessage("No pudimos cargar tus reportes."),
                    contentLoadError = error.userFacingMessage("No pudimos cargar tus reportes."),
                ) }
            }
        }
    }

    private fun refreshReportDetail(reportId: String) {
        val user = _uiState.value.user ?: return
        val session = sessionGeneration
        val request = ++detailGeneration
        viewModelScope.launch {
            var loadingImage = false
            try {
                val detail = reportRepository.getCurrentUserReport(reportId, user.id, _uiState.value.categories)
                if (session != sessionGeneration || request != detailGeneration) return@launch
                _uiState.update { it.copy(reports = it.reports.filterNot { report -> report.id == reportId }.map { report -> report.copy(imageBytes = null) } + detail) }
                if (detail.hasPhoto) {
                    loadingImage = true
                    val image = reportRepository.getCurrentUserReportImage(reportId)
                    require(image.size <= 5 * 1024 * 1024) { "La foto supera el tamaño permitido." }
                    if (session != sessionGeneration || request != detailGeneration) return@launch
                    _uiState.update { it.copy(reports = it.reports.map { report -> if (report.id == reportId) report.copy(imageBytes = image) else report }) }
                }
            } catch (error: CancellationException) { throw error
            } catch (error: Throwable) {
                if (session != sessionGeneration || request != detailGeneration) return@launch
                if (error.isUnauthorized()) expireSession()
                else _uiState.update { it.copy(
                    reports = if (!loadingImage && (error as? ApiException)?.statusCode == 404) it.reports.filterNot { report -> report.id == reportId } else it.reports,
                    detailError = error.userFacingMessage("No pudimos actualizar el detalle o cargar la fotografía. Los datos pueden estar desactualizados."),
                ) }
            }
        }
    }

    fun showDialog(dialog: AppDialog) = _uiState.update { it.copy(dialog = dialog) }
    fun dismissDialog() = _uiState.update { it.copy(dialog = null) }
    fun consumeMessage() = _uiState.update { it.copy(message = null) }
    fun clearAuthError() = _uiState.update { it.copy(authError = null) }
    fun notify(message: String) = _uiState.update { it.copy(message = message) }

    fun discardDraftAndContinue() {
        photoGeneration++
        val target = (_uiState.value.dialog as? AppDialog.DiscardDraft)?.target ?: AppDestination.Map
        _uiState.update {
            it.copy(
                draft = ReportDraft(),
                dialog = null,
                destination = target,
                backStack = emptyList(),
            )
        }
    }

    fun keepDraftAndContinue() {
        val target = (_uiState.value.dialog as? AppDialog.DiscardDraft)?.target ?: AppDestination.Map
        _uiState.update {
            it.copy(
                dialog = null,
                destination = target,
                backStack = emptyList(),
            )
        }
    }

    fun deactivateAccount() {
        if (_uiState.value.isAuthenticating) return
        submissionGeneration++
        _uiState.update { it.copy(isAuthenticating = true, dialog = null) }
        viewModelScope.launch {
            try {
                authRepository.deactivateAccount()
                clearLocalSession("La cuenta fue desactivada.")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                if (error.isUnauthorized()) { expireSession(); return@launch }
                _uiState.update {
                    it.copy(
                        isAuthenticating = false,
                        message = error.userFacingMessage("No pudimos desactivar la cuenta."),
                    )
                }
            }
        }
    }

}

private fun Throwable.userFacingMessage(fallback: String): String = toUserFacingMessage(fallback)

private fun Throwable.isUnauthorized(): Boolean =
    (this as? ApiException)?.statusCode == 401

private fun String.hasValidDniFormat(): Boolean =
    trim().matches(Regex("(?:[0-9]{7,8}|[0-9]{1,2}\\.[0-9]{3}\\.[0-9]{3})")) &&
        trim().replace(".", "").any { it != '0' }
