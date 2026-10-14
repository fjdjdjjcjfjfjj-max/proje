package com.proje.sosyal.auth.dto

import com.proje.sosyal.user.dto.MeResponse
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate

// Alanlar nullable tanımlıdır: eksik alan Jackson hatası yerine anlaşılır bir doğrulama hatası üretir.

data class RegisterRequest(
    @field:NotBlank(message = "Kullanıcı adı zorunludur.")
    @field:Pattern(
        regexp = "^[A-Za-z0-9_.]{3,30}$",
        message = "Kullanıcı adı 3-30 karakter olmalı; yalnızca harf, rakam, _ ve . içerebilir.",
    )
    val username: String?,

    @field:NotBlank(message = "Görünen ad zorunludur.")
    @field:Size(max = 50, message = "Görünen ad en fazla 50 karakter olabilir.")
    val displayName: String?,

    @field:NotBlank(message = "E-posta zorunludur.")
    @field:Email(message = "Geçerli bir e-posta adresi girin.")
    @field:Size(max = 254, message = "E-posta çok uzun.")
    val email: String?,

    @field:NotNull(message = "Parola zorunludur.")
    @field:Size(min = 10, max = 64, message = "Parola 10-64 karakter olmalı.")
    val password: String?,

    @field:NotNull(message = "Doğum tarihi zorunludur.")
    val birthDate: LocalDate?,

    /** Aydınlatma metninin okunduğunun beyanı. */
    @field:NotNull(message = "Aydınlatma metnini onaylamanız gerekir.")
    @field:AssertTrue(message = "Aydınlatma metnini onaylamanız gerekir.")
    val privacyNoticeAccepted: Boolean?,
)

data class LoginRequest(
    @field:NotBlank(message = "Kullanıcı adı veya e-posta zorunludur.")
    @field:Size(max = 254)
    val identifier: String?,

    @field:NotBlank(message = "Parola zorunludur.")
    @field:Size(max = 128)
    val password: String?,
)

data class RefreshRequest(
    @field:NotBlank(message = "Yenileme belirteci zorunludur.")
    @field:Size(max = 200)
    val refreshToken: String?,
)

data class AuthResponse(
    val accessToken: String,
    val tokenType: String = "Bearer",
    /** Erişim belirtecinin kalan ömrü (saniye). */
    val expiresIn: Long,
    val refreshToken: String,
    val refreshExpiresIn: Long,
    val user: MeResponse,
)
