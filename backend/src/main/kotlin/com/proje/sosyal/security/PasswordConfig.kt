package com.proje.sosyal.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.DelegatingPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder

@Configuration(proxyBeanMethods = false)
class PasswordConfig {
    /**
     * BCrypt (maliyet 12). Hash'ler "{bcrypt}" önekiyle saklanır; ileride algoritma değiştirilirse
     * eski hash'ler doğrulanmaya devam eder.
     */
    @Bean
    fun passwordEncoder(): PasswordEncoder =
        DelegatingPasswordEncoder(BCRYPT, mapOf(BCRYPT to BCryptPasswordEncoder(BCRYPT_STRENGTH)))

    companion object {
        private const val BCRYPT = "bcrypt"
        private const val BCRYPT_STRENGTH = 12
    }
}
