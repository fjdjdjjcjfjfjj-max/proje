package com.proje.sosyal.user.dto

import com.proje.sosyal.user.AgeGroup
import com.proje.sosyal.user.AgeVerificationStatus
import com.proje.sosyal.user.ProfileVisibility
import com.proje.sosyal.user.Role
import com.proje.sosyal.user.User
import java.time.Instant
import java.util.UUID

/** Oturum sahibinin kendi hesap bilgileri. Yalnızca kullanıcının kendisine döner. */
data class MeResponse(
    val id: UUID,
    val username: String,
    val displayName: String,
    val email: String,
    val bio: String?,
    val role: Role,
    val ageGroup: AgeGroup,
    val ageVerificationStatus: AgeVerificationStatus,
    val profileVisibility: ProfileVisibility,
    val createdAt: Instant,
) {
    companion object {
        fun from(user: User) = MeResponse(
            id = user.id,
            username = user.username,
            displayName = user.displayName,
            email = user.email,
            bio = user.bio,
            role = user.role,
            ageGroup = user.ageGroup,
            ageVerificationStatus = user.ageVerificationStatus,
            profileVisibility = user.profileVisibility,
            createdAt = user.createdAt,
        )
    }
}
