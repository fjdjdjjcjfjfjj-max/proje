package com.proje.sosyal.user

import com.proje.sosyal.common.error.ApiException
import com.proje.sosyal.common.error.ErrorCode
import com.proje.sosyal.common.error.RateLimitExceededException
import com.proje.sosyal.common.ratelimit.RateLimiter
import com.proje.sosyal.media.ImageProcessor
import com.proje.sosyal.media.MediaProperties
import com.proje.sosyal.media.MediaStorage
import com.proje.sosyal.user.dto.MeResponse
import com.proje.sosyal.user.dto.ProfileStats
import com.proje.sosyal.user.dto.UpdateMeRequest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.support.TransactionSynchronization
import org.springframework.transaction.support.TransactionSynchronizationManager
import org.springframework.web.multipart.MultipartFile
import java.time.Clock
import java.time.Duration
import java.util.UUID

@Service
class UserService(
    private val users: UserRepository,
    private val jdbc: JdbcTemplate,
    private val images: ImageProcessor,
    private val storage: MediaStorage,
    mediaProperties: MediaProperties,
    clock: Clock,
) {
    private val uploadLimiter = RateLimiter(mediaProperties.uploadsPerHour, Duration.ofHours(1), clock)

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

    /** Profil fotoğrafını veya kapak görselini değiştirir; eski dosya işlem başarılı olunca silinir. */
    @Transactional
    fun replaceImage(userId: UUID, kind: ImageProcessor.Kind, file: MultipartFile): MeResponse {
        uploadLimiter.tryConsume(userId.toString())?.let { throw RateLimitExceededException(it) }
        if (file.isEmpty) throw ApiException(ErrorCode.UNSUPPORTED_IMAGE)
        val user = load(userId)
        val jpeg = file.inputStream.use { images.process(it, kind) }
        val newKey = storage.save(jpeg)
        val oldKey = when (kind) {
            ImageProcessor.Kind.AVATAR -> user.avatarKey.also { user.avatarKey = newKey }
            ImageProcessor.Kind.BANNER -> user.bannerKey.also { user.bannerKey = newKey }
        }
        afterTransaction(onCommit = { storage.delete(oldKey) }, onRollback = { storage.delete(newKey) })
        return toMe(users.saveAndFlush(user))
    }

    @Transactional
    fun removeImage(userId: UUID, kind: ImageProcessor.Kind): MeResponse {
        val user = load(userId)
        val oldKey = when (kind) {
            ImageProcessor.Kind.AVATAR -> user.avatarKey.also { user.avatarKey = null }
            ImageProcessor.Kind.BANNER -> user.bannerKey.also { user.bannerKey = null }
        }
        afterTransaction(onCommit = { storage.delete(oldKey) })
        return toMe(users.saveAndFlush(user))
    }

    private fun afterTransaction(onCommit: () -> Unit, onRollback: () -> Unit = {}) {
        TransactionSynchronizationManager.registerSynchronization(object : TransactionSynchronization {
            override fun afterCompletion(status: Int) {
                if (status == TransactionSynchronization.STATUS_COMMITTED) onCommit() else onRollback()
            }
        })
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
