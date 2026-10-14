package com.proje.sosyal.android.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proje.sosyal.android.R
import com.proje.sosyal.android.core.common.AppError
import com.proje.sosyal.android.core.common.AppResult
import com.proje.sosyal.android.domain.model.RegistrationForm
import com.proje.sosyal.android.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class RegisterUiState(
    val username: String = "",
    val displayName: String = "",
    val email: String = "",
    val password: String = "",
    val birthDate: LocalDate? = null,
    val privacyNoticeAccepted: Boolean = false,
    val fieldErrors: Map<RegisterField, Int> = emptyMap(),
    val isSubmitting: Boolean = false,
    val error: AppError? = null,
    val serverMessage: String? = null,
)

enum class RegisterField { USERNAME, DISPLAY_NAME, EMAIL, PASSWORD, BIRTH_DATE, PRIVACY }

/**
 * İstemci tarafı doğrulama yalnızca kullanıcıya hızlı geri bildirim içindir;
 * asıl doğrulama (yaş sınırı dahil) backend'de yapılır.
 */
@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val auth: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    fun onUsernameChange(v: String) = edit(RegisterField.USERNAME) { it.copy(username = v) }
    fun onDisplayNameChange(v: String) = edit(RegisterField.DISPLAY_NAME) { it.copy(displayName = v) }
    fun onEmailChange(v: String) = edit(RegisterField.EMAIL) { it.copy(email = v) }
    fun onPasswordChange(v: String) = edit(RegisterField.PASSWORD) { it.copy(password = v) }
    fun onBirthDateChange(v: LocalDate) = edit(RegisterField.BIRTH_DATE) { it.copy(birthDate = v) }
    fun onPrivacyAcceptedChange(v: Boolean) = edit(RegisterField.PRIVACY) { it.copy(privacyNoticeAccepted = v) }

    fun submit() {
        val current = _state.value
        if (current.isSubmitting) return
        val errors = validate(current)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(fieldErrors = errors) }
            return
        }
        _state.update { it.copy(isSubmitting = true, error = null, serverMessage = null) }
        viewModelScope.launch {
            val result = auth.register(
                RegistrationForm(
                    username = current.username,
                    displayName = current.displayName,
                    email = current.email,
                    password = current.password,
                    birthDate = current.birthDate!!,
                    privacyNoticeAccepted = current.privacyNoticeAccepted,
                ),
            )
            _state.update {
                when (result) {
                    is AppResult.Success -> it.copy(isSubmitting = false, password = "")
                    is AppResult.Failure -> it.copy(
                        isSubmitting = false,
                        error = result.error,
                        serverMessage = result.message,
                        fieldErrors = serverFieldError(result.error)?.let { field -> it.fieldErrors + field } ?: it.fieldErrors,
                    )
                }
            }
        }
    }

    private fun edit(field: RegisterField, change: (RegisterUiState) -> RegisterUiState) =
        _state.update { change(it).copy(fieldErrors = it.fieldErrors - field, error = null) }

    private fun serverFieldError(error: AppError): Pair<RegisterField, Int>? = when (error) {
        AppError.USERNAME_TAKEN -> RegisterField.USERNAME to R.string.error_username_taken
        AppError.EMAIL_TAKEN -> RegisterField.EMAIL to R.string.error_email_taken
        AppError.AGE_NOT_ELIGIBLE -> RegisterField.BIRTH_DATE to R.string.error_age_not_eligible
        else -> null
    }

    companion object {
        private val USERNAME = Regex("^[A-Za-z0-9_.]{3,30}$")
        private val EMAIL = Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")

        internal fun validate(s: RegisterUiState): Map<RegisterField, Int> = buildMap {
            if (!USERNAME.matches(s.username.trim())) put(RegisterField.USERNAME, R.string.validation_username)
            if (s.displayName.trim().length !in 1..50) put(RegisterField.DISPLAY_NAME, R.string.validation_display_name)
            if (s.email.trim().length > 254 || !EMAIL.matches(s.email.trim())) put(RegisterField.EMAIL, R.string.validation_email)
            if (s.password.length !in 10..64) put(RegisterField.PASSWORD, R.string.validation_password)
            if (s.birthDate == null) put(RegisterField.BIRTH_DATE, R.string.validation_birth_date)
            if (!s.privacyNoticeAccepted) put(RegisterField.PRIVACY, R.string.validation_privacy)
        }
    }
}
