package com.proje.sosyal.user

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.PreUpdate
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Kullanıcı hesabı. Bu sınıf API yanıtlarında doğrudan kullanılmaz;
 * dışarıya yalnızca DTO'lar (ör. [com.proje.sosyal.user.dto.MeResponse]) açılır.
 */
@Entity
@Table(name = "users")
class User(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(nullable = false, length = 30)
    var username: String,

    @Column(nullable = false, length = 50)
    var displayName: String,

    @Column(nullable = false, length = 254)
    var email: String,

    @Column(nullable = false)
    var passwordHash: String,

    @Column(length = 300)
    var bio: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var role: Role = Role.USER,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: UserStatus = UserStatus.ACTIVE,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var ageGroup: AgeGroup,

    /** Yalnızca MINOR için: yetişkin grubuna geçiş tarihi (ay başı). */
    var adultFrom: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var ageVerificationStatus: AgeVerificationStatus = AgeVerificationStatus.UNVERIFIED,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var profileVisibility: ProfileVisibility,

    @Column(nullable = false, updatable = false)
    val createdAt: Instant = Instant.now(),

    @Column(nullable = false)
    var updatedAt: Instant = createdAt,

    var deletedAt: Instant? = null,

    @Version
    var version: Long? = null,
) {
    val isActive: Boolean get() = status == UserStatus.ACTIVE

    @PreUpdate
    fun touch() {
        updatedAt = Instant.now()
    }
}
