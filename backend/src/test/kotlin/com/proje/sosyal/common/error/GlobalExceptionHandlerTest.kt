package com.proje.sosyal.common.error

import io.mockk.every
import io.mockk.mockk
import jakarta.servlet.http.HttpServletRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.dao.DataIntegrityViolationException
import java.sql.SQLException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class GlobalExceptionHandlerTest {

    private val clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC)
    private val handler = GlobalExceptionHandler(ApiErrorFactory(clock))
    private val request = mockk<HttpServletRequest> { every { requestURI } returns "/api/v1/test" }

    @Test
    fun `beklenmeyen hata ic detaylari istemciye sizdirmaz`() {
        val response = handler.handleUnexpected(IllegalStateException("db parola=gizli kullanici@example.com"), request)

        assertThat(response.statusCode.value()).isEqualTo(500)
        val body = response.body!!
        assertThat(body.code).isEqualTo("INTERNAL_ERROR")
        assertThat(body.message).isEqualTo(ErrorCode.INTERNAL_ERROR.defaultMessage)
        assertThat(body.toString()).doesNotContain("gizli").doesNotContain("example.com")
        assertThat(body.timestamp).isEqualTo(Instant.parse("2026-01-01T00:00:00Z"))
    }

    @Test
    fun `kisit ihlali 409 doner ve kisisel veri icermez`() {
        val cause = SQLException("Key (email)=(kullanici@example.com) already exists")
        val response = handler.handleIntegrity(DataIntegrityViolationException("duplicate", cause), request)

        assertThat(response.statusCode.value()).isEqualTo(409)
        assertThat(response.body!!.code).isEqualTo("CONFLICT")
        assertThat(response.body.toString()).doesNotContain("kullanici@example.com")
    }

    @Test
    fun `is kurali hatasi kendi kodu ve mesajiyla doner`() {
        val response = handler.handleApi(ApiException(ErrorCode.FORBIDDEN, "Bu içeriği göremezsiniz."), request)

        assertThat(response.statusCode.value()).isEqualTo(403)
        assertThat(response.body!!.code).isEqualTo("FORBIDDEN")
        assertThat(response.body!!.message).isEqualTo("Bu içeriği göremezsiniz.")
        assertThat(response.body!!.path).isEqualTo("/api/v1/test")
    }
}
