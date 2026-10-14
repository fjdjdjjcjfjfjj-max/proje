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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ProfileUiState {
    data object Loading : ProfileUiState
    data class Loaded(val profile: UserProfile) : ProfileUiState
    data class Error(val error: AppError) : ProfileUiState
}

/** Profil, depodaki ortak `me` akışından okunur: düzenleme ekranında yapılan değişiklik hemen görünür. */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val users: UserRepository,
    private val auth: AuthRepository,
) : ViewModel() {

    private val loadError = MutableStateFlow<AppError?>(null)

    val state: StateFlow<ProfileUiState> = combine(users.me, loadError) { profile, error ->
        when {
            profile != null -> ProfileUiState.Loaded(profile)
            error != null -> ProfileUiState.Error(error)
            else -> ProfileUiState.Loading
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ProfileUiState.Loading)

    init {
        refresh()
    }

    fun refresh() {
        loadError.value = null
        viewModelScope.launch {
            val result = users.getMe()
            if (result is AppResult.Failure) loadError.value = result.error
        }
    }

    /** Oturum kapanınca navigasyon otomatik olarak giriş ekranına döner. */
    fun logout() {
        viewModelScope.launch { auth.logout() }
    }
}
