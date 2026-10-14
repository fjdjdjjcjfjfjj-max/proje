package com.proje.sosyal.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class CorsPropertiesTest {

    @Test
    fun `acik origin listesi kabul edilir`() {
        val props = CorsProperties(listOf("https://www.example.com", "http://localhost:3000"))
        assertThat(props.allowedOrigins).hasSize(2)
    }

    @Test
    fun `joker karakter reddedilir`() {
        assertThrows<IllegalArgumentException> { CorsProperties(listOf("*")) }
        assertThrows<IllegalArgumentException> { CorsProperties(listOf("https://*.example.com")) }
    }

    @Test
    fun `gecersiz sema ve sondaki egik cizgi reddedilir`() {
        assertThrows<IllegalArgumentException> { CorsProperties(listOf("example.com")) }
        assertThrows<IllegalArgumentException> { CorsProperties(listOf("https://example.com/")) }
    }
}
