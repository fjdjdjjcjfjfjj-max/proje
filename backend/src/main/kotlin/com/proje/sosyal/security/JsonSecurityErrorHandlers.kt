package com.proje.sosyal.security

import com.proje.sosyal.common.error.ApiErrorFactory
import com.proje.sosyal.common.error.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.MediaType
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.security.web.access.AccessDeniedHandler
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

/**
 * Güvenlik filtre zincirinde oluşan 401/403 hatalarını, MVC katmanındaki hatalarla
 * aynı [com.proje.sosyal.common.error.ApiError] biçiminde döndürür.
 */
@Component
class JsonSecurityErrorWriter(
    private val errors: ApiErrorFactory,
    private val jsonMapper: JsonMapper,
) {
    fun write(code: ErrorCode, request: HttpServletRequest, response: HttpServletResponse) {
        response.status = code.status.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        response.characterEncoding = Charsets.UTF_8.name()
        jsonMapper.writeValue(response.outputStream, errors.create(code, request.requestURI))
    }
}

@Component
class JsonAuthenticationEntryPoint(private val writer: JsonSecurityErrorWriter) : AuthenticationEntryPoint {
    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException,
    ) = writer.write(ErrorCode.UNAUTHORIZED, request, response)
}

@Component
class JsonAccessDeniedHandler(private val writer: JsonSecurityErrorWriter) : AccessDeniedHandler {
    override fun handle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        accessDeniedException: AccessDeniedException,
    ) = writer.write(ErrorCode.FORBIDDEN, request, response)
}
