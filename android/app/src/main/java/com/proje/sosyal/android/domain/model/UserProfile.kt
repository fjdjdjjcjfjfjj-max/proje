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
    val postCount: Long = 0,
    val likesReceived: Long = 0,
    /** Tam adres (API taban adresi eklenmiş); yoksa null. */
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
)

enum class ProfileImageKind { AVATAR, BANNER }

data class ProfileUpdate(
    val displayName: String,
    val bio: String,
    val isPrivate: Boolean,
)
