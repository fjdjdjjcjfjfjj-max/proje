package com.proje.sosyal.security

import com.proje.sosyal.config.CorsProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import java.time.Duration

/**
 * Varsayılan politika: açıkça izin verilmeyen her uç nokta kimlik doğrulaması ister.
 *
 * - Durumsuz (stateless) API: oturum çerezi yok, kimlik doğrulama Aşama 2'de
 *   Bearer erişim belirteci (JWT) ile eklenecek.
 * - CSRF koruması kapalıdır çünkü kimlik bilgisi çerezle değil Authorization
 *   başlığıyla taşınır; çerez tabanlı oturuma geçilirse yeniden açılmalıdır.
 * - Yönetici/moderatör uç noktaları ayrıca rol kontrolüne tabidir (EnableMethodSecurity).
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        authenticationEntryPoint: JsonAuthenticationEntryPoint,
        accessDeniedHandler: JsonAccessDeniedHandler,
    ): SecurityFilterChain {
        http {
            csrf { disable() }
            cors { }
            httpBasic { disable() }
            formLogin { disable() }
            logout { disable() }
            anonymous { }
            sessionManagement { sessionCreationPolicy = SessionCreationPolicy.STATELESS }
            headers {
                frameOptions { deny = true }
                referrerPolicy { policy = ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER }
            }
            exceptionHandling {
                this.authenticationEntryPoint = authenticationEntryPoint
                this.accessDeniedHandler = accessDeniedHandler
            }
            authorizeHttpRequests {
                authorize(HttpMethod.OPTIONS, "/**", permitAll)
                authorize(HttpMethod.GET, "/api/v1/system/status", permitAll)
                authorize(HttpMethod.GET, "/actuator/health", permitAll)
                authorize(HttpMethod.GET, "/actuator/health/**", permitAll)
                // Yalnızca springdoc etkinse (dev) bu yollar gerçekten cevap verir; prod'da 404 olur.
                authorize("/v3/api-docs/**", permitAll)
                authorize("/swagger-ui/**", permitAll)
                authorize("/swagger-ui.html", permitAll)
                authorize("/error", permitAll)
                authorize(anyRequest, authenticated)
            }
        }
        return http.build()
    }

    @Bean
    fun corsConfigurationSource(properties: CorsProperties): CorsConfigurationSource {
        val config = CorsConfiguration().apply {
            allowedOrigins = properties.allowedOrigins
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("Authorization", "Content-Type", "Accept", "Accept-Language")
            // Belirteç Authorization başlığıyla taşınır; tarayıcı çerezlerine gerek yok.
            allowCredentials = false
            setMaxAge(Duration.ofHours(1))
        }
        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/api/**", config)
        }
    }
}
