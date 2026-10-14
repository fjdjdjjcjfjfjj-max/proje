package com.proje.sosyal.security

import com.proje.sosyal.user.Role
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import java.util.UUID

/**
 * Doğrulanmış istek kimliği. Controller'larda `@AuthenticationPrincipal userId: UUID` ile alınır.
 * Rol, belirteçten değil her istekte veritabanından okunur.
 */
class UserAuthentication(
    private val userId: UUID,
    val role: Role,
) : AbstractAuthenticationToken(listOf(SimpleGrantedAuthority("ROLE_${role.name}"))) {
    init {
        isAuthenticated = true
    }

    override fun getPrincipal(): UUID = userId

    override fun getCredentials(): Any? = null

    override fun getName(): String = userId.toString()
}
