package com.proje.sosyal.security

import com.nimbusds.jose.jwk.source.ImmutableSecret
import com.proje.sosyal.auth.AuthProperties
import com.proje.sosyal.user.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec

@Configuration(proxyBeanMethods = false)
class JwtConfig {

    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun jwtSigningKey(properties: AuthProperties): SecretKey {
        val secret = properties.jwtSecret.trim()
        val bytes = when {
            secret.isNotEmpty() -> runCatching { Base64.getDecoder().decode(secret) }.getOrElse {
                throw IllegalStateException("JWT_SECRET geçerli bir Base64 değeri olmalı")
            }
            properties.allowEphemeralSecret -> {
                log.warn("JWT_SECRET tanımlı değil; geçici rastgele anahtar kullanılıyor (yalnızca geliştirme).")
                ByteArray(MIN_KEY_BYTES).also { SecureRandom().nextBytes(it) }
            }
            else -> throw IllegalStateException("JWT_SECRET ortam değişkeni zorunludur")
        }
        check(bytes.size >= MIN_KEY_BYTES) { "JWT_SECRET en az $MIN_KEY_BYTES bayt olmalı" }
        return SecretKeySpec(bytes, "HmacSHA256")
    }

    @Bean
    fun jwtEncoder(key: SecretKey): JwtEncoder = NimbusJwtEncoder(ImmutableSecret(key))

    @Bean
    fun jwtDecoder(key: SecretKey, properties: AuthProperties): JwtDecoder =
        NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build().apply {
            setJwtValidator(DelegatingOAuth2TokenValidator(JwtValidators.createDefaultWithIssuer(properties.issuer)))
        }

    /**
     * Belirteç imzası geçerli olsa bile kullanıcı her istekte veritabanından kontrol edilir:
     * askıya alınan veya silinen hesabın belirteçleri hemen geçersiz olur ve rol güncel kalır.
     */
    @Bean
    fun jwtAuthenticationConverter(users: UserRepository): Converter<Jwt, UserAuthentication> =
        Converter { jwt ->
            val userId = runCatching { UUID.fromString(jwt.subject) }.getOrNull()
                ?: throw InvalidBearerTokenException("Geçersiz belirteç")
            val user = users.findById(userId).orElse(null)
            if (user == null || !user.isActive) throw InvalidBearerTokenException("Geçersiz belirteç")
            UserAuthentication(user.id, user.role)
        }

    companion object {
        private const val MIN_KEY_BYTES = 32
    }
}
