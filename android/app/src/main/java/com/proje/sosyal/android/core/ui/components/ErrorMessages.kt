package com.proje.sosyal.android.core.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.proje.sosyal.android.R
import com.proje.sosyal.android.core.common.AppError

@StringRes
fun AppError.messageRes(): Int = when (this) {
    AppError.NETWORK -> R.string.error_network_message
    AppError.SERVER -> R.string.error_server_message
    AppError.UNAUTHORIZED -> R.string.error_session_expired
    AppError.VALIDATION -> R.string.error_validation
    AppError.INVALID_CREDENTIALS -> R.string.error_invalid_credentials
    AppError.ACCOUNT_SUSPENDED -> R.string.error_account_suspended
    AppError.USERNAME_TAKEN -> R.string.error_username_taken
    AppError.EMAIL_TAKEN -> R.string.error_email_taken
    AppError.AGE_NOT_ELIGIBLE -> R.string.error_age_not_eligible
    AppError.TOO_MANY_REQUESTS -> R.string.error_too_many_requests
    AppError.UNKNOWN -> R.string.error_unknown_message
}

/** Ağ/sunucu hatalarında yerel metin; iş kuralı hatalarında backend'in Türkçe mesajı tercih edilir. */
@Composable
fun errorText(error: AppError, serverMessage: String?): String =
    when (error) {
        AppError.NETWORK, AppError.SERVER, AppError.UNKNOWN -> stringResource(error.messageRes())
        else -> serverMessage ?: stringResource(error.messageRes())
    }
