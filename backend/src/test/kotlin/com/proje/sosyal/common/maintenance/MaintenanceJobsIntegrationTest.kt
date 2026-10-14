package com.proje.sosyal.common.maintenance

import com.proje.sosyal.support.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

@IntegrationTest
class MaintenanceJobsIntegrationTest @Autowired constructor(
    private val jdbc: JdbcTemplate,
) {
    // 2026-11-01 00:30 İstanbul
    private val now = Instant.parse("2026-10-31T21:30:00Z")
    private val jobs by lazy { MaintenanceJobs(jdbc, Clock.fixed(now, ZoneOffset.UTC)) }

    @BeforeEach
    fun clean() {
        jdbc.execute("ALTER TABLE moderation_actions DISABLE TRIGGER trg_moderation_actions_append_only")
        jdbc.execute("TRUNCATE moderation_actions, reports, user_blocks, post_likes, comments, posts, refresh_tokens, users CASCADE")
        jdbc.execute("ALTER TABLE moderation_actions ENABLE TRIGGER trg_moderation_actions_append_only")
    }

    @Test
    fun `yetiskinlige gecis tarihi gelen hesap ADULT olur, gelmeyen MINOR kalir`() {
        insertUser("bugun", "MINOR", "2026-11-01")   // İstanbul'a göre bugün
        insertUser("yarin", "MINOR", "2026-12-01")
        insertUser("yetiskin", "ADULT", null)

        assertThat(jobs.promoteMinorsWhoTurnedAdult()).isEqualTo(1)

        assertThat(ageGroup("bugun")).isEqualTo("ADULT")
        assertThat(ageGroup("yarin")).isEqualTo("MINOR")
        // Gizlilik ayarı otomatik gevşetilmez
        assertThat(jdbc.queryForObject("SELECT profile_visibility FROM users WHERE username = 'bugun'", String::class.java))
            .isEqualTo("PRIVATE")
        // İkinci çalıştırma bir şey değiştirmez
        assertThat(jobs.promoteMinorsWhoTurnedAdult()).isZero()
    }

    @Test
    fun `yalnizca suresi dolmus oturum kayitlari silinir, iptal edilmis gecerliler kalir`() {
        val user = insertUser("oturum", "ADULT", null)
        val family = UUID.randomUUID()
        insertToken(user, family, expiresAt = now.minusSeconds(60), revoked = false)              // süresi dolmuş
        insertToken(user, family, expiresAt = now.plusSeconds(3600), revoked = true)             // iptal, süresi dolmamış
        insertToken(user, family, expiresAt = now.plusSeconds(3600), revoked = false)            // geçerli

        assertThat(jobs.deleteExpiredRefreshTokens()).isEqualTo(1)
        assertThat(jdbc.queryForObject("SELECT count(*) FROM refresh_tokens", Int::class.java)).isEqualTo(2)
    }

    private fun insertUser(username: String, ageGroup: String, adultFrom: String?): UUID {
        val id = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO users (id, username, display_name, email, password_hash, age_group, adult_from) " +
                "VALUES (?, ?, ?, ?, 'x', ?, CAST(? AS DATE))",
            id, username, username, "$username@example.com", ageGroup, adultFrom,
        )
        return id
    }

    private fun insertToken(userId: UUID, family: UUID, expiresAt: Instant, revoked: Boolean) {
        jdbc.update(
            "INSERT INTO refresh_tokens (user_id, family_id, token_hash, expires_at, created_at, revoked_at) VALUES (?, ?, ?, ?, ?, ?)",
            userId, family, UUID.randomUUID().toString().replace("-", "").padEnd(64, '0'),
            Timestamp.from(expiresAt), Timestamp.from(expiresAt.minusSeconds(86_400)),
            if (revoked) Timestamp.from(now.minusSeconds(10)) else null,
        )
    }

    private fun ageGroup(username: String): String =
        jdbc.queryForObject("SELECT age_group FROM users WHERE username = ?", String::class.java, username)!!
}
