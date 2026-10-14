package com.proje.sosyal.auth

import com.proje.sosyal.common.error.ApiException
import com.proje.sosyal.common.error.ErrorCode
import com.proje.sosyal.user.AgeGroup
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class AgePolicyTest {

    // 2026-10-14 12:00 UTC = 15:00 İstanbul
    private val policy = AgePolicy(Clock.fixed(Instant.parse("2026-10-14T12:00:00Z"), ZoneOffset.UTC))

    @Test
    fun `18 yasini doldurmus kullanici ADULT olur`() {
        val result = policy.assess(LocalDate.of(2008, 10, 14))
        assertThat(result.ageGroup).isEqualTo(AgeGroup.ADULT)
        assertThat(result.adultFrom).isNull()
    }

    @Test
    fun `18 yasina bir gun kalan kullanici MINOR olur ve gecis ayi izleyen ayin ilk gunudur`() {
        val result = policy.assess(LocalDate.of(2008, 10, 15))
        assertThat(result.ageGroup).isEqualTo(AgeGroup.MINOR)
        // 18. yaş günü 2026-10-15 -> geçiş 2026-11-01 (asla erken değil)
        assertThat(result.adultFrom).isEqualTo(LocalDate.of(2026, 11, 1))
    }

    @Test
    fun `ayin ilk gunu dogan kullanicinin gecisi de erken olmaz`() {
        val result = policy.assess(LocalDate.of(2010, 3, 1))
        assertThat(result.adultFrom).isEqualTo(LocalDate.of(2028, 4, 1))
        assertThat(result.adultFrom).isAfterOrEqualTo(LocalDate.of(2028, 3, 1))
    }

    @Test
    fun `13 yasini dolduran kayit olabilir, dolduramayan reddedilir`() {
        assertThat(policy.assess(LocalDate.of(2013, 10, 14)).ageGroup).isEqualTo(AgeGroup.MINOR)
        val ex = assertThrows<ApiException> { policy.assess(LocalDate.of(2013, 10, 15)) }
        assertThat(ex.errorCode).isEqualTo(ErrorCode.AGE_NOT_ELIGIBLE)
    }

    @Test
    fun `gelecekteki veya gercek disi dogum tarihi reddedilir`() {
        assertThrows<ApiException> { policy.assess(LocalDate.of(2027, 1, 1)) }
        assertThrows<ApiException> { policy.assess(LocalDate.of(1890, 1, 1)) }
    }
}
