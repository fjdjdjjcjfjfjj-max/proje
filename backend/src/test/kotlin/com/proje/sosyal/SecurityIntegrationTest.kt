package com.proje.sosyal

import com.proje.sosyal.support.IntegrationTest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.options
import org.springframework.test.web.servlet.post

@IntegrationTest
class SecurityIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
) {

    @Test
    fun `sistem durumu kimlik dogrulamasiz erisilebilir`() {
        mockMvc.get("/api/v1/system/status").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("UP") }
            jsonPath("$.apiVersion") { value("v1") }
            jsonPath("$.serverTime") { exists() }
        }
    }

    @Test
    fun `saglik kontrolu veritabani dahil UP doner ve detay sizdirmaz`() {
        mockMvc.get("/actuator/health").andExpect {
            status { isOk() }
            jsonPath("$.status") { value("UP") }
            jsonPath("$.components") { doesNotExist() }
        }
    }

    @Test
    fun `korunan uc nokta kimlik dogrulamasiz 401 ve standart hata govdesi doner`() {
        mockMvc.get("/api/v1/posts").andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("UNAUTHORIZED") }
            jsonPath("$.status") { value(401) }
            jsonPath("$.path") { value("/api/v1/posts") }
            header { doesNotExist(HttpHeaders.SET_COOKIE) }
        }
    }

    @Test
    fun `actuator yalnizca health ve info acar`() {
        mockMvc.get("/actuator/env").andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `guvenlik basliklari eklenir`() {
        mockMvc.get("/api/v1/system/status").andExpect {
            header { string("X-Frame-Options", "DENY") }
            header { string("X-Content-Type-Options", "nosniff") }
            header { string("Referrer-Policy", "no-referrer") }
        }
    }

    @Test
    fun `izin verilen origin icin CORS on istegi kabul edilir`() {
        mockMvc.options("/api/v1/system/status") {
            header(HttpHeaders.ORIGIN, "http://localhost:3000")
            header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        }.andExpect {
            status { isOk() }
            header { string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, "http://localhost:3000") }
            header { doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS) }
        }
    }

    @Test
    fun `izin verilmeyen origin icin CORS reddedilir`() {
        mockMvc.options("/api/v1/system/status") {
            header(HttpHeaders.ORIGIN, "https://kotu-site.example")
            header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
        }.andExpect {
            status { isForbidden() }
            header { doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN) }
        }
    }

    @Test
    fun `yalnizca GET ile acik uc noktaya POST kimlik dogrulamasi ister`() {
        mockMvc.post("/api/v1/system/status").andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `OpenAPI belgesi uretilir`() {
        mockMvc.get("/v3/api-docs").andExpect {
            status { isOk() }
            jsonPath("$.info.title") { value("Sosyal Platform API") }
            jsonPath("$.paths['/api/v1/system/status']") { exists() }
        }
    }
}
