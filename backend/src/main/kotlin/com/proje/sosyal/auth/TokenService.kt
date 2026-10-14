package com.proje.sosyal.auth

import com.proje.sosyal.user.User
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Instant
import java.util.Base64
import java.util.HexFormat
import java.util.UUID

/** Erişim (JWT) ve yenileme (opak, rastgele) belirteçlerini üretir. */
@Service
class TokenService(
    private val jwtEncoder: JwtEncoder,
    private val refreshTokens: RefreshTokenRepository,
    private val properties: AuthProperties,
    private val clock: Clock,
) {
    private val random = SecureRandom()

    data class IssuedTokens(
        val accessToken: String,
        val accessTokenExpiresAt: Instant,
        val refreshToken: String,
        val refreshTokenExpiresAt: Instant,
        val refreshTokenId: UUID,
    )

    /** Yeni bir oturum (yeni belirteç ailesi) ya da [familyId] verilirse mevcut ailede rotasyon. */
    fun issue(user: User, familyId: UUID = UUID.randomUUID()): IssuedTokens {
        val now = Instant.now(clock)
        val accessExpiresAt = now.plus(properties.accessTokenTtl)
        // Erişim belirtecinde kişisel veri yok: yalnızca kullanıcı kimliği (UUID).
        val claims = JwtClaimsSet.builder()
            .issuer(properties.issuer)
            .subject(user.id.toString())
            .issuedAt(now)
            .expiresAt(accessExpiresAt)
            .id(UUID.randomUUID().toString())
            .build()
        val header = JwsHeader.with(MacAlgorithm.HS256).build()
        val accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).tokenValue

        val rawRefresh = ByteArray(REFRESH_TOKEN_BYTES).also(random::nextBytes)
        val refreshToken = Base64.getUrlEncoder().withoutPadding().encodeToString(rawRefresh)
        val refreshExpiresAt = now.plus(properties.refreshTokenTtl)
        val stored = refreshTokens.save(
            RefreshToken(
                userId = user.id,
                familyId = familyId,
                tokenHash = hash(refreshToken),
                expiresAt = refreshExpiresAt,
                createdAt = now,
            ),
        )
        return IssuedTokens(accessToken, accessExpiresAt, refreshToken, refreshExpiresAt, stored.id)
    }

    companion object {
        private const val REFRESH_TOKEN_BYTES = 32

        fun hash(token: String): String =
            HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8)))
    }
}
