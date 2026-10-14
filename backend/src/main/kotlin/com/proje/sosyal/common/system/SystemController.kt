package com.proje.sosyal.common.system

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Clock
import java.time.Instant

/**
 * İstemcilerin (Android/web) sunucuya ulaşabildiğini ve API sürümünü doğrulaması için
 * kimlik doğrulaması gerektirmeyen uç nokta. Altyapı/DB detayı içermez.
 */
@RestController
@RequestMapping("/api/v1/system")
@Tag(name = "Sistem")
class SystemController(private val clock: Clock) {

    @GetMapping("/status")
    @Operation(summary = "API erişilebilirlik durumu")
    fun status(): SystemStatusResponse =
        SystemStatusResponse(status = "UP", apiVersion = API_VERSION, serverTime = Instant.now(clock))

    companion object {
        const val API_VERSION = "v1"
    }
}

data class SystemStatusResponse(
    val status: String,
    val apiVersion: String,
    val serverTime: Instant,
)
