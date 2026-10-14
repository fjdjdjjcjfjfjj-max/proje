package com.proje.sosyal.android.domain.model

data class UserProfile(
    val id: String,
    val username: String,
    val displayName: String,
    val email: String,
    val bio: String?,
    val isMinor: Boolean,
    /** Yaş yalnızca bağımsız doğrulama servisi onayladıysa "doğrulanmış" sayılır. */
    val isAgeVerified: Boolean,
    val isProfilePrivate: Boolean,
)
