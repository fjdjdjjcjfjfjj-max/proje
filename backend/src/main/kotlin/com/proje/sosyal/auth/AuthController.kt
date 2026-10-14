package com.proje.sosyal.auth

import com.proje.sosyal.auth.dto.AuthResponse
import com.proje.sosyal.auth.dto.LoginRequest
import com.proje.sosyal.auth.dto.RefreshRequest
import com.proje.sosyal.auth.dto.RegisterRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Kimlik doğrulama")
class AuthController(private val auth: AuthService) {

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Yeni hesap oluşturur ve oturum açar")
    fun register(@Valid @RequestBody request: RegisterRequest, http: HttpServletRequest): AuthResponse =
        auth.register(request, http.remoteAddr)

    @PostMapping("/login")
    @Operation(summary = "Kullanıcı adı veya e-posta ile giriş")
    fun login(@Valid @RequestBody request: LoginRequest, http: HttpServletRequest): AuthResponse =
        auth.login(request, http.remoteAddr)

    @PostMapping("/refresh")
    @Operation(summary = "Yenileme belirteciyle yeni belirteç çifti alır (rotasyon)")
    fun refresh(@Valid @RequestBody request: RefreshRequest, http: HttpServletRequest): AuthResponse =
        auth.refresh(request.refreshToken!!, http.remoteAddr)

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Oturumu kapatır (yenileme belirteci ailesini iptal eder)")
    fun logout(@Valid @RequestBody request: RefreshRequest) =
        auth.logout(request.refreshToken!!)
}
