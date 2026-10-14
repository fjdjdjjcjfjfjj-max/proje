package com.proje.sosyal.common.maintenance

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Gece çalışan bakım işleri. Sorgular idempotenttir; birden fazla backend örneği aynı anda
 * çalıştırsa da sonuç değişmez. Loglara kişisel veri değil, yalnızca etkilenen satır sayısı yazılır.
 */
@Component
class MaintenanceJobs(
    private val jdbc: JdbcTemplate,
    private val clock: Clock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Yetişkinliğe geçiş tarihi gelen 18 yaş altı hesapları ADULT grubuna taşır (docs/age-safety.md).
     * Gizlilik ayarları bilinçli olarak değiştirilmez; profil gizli kalır, kullanıcı isterse açar.
     */
    @Scheduled(cron = "0 5 0 * * *", zone = "Europe/Istanbul")
    @Transactional
    fun promoteMinorsWhoTurnedAdult(): Int {
        val today = LocalDate.now(clock.withZone(ISTANBUL))
        val updated = jdbc.update(
            "UPDATE users SET age_group = 'ADULT', adult_from = NULL WHERE age_group = 'MINOR' AND adult_from <= ?",
            today,
        )
        if (updated > 0) log.info("Yetişkin grubuna geçirilen hesap sayısı: {}", updated)
        return updated
    }

    /**
     * Süresi dolmuş yenileme belirteçlerini siler. İptal edilmiş ama süresi dolmamış kayıtlar
     * bilerek tutulur: çalınmış bir belirtecin tekrar kullanımı ancak böyle tespit edilebilir.
     */
    @Scheduled(cron = "0 20 3 * * *", zone = "Europe/Istanbul")
    @Transactional
    fun deleteExpiredRefreshTokens(): Int {
        // replaced_by_id öz-referansı ON DELETE SET NULL olduğu için zincir sırası sorun olmaz.
        val deleted = jdbc.update(
            "DELETE FROM refresh_tokens WHERE expires_at < ?",
            Timestamp.from(Instant.now(clock)),
        )
        if (deleted > 0) log.info("Silinen süresi dolmuş oturum kaydı: {}", deleted)
        return deleted
    }

    private companion object {
        val ISTANBUL: ZoneId = ZoneId.of("Europe/Istanbul")
    }
}
