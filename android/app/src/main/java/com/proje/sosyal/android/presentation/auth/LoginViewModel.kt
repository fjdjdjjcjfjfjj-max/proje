package com.proje.sosyal.android.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proje.sosyal.android.core.common.AppError
import com.proje.sosyal.android.core.common.AppResult
import com.proje.sosyal.android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val identifier: String = "",
    val password: String = "",
    val isSubmitting: Boolean = false,
    val error: AppError? = null,
    val serverMessage: String? = null,
) {
    val canSubmit: Boolean get() = identifier.isNotBlank() && password.isNotEmpty() && !isSubmitting
}

/** Başarılı girişte ekran değiştirmez: oturum durumu değişince navigasyon ana uygulamaya geçer. */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    fun onIdentifierChange(value: String) = _state.update { it.copy(identifier = value, error = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, error = null) }

    fun submit() {
        val current = _state.value
        if (!current.canSubmit) return
        _state.update { it.copy(isSubmitting = true, error = null, serverMessage = null) }
        viewModelScope.launch {
            val result = auth.login(current.identifier, current.password)
            _state.update {
                when (result) {
                    is AppResult.Success -> it.copy(isSubmitting = false, password = "")
                    is AppResult.Failure -> it.copy(isSubmitting = false, error = result.error, serverMessage = result.message)
                }
            }
        }
    }
}
