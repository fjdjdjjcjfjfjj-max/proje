package com.proje.sosyal.auth

import com.proje.sosyal.auth.dto.AuthResponse
import com.proje.sosyal.auth.dto.LoginRequest
import com.proje.sosyal.auth.dto.RegisterRequest
import com.proje.sosyal.common.error.ApiException
import com.proje.sosyal.common.error.ErrorCode
import com.proje.sosyal.common.error.RateLimitExceededException
import com.proje.sosyal.common.ratelimit.RateLimiter
import com.proje.sosyal.user.AgeGroup
import com.proje.sosyal.user.ProfileVisibility
import com.proje.sosyal.user.User
import com.proje.sosyal.user.UserRepository
import com.proje.sosyal.user.UserStatus
import com.proje.sosyal.user.dto.MeResponse
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.Locale

@Service
class AuthService(
    private val users: UserRepository,
    private val refreshTokens: RefreshTokenRepository,
    private val tokens: TokenService,
    private val passwordEncoder: PasswordEncoder,
    private val agePolicy: AgePolicy,
    private val properties: AuthProperties,
    private val clock: Clock,
) {
    private val ipLimiter = RateLimiter(properties.ipLimit, properties.ipWindow, clock)
    private val failedLoginLimiter = RateLimiter(properties.maxFailedLogins, properties.failedLoginWindow, clock)

    /** Kullanıcı bulunamadığında da parola karşılaştırması yapılır; yanıt süresi hesabın varlığını ele vermez. */
    private val dummyPasswordHash: String by lazy { passwordEncoder.encode("timing-equalizer-not-a-real-password")!! }

    @Transactional
    fun register(request: RegisterRequest, clientIp: String): AuthResponse {
        enforceIpLimit(clientIp)
        val username = normalizeUsername(request.username!!)
        val email = request.email!!.trim()
        val displayName = request.displayName!!.trim()
        val password = request.password!!
        if (displayName.isEmpty()) throw ApiException(ErrorCode.VALIDATION_FAILED, "Görünen ad zorunludur.")
        if (password.toByteArray(Charsets.UTF_8).size > BCRYPT_MAX_BYTES) {
            throw ApiException(ErrorCode.VALIDATION_FAILED, "Parola çok uzun.")
        }

        val age = agePolicy.assess(request.birthDate!!)

        if (users.existsByUsername(username)) throw ApiException(ErrorCode.USERNAME_TAKEN)
        if (users.existsByEmailIgnoreCase(email)) throw ApiException(ErrorCode.EMAIL_TAKEN)

        val user = User(
            username = username,
            displayName = displayName,
            email = email,
            passwordHash = passwordEncoder.encode(password)!!,
            ageGroup = age.ageGroup,
            adultFrom = age.adultFrom,
            // 18 yaş altı için profil varsayılan olarak gizli.
            profileVisibility = if (age.ageGroup == AgeGroup.MINOR) ProfileVisibility.PRIVATE else ProfileVisibility.PUBLIC,
            createdAt = Instant.now(clock),
        )
        try {
            users.saveAndFlush(user)
        } catch (e: DataIntegrityViolationException) {
            // Eşzamanlı iki kayıt isteğinde ön kontrol yarışı kaybedilebilir; benzersizlik kısıtı son sözü söyler.
            val constraint = e.mostSpecificCause.message.orEmpty()
            throw when {
                "ux_users_username" in constraint -> ApiException(ErrorCode.USERNAME_TAKEN)
                "ux_users_email_lower" in constraint -> ApiException(ErrorCode.EMAIL_TAKEN)
                else -> e
            }
        }
        return toResponse(user, tokens.issue(user))
    }

    @Transactional
    fun login(request: LoginRequest, clientIp: String): AuthResponse {
        enforceIpLimit(clientIp)
        val identifier = request.identifier!!.trim()
        val accountKey = identifier.lowercase(Locale.ROOT)
        failedLoginLimiter.exhaustedFor(accountKey)?.let { throw RateLimitExceededException(it) }

        val user = if ('@' in identifier) {
            users.findByEmailIgnoreCase(identifier)
        } else {
            users.findByUsername(normalizeUsername(identifier))
        }
        val passwordMatches = passwordEncoder.matches(request.password!!, user?.passwordHash ?: dummyPasswordHash)

        if (user == null || !passwordMatches || user.status == UserStatus.DELETED) {
            failedLoginLimiter.tryConsume(accountKey)
            throw ApiException(ErrorCode.INVALID_CREDENTIALS)
        }
        failedLoginLimiter.reset(accountKey)
        // Askıya alma bilgisi yalnızca doğru parola girildikten sonra verilir.
        if (user.status == UserStatus.SUSPENDED) throw ApiException(ErrorCode.ACCOUNT_SUSPENDED)

        return toResponse(user, tokens.issue(user))
    }

    /**
     * Yenileme belirteci rotasyonu. Daha önce kullanılmış (iptal edilmiş) bir belirteç gelirse bu,
     * belirtecin çalındığına işaret eder: ailenin tamamı iptal edilir ve kullanıcı yeniden giriş yapmalıdır.
     * İptal işlemi istisnaya rağmen kalıcı olmalıdır (noRollbackFor).
     */
    @Transactional(noRollbackFor = [ApiException::class])
    fun refresh(rawRefreshToken: String, clientIp: String): AuthResponse {
        enforceIpLimit(clientIp)
        val now = Instant.now(clock)
        val stored = refreshTokens.findForUpdate(TokenService.hash(rawRefreshToken))
            ?: throw ApiException(ErrorCode.INVALID_REFRESH_TOKEN)

        if (stored.revokedAt != null) {
            refreshTokens.revokeFamily(stored.familyId, now)
            throw ApiException(ErrorCode.INVALID_REFRESH_TOKEN)
        }
        if (!now.isBefore(stored.expiresAt)) throw ApiException(ErrorCode.INVALID_REFRESH_TOKEN)

        val user = users.findById(stored.userId).orElse(null)
        if (user == null || !user.isActive) {
            refreshTokens.revokeFamily(stored.familyId, now)
            throw ApiException(ErrorCode.INVALID_REFRESH_TOKEN)
        }

        val issued = tokens.issue(user, stored.familyId)
        stored.revokedAt = now
        stored.replacedById = issued.refreshTokenId
        return toResponse(user, issued)
    }

    /** Oturumu (belirteç ailesini) kapatır. Belirteç bilinmiyorsa da sessizce başarılı döner. */
    @Transactional
    fun logout(rawRefreshToken: String) {
        val stored = refreshTokens.findForUpdate(TokenService.hash(rawRefreshToken)) ?: return
        refreshTokens.revokeFamily(stored.familyId, Instant.now(clock))
    }

    private fun enforceIpLimit(clientIp: String) {
        ipLimiter.tryConsume(clientIp)?.let { throw RateLimitExceededException(it) }
    }

    private fun toResponse(user: User, issued: TokenService.IssuedTokens) = AuthResponse(
        accessToken = issued.accessToken,
        expiresIn = properties.accessTokenTtl.seconds,
        refreshToken = issued.refreshToken,
        refreshExpiresIn = properties.refreshTokenTtl.seconds,
        user = MeResponse.from(user),
    )

    companion object {
        /** BCrypt yalnızca ilk 72 baytı kullanır; daha uzun parolalar reddedilir. */
        private const val BCRYPT_MAX_BYTES = 72

        fun normalizeUsername(raw: String): String = raw.trim().lowercase(Locale.ROOT)
    }
}
