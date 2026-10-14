package com.proje.sosyal.user

import com.proje.sosyal.common.error.ApiException
import com.proje.sosyal.common.error.ErrorCode
import com.proje.sosyal.user.dto.MeResponse
import com.proje.sosyal.user.dto.ProfileStats
import com.proje.sosyal.user.dto.UpdateMeRequest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class UserService(
    private val users: UserRepository,
    private val jdbc: JdbcTemplate,
) {
    @Transactional(readOnly = true)
    fun me(userId: UUID): MeResponse = toMe(load(userId))

    @Transactional(readOnly = true)
    fun toMe(user: User): MeResponse = MeResponse.from(user, statsFor(user.id))

    @Transactional
    fun updateMe(userId: UUID, request: UpdateMeRequest): MeResponse {
        val user = load(userId)
        request.displayName?.let {
            val trimmed = it.trim()
            if (trimmed.isEmpty()) throw ApiException(ErrorCode.VALIDATION_FAILED, "Görünen ad boş olamaz.")
            user.displayName = trimmed
        }
        request.bio?.let { user.bio = it.trim().ifEmpty { null } }
        request.profileVisibility?.let { visibility ->
            // 18 yaş altı hesaplar profillerini herkese açamaz (docs/age-safety.md).
            if (visibility == ProfileVisibility.PUBLIC && user.ageGroup == AgeGroup.MINOR) {
                throw ApiException(ErrorCode.PROFILE_VISIBILITY_NOT_ALLOWED)
            }
            user.profileVisibility = visibility
        }
        return toMe(users.saveAndFlush(user))
    }

    private fun load(userId: UUID): User =
        users.findById(userId).filter { it.isActive }.orElseThrow { ApiException(ErrorCode.UNAUTHORIZED) }

    private fun statsFor(userId: UUID): ProfileStats =
        jdbc.queryForObject(
            "SELECT count(*), coalesce(sum(like_count), 0) FROM posts WHERE author_id = ? AND status = 'PUBLISHED'",
            { rs, _ -> ProfileStats(postCount = rs.getLong(1), likesReceived = rs.getLong(2)) },
            userId,
        )!!
}
