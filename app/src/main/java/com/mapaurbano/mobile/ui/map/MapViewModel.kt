package com.mapaurbano.mobile.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapaurbano.mobile.data.ReportRepository
import com.mapaurbano.mobile.model.ReportSummary
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface MapUiState {
  data object Loading : MapUiState

  data class Success(val reports: List<ReportSummary>) : MapUiState

  data class Error(val message: String) : MapUiState
}

class MapViewModel(repository: ReportRepository) : ViewModel() {
  val uiState: StateFlow<MapUiState> =
    repository
      .getPublicReportsStream()
      .map<List<ReportSummary>, MapUiState>(MapUiState::Success)
      .catch { error -> emit(MapUiState.Error(error.message ?: "No se pudieron cargar los reportes")) }
      .stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MapUiState.Loading,
      )
}
