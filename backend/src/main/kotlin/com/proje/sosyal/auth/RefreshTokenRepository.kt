package com.proje.sosyal.auth

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.Instant
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {

    /** Satır kilitlenir: aynı belirteçle eşzamanlı iki yenileme isteği iki ayrı oturum üretemez. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshToken t where t.tokenHash = :tokenHash")
    fun findForUpdate(tokenHash: String): RefreshToken?

    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now where t.familyId = :familyId and t.revokedAt is null")
    fun revokeFamily(familyId: UUID, now: Instant): Int

    @Modifying
    @Query("update RefreshToken t set t.revokedAt = :now where t.userId = :userId and t.revokedAt is null")
    fun revokeAllForUser(userId: UUID, now: Instant): Int
}
