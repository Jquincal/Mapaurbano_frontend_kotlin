package com.mapaurbano.mobile

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import java.io.File
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mapaurbano.mobile.core.data.AppDialog
import com.mapaurbano.mobile.core.data.AppUiState
import com.mapaurbano.mobile.core.data.AppViewModel
import com.mapaurbano.mobile.core.model.ContentLoadState
import com.mapaurbano.mobile.core.model.PhotoIssue
import com.mapaurbano.mobile.core.navigation.AppDestination
import com.mapaurbano.mobile.core.navigation.isMainDestination
import com.mapaurbano.mobile.feature.auth.LoginScreen
import com.mapaurbano.mobile.feature.auth.ProfileScreen
import com.mapaurbano.mobile.feature.auth.RegisterScreen
import com.mapaurbano.mobile.feature.common.ContentState
import com.mapaurbano.mobile.feature.common.NoticeCard
import com.mapaurbano.mobile.feature.common.ScrollableFeatureScreen
import com.mapaurbano.mobile.feature.common.StitchBottomNavigation
import com.mapaurbano.mobile.feature.map.MapScreen
import com.mapaurbano.mobile.feature.myreports.MyReportsScreen
import com.mapaurbano.mobile.feature.myreports.ReportDetailScreen
import com.mapaurbano.mobile.feature.report.ConfirmationScreen
import com.mapaurbano.mobile.feature.report.LocationPickerScreen
import com.mapaurbano.mobile.feature.report.ReportFormScreen

@Composable
fun MapaUrbanoApp(appViewModel: AppViewModel = viewModel()) {
    val state by appViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    var cameraUri by rememberSaveable { mutableStateOf<String?>(null) }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { appViewModel.attachPhoto(it.toString()) }
    }
    val camera = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        val uri = cameraUri
        cameraUri = null
        if (success && uri != null) appViewModel.attachPhoto(uri)
        else uri?.let { context.contentResolver.delete(Uri.parse(it), null, null) }
    }

    var dni by rememberSaveable { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var displayName by rememberSaveable { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    LaunchedEffect(state.user) {
        if (state.user != null) {
            password = ""
            confirmPassword = ""
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            appViewModel.consumeMessage()
        }
    }

    BackHandler(
        enabled = state.dialog != null ||
            state.destination is AppDestination.Register ||
            (state.user != null && state.destination != AppDestination.Map),
    ) {
        if (state.destination is AppDestination.Login || state.destination is AppDestination.Register) {
            password = ""
            confirmPassword = ""
        }
        appViewModel.back()
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (state.destination.isMainDestination) {
                MainNavigationBar(
                    destination = state.destination,
                    onNavigate = { appViewModel.navigate(it) },
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            DestinationContent(
                state = state,
                viewModel = appViewModel,
                dni = dni,
                password = password,
                displayName = displayName,
                confirmPassword = confirmPassword,
                onDniChange = {
                    dni = it
                    appViewModel.clearAuthError()
                },
                onPasswordChange = {
                    password = it
                    appViewModel.clearAuthError()
                },
                onDisplayNameChange = {
                    displayName = it
                    appViewModel.clearAuthError()
                },
                onConfirmPasswordChange = {
                    confirmPassword = it
                    appViewModel.clearAuthError()
                },
                onClearSecrets = {
                    password = ""
                    confirmPassword = ""
                },
                onShareText = { text ->
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, text)
                    }
                    context.startActivity(Intent.createChooser(intent, "Compartir con"))
                },
            )

        }
    }

    AppDialogHost(
        dialog = state.dialog,
        onDismiss = appViewModel::dismissDialog,
        onKeepDraft = appViewModel::keepDraftAndContinue,
        onDiscardDraft = appViewModel::discardDraftAndContinue,
        onDeactivate = appViewModel::deactivateAccount,
        onGallery = {
            appViewModel.dismissDialog()
            try { gallery.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
            catch (_: Exception) { appViewModel.setPhoto(false, PhotoIssue.PermissionDenied) }
        },
        onCamera = {
            appViewModel.dismissDialog()
            try {
                val directory = File(context.cacheDir, "camera").apply { mkdirs() }
                val file = File.createTempFile("reporte-", ".jpg", directory)
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                cameraUri = uri.toString()
                camera.launch(uri)
            } catch (_: Exception) {
                cameraUri?.let { context.contentResolver.delete(Uri.parse(it), null, null) }
                cameraUri = null
                appViewModel.setPhoto(false, PhotoIssue.PermissionDenied)
            }
        },
    )
}

@Composable
private fun DestinationContent(
    state: AppUiState,
    viewModel: AppViewModel,
    dni: String,
    password: String,
    displayName: String,
    confirmPassword: String,
    onDniChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onClearSecrets: () -> Unit,
    onShareText: (String) -> Unit,
) {
    if (state.isRestoringSession) {
        SessionRestoringScreen()
        return
    }

    when (val destination = state.destination) {
        AppDestination.Map -> MapScreen(
            reports = state.visibleReports,
            categories = state.categories,
            filters = state.mapFilters,
            presentation = state.mapPresentation,
            contentLoadState = state.contentLoadState,
            contentErrorMessage = state.contentLoadError,
            onQueryChange = viewModel::setQuery,
            onPresentationChange = viewModel::setMapPresentation,
            onStatusFilterChange = viewModel::setStatusFilter,
            onCategoryFilterChange = viewModel::setCategoryFilter,
            onClearFilters = viewModel::clearFilters,
            onReportClick = { viewModel.navigate(AppDestination.ReportDetail(it.id)) },
            onReportSelect = viewModel::selectReportOnMap,
            onCreateReport = { viewModel.navigate(AppDestination.CreateReport) },
            onRetry = viewModel::retryContent,
            selectedReportId = state.selectedReportId,
            onProfile = { viewModel.navigate(AppDestination.Profile) },
            onNotifications = {
                viewModel.showDialog(AppDialog.FutureFeature("Notificaciones"))
            },
        )

        AppDestination.CreateReport -> ReportFormScreen(
            draft = state.draft,
            categories = state.categories,
            validationMessage = state.formError,
            onDraftChange = { draft -> viewModel.updateDraft { draft } },
            onChooseLocation = { viewModel.navigate(AppDestination.LocationPicker) },
            onPhotoRequest = { viewModel.showDialog(AppDialog.PhotoPicker) },
            onRemovePhoto = { viewModel.setPhoto(false) },
            onSubmit = viewModel::submitReport,
            onBack = viewModel::back,
            isSubmitting = state.isSubmitting,
            isReadingPhoto = state.isReadingPhoto,
        )

        AppDestination.MyReports -> MyReportsScreen(
            reports = state.ownedReports,
            hasMore = state.nextCursor != null,
            loadingMore = state.isLoadingMore,
            paginationError = state.paginationError,
            onLoadMore = viewModel::loadMoreReports,
            categories = state.categories,
            filters = state.myReportsFilters,
            contentState = when (state.myReportsLoadState) {
                ContentLoadState.Ready -> ContentState.Ready
                ContentLoadState.Loading -> ContentState.Loading
                ContentLoadState.Error -> ContentState.Error(
                    state.myReportsLoadError ?: "No pudimos cargar tus reportes.",
                )
            },
            onQueryChange = viewModel::setMyReportsQuery,
            onStatusFilterChange = viewModel::setMyReportsStatus,
            onCategoryFilterChange = viewModel::setMyReportsCategory,
            onClearFilters = viewModel::clearMyReportsFilters,
            onReportClick = { viewModel.navigate(AppDestination.ReportDetail(it.id)) },
            onCreateReport = { viewModel.navigate(AppDestination.CreateReport) },
            onRetry = viewModel::retryContent,
            onProfile = { viewModel.navigate(AppDestination.Profile) },
            onNotifications = {
                viewModel.showDialog(AppDialog.FutureFeature("Notificaciones"))
            },
        )

        AppDestination.Profile -> state.user?.let { user ->
            ProfileScreen(
                user = user,
                message = state.detailError,
                onBack = viewModel::back,
                onEditProfile = { viewModel.showDialog(AppDialog.FutureFeature("Editar perfil")) },
                onChangePassword = { viewModel.showDialog(AppDialog.FutureFeature("Cambiar contraseña")) },
                onViewReports = { viewModel.navigate(AppDestination.MyReports) },
                onLogout = viewModel::logout,
                onDeactivateAccount = { viewModel.showDialog(AppDialog.DeactivateAccount) },
            )
        }

        is AppDestination.Login -> LoginScreen(
            dni = dni,
            password = password,
            message = state.authError,
            isAuthenticating = state.isAuthenticating,
            onDniChange = onDniChange,
            onPasswordChange = onPasswordChange,
            onLogin = { viewModel.login(dni, password) },
            onRegister = {
                onClearSecrets()
                viewModel.navigate(AppDestination.Register(destination.returnTo))
            },
            onForgotPassword = { viewModel.showDialog(AppDialog.FutureFeature("Recuperar contraseña")) },
            onUnavailableFeature = { title ->
                viewModel.showDialog(AppDialog.FutureFeature(title))
            },
        )

        is AppDestination.Register -> RegisterScreen(
            displayName = displayName,
            dni = dni,
            password = password,
            confirmPassword = confirmPassword,
            message = state.authError,
            isAuthenticating = state.isAuthenticating,
            onDisplayNameChange = onDisplayNameChange,
            onDniChange = onDniChange,
            onPasswordChange = onPasswordChange,
            onConfirmPasswordChange = onConfirmPasswordChange,
            onRegister = {
                if (password == confirmPassword) {
                    viewModel.register(displayName, dni, password)
                } else {
                    viewModel.notify("Las contraseñas no coinciden.")
                }
            },
            onBack = {
                onClearSecrets()
                viewModel.back()
            },
        )

        is AppDestination.ReportDetail -> {
            val report = state.report(destination.reportId)
            if (report == null) {
                UnavailableReportScreen(onBack = viewModel::back)
            } else {
                ReportDetailScreen(
                    report = report,
                    message = state.detailError,
                    showPrivateInformation = report.ownerId != null && state.user?.id == report.ownerId,
                    onBack = viewModel::back,
                    onViewOnMap = viewModel::focusReportOnMap,
                    onShare = { shareReport ->
                        onShareText("${shareReport.title} · ${shareReport.location} · ${shareReport.status.label}")
                    },
                )
            }
        }

        is AppDestination.Confirmation -> {
            val report = state.report(destination.reportId)
            if (report == null) {
                UnavailableReportScreen(onBack = viewModel::back)
            } else {
                ConfirmationScreen(
                    report = report,
                    onOpenReport = { viewModel.navigate(AppDestination.ReportDetail(report.id)) },
                    onCreateAnother = { viewModel.navigate(AppDestination.CreateReport) },
                    onGoToMap = { viewModel.navigate(AppDestination.Map, replace = true) },
                )
            }
        }

        AppDestination.LocationPicker -> LocationPickerScreen(
            initialPoint = state.draft.point,
            initialLocation = state.draft.location,
            onConfirm = viewModel::confirmLocation,
            onCancel = viewModel::back,
        )
    }
}

@Composable
private fun MainNavigationBar(
    destination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
) {
    StitchBottomNavigation(
        selectedLabel = when (destination) {
            AppDestination.CreateReport -> "Reportar"
            AppDestination.MyReports -> "Mis reportes"
            else -> "Mapa"
        },
        onMap = { onNavigate(AppDestination.Map) },
        onCreate = { onNavigate(AppDestination.CreateReport) },
        onMyReports = { onNavigate(AppDestination.MyReports) },
    )
}

@Composable
private fun AppDialogHost(
    dialog: AppDialog?,
    onDismiss: () -> Unit,
    onKeepDraft: () -> Unit,
    onDiscardDraft: () -> Unit,
    onDeactivate: () -> Unit,
    onGallery: () -> Unit,
    onCamera: () -> Unit,
) {
    when (dialog) {
        null -> Unit
        AppDialog.Help -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Ayuda") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Iniciá sesión para explorar el mapa, crear reportes vinculados a tu cuenta y consultar sus avances desde Mis reportes.")
                    Text("El mapa muestra los reportes de tu cuenta. Podés adjuntar una foto JPEG, PNG o WebP de hasta 5 MB.")
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
        )
        AppDialog.Filters -> InformationalDialog(
            title = "Filtros",
            text = "Combiná estado, categoría y búsqueda. Los mismos resultados se muestran en el mapa y en la lista.",
            onDismiss = onDismiss,
        )
        is AppDialog.FutureFeature -> InformationalDialog(
            title = dialog.title,
            text = "Función prevista para una próxima versión. No se realizaron cambios.",
            onDismiss = onDismiss,
        )
        is AppDialog.DiscardDraft -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("¿Salir del borrador?") },
            text = { Text("Podés conservarlo para retomarlo después o descartarlo.") },
            confirmButton = { TextButton(onClick = onKeepDraft) { Text("Conservar") } },
            dismissButton = { TextButton(onClick = onDiscardDraft) { Text("Descartar") } },
        )
        AppDialog.DeactivateAccount -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Desactivar cuenta") },
            text = { Text("La cuenta se desactivará de forma lógica, se cerrarán sus sesiones y los reportes enviados se conservarán.") },
            confirmButton = { TextButton(onClick = onDeactivate) { Text("Desactivar") } },
            dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        )
        AppDialog.PhotoPicker -> AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Adjuntar fotografía") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("JPEG, PNG o WebP. Máximo 5 MB y 12000 píxeles por lado.")
                    TextButton(onClick = onGallery) { Text("Elegir de la galería") }
                    TextButton(onClick = onCamera) { Text("Tomar una foto") }
                }
            },
            confirmButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun SessionRestoringScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            CircularProgressIndicator()
            Text("Comprobando tu sesión…", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun UnavailableReportScreen(onBack: () -> Unit) {
    ScrollableFeatureScreen(
        title = "Reporte no disponible",
        onBack = onBack,
    ) {
        NoticeCard("El reporte no está disponible para esta cuenta.")
        Button(
            onClick = onBack,
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(minHeight = 48.dp),
        ) {
            Text("Volver")
        }
    }
}

@Composable
private fun InformationalDialog(title: String, text: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}
