package com.proje.sosyal.android.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proje.sosyal.android.core.common.AppError
import com.proje.sosyal.android.core.common.AppResult
import com.proje.sosyal.android.domain.usecase.GetServerStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Connected(val apiVersion: String) : HomeUiState
    data class Error(val error: AppError) : HomeUiState
}

/**
 * Aşama 1: Ana sayfa yalnızca backend bağlantısını doğrular.
 * Gönderi akışı Aşama 3'te bu ViewModel'e eklenecek.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getServerStatus: GetServerStatusUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    private fun load() {
        _uiState.value = HomeUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val result = getServerStatus()) {
                is AppResult.Success ->
                    if (result.data.isUp) HomeUiState.Connected(result.data.apiVersion)
                    else HomeUiState.Error(AppError.SERVER)
                is AppResult.Failure -> HomeUiState.Error(result.error)
            }
        }
    }
}
