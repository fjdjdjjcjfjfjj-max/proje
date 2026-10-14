package com.proje.sosyal.android.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proje.sosyal.android.core.common.AppError
import com.proje.sosyal.android.core.common.AppResult
import com.proje.sosyal.android.domain.model.ProfileUpdate
import com.proje.sosyal.android.domain.model.UserProfile
import com.proje.sosyal.android.domain.repository.AuthRepository
import com.proje.sosyal.android.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Loaded(val profile: UserProfile, val edit: EditState = EditState()) : ProfileUiState
    data class Error(val error: AppError) : ProfileUiState
}

data class EditState(
    val isOpen: Boolean = false,
    val isSaving: Boolean = false,
    val error: AppError? = null,
    val serverMessage: String? = null,
)

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

    fun openEditor() = updateLoaded { it.copy(edit = EditState(isOpen = true)) }

    fun closeEditor() = updateLoaded { if (it.edit.isSaving) it else it.copy(edit = EditState()) }

    fun save(update: ProfileUpdate) {
        val loaded = _state.value as? ProfileUiState.Loaded ?: return
        if (loaded.edit.isSaving) return
        updateLoaded { it.copy(edit = it.edit.copy(isSaving = true, error = null, serverMessage = null)) }
        viewModelScope.launch {
            when (val result = users.updateMe(update)) {
                is AppResult.Success -> _state.value = ProfileUiState.Loaded(result.data)
                is AppResult.Failure -> updateLoaded {
                    it.copy(edit = it.edit.copy(isSaving = false, error = result.error, serverMessage = result.message))
                }
            }
        }
    }

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

    private fun updateLoaded(change: (ProfileUiState.Loaded) -> ProfileUiState.Loaded) =
        _state.update { if (it is ProfileUiState.Loaded) change(it) else it }
}
