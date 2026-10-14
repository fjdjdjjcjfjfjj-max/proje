package com.proje.sosyal.auth

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "app.auth")
data class AuthProperties(
    /** Base64 kodlu HMAC anahtarı (en az 32 bayt). Yalnızca ortam değişkeninden (JWT_SECRET) verilir. */
    val jwtSecret: String = "",
    /**
     * true ise anahtar verilmediğinde her açılışta rastgele bir anahtar üretilir (yalnızca dev/test).
     * Uygulama yeniden başlayınca tüm erişim belirteçleri geçersiz olur.
     */
    val allowEphemeralSecret: Boolean = false,
    val issuer: String = "sosyal-api",
    val accessTokenTtl: Duration = Duration.ofMinutes(15),
    val refreshTokenTtl: Duration = Duration.ofDays(30),
    /** Kayıt/giriş/yenileme uç noktaları için IP başına istek sınırı. */
    val ipLimit: Int = 30,
    val ipWindow: Duration = Duration.ofMinutes(1),
    /** Aynı hesap için art arda hatalı giriş sınırı (kaba kuvvet koruması). */
    val maxFailedLogins: Int = 5,
    val failedLoginWindow: Duration = Duration.ofMinutes(15),
)
