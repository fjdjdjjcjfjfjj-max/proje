package com.proje.sosyal.common.ratelimit

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

class RateLimiterTest {

    private class MutableClock(var now: Instant) : Clock() {
        override fun getZone(): ZoneId = ZoneOffset.UTC
        override fun withZone(zone: ZoneId?): Clock = this
        override fun instant(): Instant = now
    }

    private val clock = MutableClock(Instant.parse("2026-01-01T00:00:00Z"))
    private val limiter = RateLimiter(limit = 3, window = Duration.ofMinutes(1), clock = clock)

    @Test
    fun `sinira kadar izin verir, sonra bekleme suresi doner`() {
        repeat(3) { assertThat(limiter.tryConsume("ip")).isNull() }
        clock.now = clock.now.plusSeconds(20)
        assertThat(limiter.tryConsume("ip")).isEqualTo(40)
        assertThat(limiter.exhaustedFor("ip")).isEqualTo(40)
    }

    @Test
    fun `pencere dolunca sayac sifirlanir`() {
        repeat(3) { limiter.tryConsume("ip") }
        clock.now = clock.now.plus(Duration.ofMinutes(1))
        assertThat(limiter.tryConsume("ip")).isNull()
    }

    @Test
    fun `anahtarlar birbirinden bagimsizdir ve reset sayaci temizler`() {
        repeat(3) { limiter.tryConsume("a") }
        assertThat(limiter.tryConsume("b")).isNull()
        assertThat(limiter.exhaustedFor("a")).isNotNull()
        limiter.reset("a")
        assertThat(limiter.exhaustedFor("a")).isNull()
    }
}
