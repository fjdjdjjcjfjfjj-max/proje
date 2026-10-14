package com.proje.sosyal.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * İzin verilen origin'ler ortam değişkeninden okunur (CORS_ALLOWED_ORIGINS, virgülle ayrılmış).
 * Joker karakter (*) bilinçli olarak reddedilir; her origin açıkça listelenmelidir.
 * Android uygulaması CORS'a tabi değildir; bu ayar yalnızca tarayıcı istemcileri (web sitesi) içindir.
 */
@ConfigurationProperties(prefix = "app.cors")
data class CorsProperties(
    val allowedOrigins: List<String> = emptyList(),
) {
    init {
        allowedOrigins.forEach { origin ->
            require(!origin.contains('*')) { "app.cors.allowed-origins joker karakter içeremez" }
            require(origin.startsWith("https://") || origin.startsWith("http://")) {
                "app.cors.allowed-origins yalnızca http(s) origin'leri içerebilir"
            }
            require(!origin.endsWith("/")) { "app.cors.allowed-origins '/' ile bitmemelidir" }
        }
    }
}
