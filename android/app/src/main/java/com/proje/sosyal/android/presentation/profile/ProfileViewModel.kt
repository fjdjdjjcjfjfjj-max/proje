package com.proje.sosyal.android.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proje.sosyal.android.core.common.AppError
import com.proje.sosyal.android.core.common.AppResult
import com.proje.sosyal.android.domain.model.UserProfile
import com.proje.sosyal.android.domain.repository.AuthRepository
import com.proje.sosyal.android.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Loaded(val profile: UserProfile) : ProfileUiState
    data class Error(val error: AppError) : ProfileUiState
}

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val users: UserRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<ProfileUiState>(ProfileUiState.Loading)
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun retry() = load()

    /** Oturum kapanınca navigasyon otomatik olarak giriş ekranına döner. */
    fun logout() {
        viewModelScope.launch { auth.logout() }
    }

    private fun load() {
        _state.value = ProfileUiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = users.getMe()) {
                is AppResult.Success -> ProfileUiState.Loaded(result.data)
                is AppResult.Failure -> ProfileUiState.Error(result.error)
            }
        }
    }
}
