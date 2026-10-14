package com.proje.sosyal

import com.proje.sosyal.support.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataAccessException
import org.springframework.jdbc.core.JdbcTemplate
import java.util.UUID

/**
 * V1 şemasının gerçek PostgreSQL üzerinde uygulandığını ve veri bütünlüğü
 * kurallarının veritabanı seviyesinde zorlandığını doğrular.
 */
@IntegrationTest
class FlywayMigrationIntegrationTest @Autowired constructor(
    private val jdbc: JdbcTemplate,
) {

    @BeforeEach
    fun cleanDatabase() {
        // moderation_actions yalnızca ekleme yapılabilir; test temizliği için tetikleyici geçici olarak devre dışı.
        jdbc.execute("ALTER TABLE moderation_actions DISABLE TRIGGER trg_moderation_actions_append_only")
        jdbc.execute("TRUNCATE moderation_actions, reports, user_blocks, post_likes, comments, posts, users CASCADE")
        jdbc.execute("ALTER TABLE moderation_actions ENABLE TRIGGER trg_moderation_actions_append_only")
    }

    @Test
    fun `V1 migrasyonu basariyla uygulanir`() {
        val success = jdbc.queryForObject(
            "SELECT success FROM flyway_schema_history WHERE version = '1'",
            Boolean::class.java,
        )
        assertThat(success).isTrue()

        val tables = jdbc.queryForList(
            "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'",
            String::class.java,
        )
        assertThat(tables).contains(
            "users", "posts", "comments", "post_likes", "user_blocks", "reports", "moderation_actions",
        )
    }

    @Test
    fun `kullanici adi ve e-posta buyuk kucuk harf duyarsiz benzersizdir`() {
        insertUser("ayse", "ayse@example.com")

        assertThrows<DataAccessException> { insertUser("ayse", "baska@example.com") }
        assertThrows<DataAccessException> { insertUser("ayse2", "AYSE@example.com") }
        // Kullanıcı adı küçük harf olarak saklanmalıdır
        assertThrows<DataAccessException> { insertUser("Ayse3", "ayse3@example.com") }
    }

    @Test
    fun `MINOR kullanicida adult_from zorunlu, ADULT kullanicida yasaktir`() {
        assertThrows<DataAccessException> { insertUser("cocuk", "c@example.com", ageGroup = "MINOR", adultFrom = null) }
        assertThrows<DataAccessException> {
            insertUser("yetiskin", "y@example.com", ageGroup = "ADULT", adultFrom = "2030-01-01")
        }
        insertUser("cocuk", "c@example.com", ageGroup = "MINOR", adultFrom = "2030-01-01")
    }

    @Test
    fun `yeni kullanicinin profili varsayilan olarak gizli ve yasi dogrulanmamistir`() {
        val id = insertUser("veli", "veli@example.com")
        val row = jdbc.queryForMap(
            "SELECT profile_visibility, age_verification_status, role FROM users WHERE id = ?", id,
        )
        assertThat(row["profile_visibility"]).isEqualTo("PRIVATE")
        assertThat(row["age_verification_status"]).isEqualTo("UNVERIFIED")
        assertThat(row["role"]).isEqualTo("USER")
    }

    @Test
    fun `updated_at guncellemede otomatik yenilenir`() {
        val id = insertUser("zeynep", "z@example.com")
        jdbc.update("UPDATE users SET updated_at = now() - interval '1 day' WHERE id = ?", id)
        jdbc.update("UPDATE users SET display_name = 'Zeynep Y' WHERE id = ?", id)
        val recent = jdbc.queryForObject(
            "SELECT updated_at > now() - interval '1 minute' FROM users WHERE id = ?", Boolean::class.java, id,
        )
        assertThat(recent).isTrue()
    }

    @Test
    fun `bos gonderi ve ayni gonderiyi iki kez begenme reddedilir`() {
        val user = insertUser("mehmet", "m@example.com")
        assertThrows<DataAccessException> { insertPost(user, "   ") }

        val post = insertPost(user, "Merhaba dünya")
        jdbc.update("INSERT INTO post_likes (user_id, post_id) VALUES (?, ?)", user, post)
        assertThrows<DataAccessException> {
            jdbc.update("INSERT INTO post_likes (user_id, post_id) VALUES (?, ?)", user, post)
        }
    }

    @Test
    fun `kullanici kendini engelleyemez`() {
        val user = insertUser("ali", "ali@example.com")
        assertThrows<DataAccessException> {
            jdbc.update("INSERT INTO user_blocks (blocker_id, blocked_id) VALUES (?, ?)", user, user)
        }
    }

    @Test
    fun `sikayet tam olarak bir hedef icermeli ve acik sikayet tekrarlanamaz`() {
        val reporter = insertUser("raporcu", "r@example.com")
        val author = insertUser("yazar", "yazar@example.com")
        val post = insertPost(author, "İçerik")

        assertThrows<DataAccessException> {
            jdbc.update(
                "INSERT INTO reports (reporter_id, target_type, reason) VALUES (?, 'POST', 'SPAM')", reporter,
            )
        }
        assertThrows<DataAccessException> {
            jdbc.update(
                "INSERT INTO reports (reporter_id, target_type, reported_post_id, reported_user_id, reason) " +
                    "VALUES (?, 'POST', ?, ?, 'SPAM')",
                reporter, post, author,
            )
        }

        insertPostReport(reporter, post)
        assertThrows<DataAccessException> { insertPostReport(reporter, post) }
    }

    @Test
    fun `moderasyon denetim kaydi degistirilemez ve silinemez`() {
        val moderator = insertUser("moderator", "mod@example.com", role = "MODERATOR")
        val author = insertUser("yazar", "yazar@example.com")
        val post = insertPost(author, "Kural dışı içerik")
        val actionId = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO moderation_actions (id, moderator_id, action_type, target_type, target_id, reason) " +
                "VALUES (?, ?, 'HIDE_POST', 'POST', ?, 'Topluluk kuralları ihlali')",
            actionId, moderator, post,
        )

        assertThrows<DataAccessException> {
            jdbc.update("UPDATE moderation_actions SET reason = 'değiştirildi' WHERE id = ?", actionId)
        }
        assertThrows<DataAccessException> {
            jdbc.update("DELETE FROM moderation_actions WHERE id = ?", actionId)
        }
        // İşlem yapmış moderatör, denetim kaydı varken fiziksel olarak silinemez
        assertThrows<DataAccessException> { jdbc.update("DELETE FROM users WHERE id = ?", moderator) }
    }

    @Test
    fun `sikayete bagli moderasyon kaydi, sikayet silinince korunur`() {
        val moderator = insertUser("moderator", "mod@example.com", role = "MODERATOR")
        val reporter = insertUser("raporcu", "r@example.com")
        val author = insertUser("yazar", "yazar@example.com")
        val post = insertPost(author, "İçerik")
        val report = insertPostReport(reporter, post)
        jdbc.update(
            "INSERT INTO moderation_actions (moderator_id, action_type, target_type, target_id, report_id, reason) " +
                "VALUES (?, 'DISMISS_REPORT', 'REPORT', ?, ?, 'Kural ihlali yok')",
            moderator, report, report,
        )

        jdbc.update("DELETE FROM reports WHERE id = ?", report)

        val count = jdbc.queryForObject(
            "SELECT count(*) FROM moderation_actions WHERE target_id = ? AND report_id IS NULL",
            Int::class.java, report,
        )
        assertThat(count).isEqualTo(1)
    }

    private fun insertUser(
        username: String,
        email: String,
        ageGroup: String = "ADULT",
        adultFrom: String? = null,
        role: String = "USER",
    ): UUID {
        val id = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO users (id, username, display_name, email, password_hash, age_group, adult_from, role) " +
                "VALUES (?, ?, ?, ?, ?, ?, CAST(? AS DATE), ?)",
            id, username, username, email, "{bcrypt}placeholder-hash", ageGroup, adultFrom, role,
        )
        return id
    }

    private fun insertPost(authorId: UUID, content: String): UUID {
        val id = UUID.randomUUID()
        jdbc.update("INSERT INTO posts (id, author_id, content) VALUES (?, ?, ?)", id, authorId, content)
        return id
    }

    private fun insertPostReport(reporterId: UUID, postId: UUID): UUID {
        val id = UUID.randomUUID()
        jdbc.update(
            "INSERT INTO reports (id, reporter_id, target_type, reported_post_id, reason) VALUES (?, ?, 'POST', ?, 'SPAM')",
            id, reporterId, postId,
        )
        return id
    }
}
