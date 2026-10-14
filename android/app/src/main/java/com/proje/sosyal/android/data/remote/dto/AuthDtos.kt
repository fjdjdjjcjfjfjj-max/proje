package com.proje.sosyal.android.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequestDto(
    val username: String,
    val displayName: String,
    val email: String,
    val password: String,
    /** ISO-8601 (yyyy-MM-dd). Backend yalnızca yaş grubunu hesaplar, tarihi saklamaz. */
    val birthDate: String,
    val privacyNoticeAccepted: Boolean,
)

@Serializable
data class LoginRequestDto(val identifier: String, val password: String)

@Serializable
data class RefreshRequestDto(val refreshToken: String)

@Serializable
data class AuthResponseDto(
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val refreshExpiresIn: Long,
    val user: MeDto,
)

@Serializable
data class MeDto(
    val id: String,
    val username: String,
    val displayName: String,
    val email: String,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val bannerUrl: String? = null,
    val role: String,
    val ageGroup: String,
    val ageVerificationStatus: String,
    val profileVisibility: String,
    val createdAt: String,
    val stats: ProfileStatsDto = ProfileStatsDto(),
)

@Serializable
data class ProfileStatsDto(val postCount: Long = 0, val likesReceived: Long = 0)

/** Kısmi güncelleme: null alanlar gönderilmez (explicitNulls = false) ve değişmez. */
@Serializable
data class UpdateMeRequestDto(
    val displayName: String? = null,
    val bio: String? = null,
    val profileVisibility: String? = null,
)
