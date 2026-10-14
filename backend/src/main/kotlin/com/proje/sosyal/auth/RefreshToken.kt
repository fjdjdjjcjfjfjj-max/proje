package com.proje.sosyal.auth

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "refresh_tokens")
class RefreshToken(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, updatable = false)
    val userId: UUID,

    @Column(nullable = false, updatable = false)
    val familyId: UUID,

    /** SHA-256 (hex). Belirtecin kendisi saklanmaz. */
    @Column(nullable = false, updatable = false, length = 64)
    val tokenHash: String,

    @Column(nullable = false, updatable = false)
    val expiresAt: Instant,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant,

    var revokedAt: Instant? = null,

    var replacedById: UUID? = null,
)
