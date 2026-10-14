package com.proje.sosyal.common.ratelimit

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Anahtar başına sabit pencereli, bellek içi sayaç.
 *
 * Tek backend örneği için yeterlidir. Birden fazla örneğe geçildiğinde sayaçlar
 * paylaşılmadığından Redis/PostgreSQL destekli bir uygulamayla değiştirilmelidir.
 * Anahtarlar (IP, kullanıcı adı) loglanmaz.
 */
class RateLimiter(
    private val limit: Int,
    private val window: Duration,
    private val clock: Clock,
    private val maxKeys: Int = 100_000,
) {
    init {
        require(limit > 0) { "limit pozitif olmalı" }
    }

    private class Counter(val windowStart: Instant, val count: Int)

    private val counters = ConcurrentHashMap<String, Counter>()

    /** Bir hak tüketir. Sınır aşılmışsa tüketmez ve kalan bekleme süresini (saniye) döner. */
    fun tryConsume(key: String): Long? {
        evictIfNeeded()
        val now = Instant.now(clock)
        var retryAfter: Long? = null
        counters.compute(key) { _, current ->
            val active = current?.takeIf { now.isBefore(it.windowStart.plus(window)) }
            when {
                active == null -> Counter(now, 1)
                active.count >= limit -> {
                    retryAfter = secondsUntilReset(active, now)
                    active
                }
                else -> Counter(active.windowStart, active.count + 1)
            }
        }
        return retryAfter
    }

    /** Hak tüketmeden sınırın dolup dolmadığını kontrol eder; doluysa bekleme süresini döner. */
    fun exhaustedFor(key: String): Long? {
        val now = Instant.now(clock)
        val current = counters[key] ?: return null
        if (!now.isBefore(current.windowStart.plus(window))) return null
        return if (current.count >= limit) secondsUntilReset(current, now) else null
    }

    fun reset(key: String) {
        counters.remove(key)
    }

    private fun secondsUntilReset(counter: Counter, now: Instant): Long =
        Duration.between(now, counter.windowStart.plus(window)).seconds.coerceAtLeast(1)

    private fun evictIfNeeded() {
        if (counters.size < maxKeys) return
        val now = Instant.now(clock)
        counters.entries.removeIf { !now.isBefore(it.value.windowStart.plus(window)) }
    }
}
