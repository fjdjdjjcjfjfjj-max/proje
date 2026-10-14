package com.proje.sosyal.auth

import com.proje.sosyal.common.error.ApiException
import com.proje.sosyal.common.error.ErrorCode
import com.proje.sosyal.user.AgeGroup
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.time.Period
import java.time.ZoneId

/**
 * Doğum tarihinden yaş grubunu hesaplar. Doğum tarihi yalnızca bu hesaplama için kullanılır
 * ve saklanmaz (bkz. docs/age-safety.md).
 */
@Component
class AgePolicy(private val clock: Clock) {

    data class Assessment(val ageGroup: AgeGroup, val adultFrom: LocalDate?)

    fun assess(birthDate: LocalDate): Assessment {
        val today = LocalDate.now(clock.withZone(ZONE))
        if (birthDate.isAfter(today) || birthDate.isBefore(today.minusYears(MAX_AGE.toLong()))) {
            throw ApiException(ErrorCode.VALIDATION_FAILED, "Geçerli bir doğum tarihi girin.")
        }
        val age = Period.between(birthDate, today).years
        if (age < MIN_AGE) {
            throw ApiException(ErrorCode.AGE_NOT_ELIGIBLE)
        }
        if (age >= ADULT_AGE) {
            return Assessment(AgeGroup.ADULT, adultFrom = null)
        }
        // 18. yaş gününün bulunduğu ayı izleyen ayın ilk günü: gün bilgisi saklanmaz, geçiş asla erken olmaz.
        val adultFrom = birthDate.plusYears(ADULT_AGE.toLong()).withDayOfMonth(1).plusMonths(1)
        return Assessment(AgeGroup.MINOR, adultFrom)
    }

    companion object {
        /** Asgari yaş. Yürürlükteki mevzuata göre hukukçu ile teyit edilmelidir. */
        const val MIN_AGE = 13
        const val ADULT_AGE = 18
        private const val MAX_AGE = 120
        private val ZONE: ZoneId = ZoneId.of("Europe/Istanbul")
    }
}
