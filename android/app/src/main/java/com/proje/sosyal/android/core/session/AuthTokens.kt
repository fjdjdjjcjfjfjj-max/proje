package com.proje.sosyal.android.core.session

data class AuthTokens(
    val accessToken: String,
    val refreshToken: String,
)

sealed interface SessionState {
    /** Kayıtlı oturum diskten okunuyor. */
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data object LoggedIn : SessionState
}
