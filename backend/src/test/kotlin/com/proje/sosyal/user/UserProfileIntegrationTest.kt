package com.proje.sosyal.user

import com.jayway.jsonpath.JsonPath
import com.proje.sosyal.support.IntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.patch
import org.springframework.test.web.servlet.post
import java.time.LocalDate
import java.util.UUID

@IntegrationTest
class UserProfileIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val jdbc: JdbcTemplate,
) {

    @BeforeEach
    fun clean() {
        jdbc.execute("ALTER TABLE moderation_actions DISABLE TRIGGER trg_moderation_actions_append_only")
        jdbc.execute("TRUNCATE moderation_actions, reports, user_blocks, post_likes, comments, posts, refresh_tokens, users CASCADE")
        jdbc.execute("ALTER TABLE moderation_actions ENABLE TRIGGER trg_moderation_actions_append_only")
    }

    @Test
    fun `profil sayaclari yalnizca yayindaki gonderilerden hesaplanir`() {
        val token = register("sayac", LocalDate.of(1995, 1, 1))
        mockMvc.get("/api/v1/users/me") { bearer(token) }.andExpect {
            jsonPath("$.stats.postCount") { value(0) }
            jsonPath("$.stats.likesReceived") { value(0) }
        }

        val userId = jdbc.queryForObject("SELECT id FROM users WHERE username = 'sayac'", UUID::class.java)
        jdbc.update("INSERT INTO posts (author_id, content, like_count) VALUES (?, 'bir', 5)", userId)
        jdbc.update("INSERT INTO posts (author_id, content, like_count) VALUES (?, 'iki', 3)", userId)
        jdbc.update("INSERT INTO posts (author_id, content, like_count, status) VALUES (?, 'gizli', 100, 'HIDDEN')", userId)

        mockMvc.get("/api/v1/users/me") { bearer(token) }.andExpect {
            jsonPath("$.stats.postCount") { value(2) }
            jsonPath("$.stats.likesReceived") { value(8) }
        }
    }

    @Test
    fun `gorunen ad ve biyografi guncellenir, bos biyografi silinir`() {
        val token = register("duzenle", LocalDate.of(1995, 1, 1))

        patch(token, """{"displayName":"  Yeni Ad  ","bio":"Merhaba, ben yazılımcıyım."}""").andExpect {
            status { isOk() }
            jsonPath("$.displayName") { value("Yeni Ad") }
            jsonPath("$.bio") { value("Merhaba, ben yazılımcıyım.") }
        }
        // Gönderilmeyen alan değişmez
        patch(token, """{"bio":"Sadece bio"}""").andExpect { jsonPath("$.displayName") { value("Yeni Ad") } }
        patch(token, """{"bio":"   "}""").andExpect { jsonPath("$.bio") { doesNotExist() } }
    }

    @Test
    fun `gecersiz guncellemeler reddedilir`() {
        val token = register("gecersiz", LocalDate.of(1995, 1, 1))
        patch(token, """{"displayName":"   "}""").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("VALIDATION_FAILED") }
        }
        patch(token, """{"bio":"${"a".repeat(301)}"}""").andExpect {
            status { isBadRequest() }
            jsonPath("$.fieldErrors[0].field") { value("bio") }
        }
    }

    @Test
    fun `istekteki rol ve yas alanlari yok sayilir`() {
        val token = register("yetkisiz", LocalDate.of(1995, 1, 1))
        patch(token, """{"role":"ADMIN","ageGroup":"ADULT","ageVerificationStatus":"VERIFIED","bio":"x"}""").andExpect {
            status { isOk() }
            jsonPath("$.role") { value("USER") }
            jsonPath("$.ageVerificationStatus") { value("UNVERIFIED") }
        }
    }

    @Test
    fun `18 yas alti kullanici profilini herkese acamaz, yetiskin acabilir`() {
        val minorToken = register("genc", LocalDate.now().minusYears(15))
        patch(minorToken, """{"profileVisibility":"PUBLIC"}""").andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("PROFILE_VISIBILITY_NOT_ALLOWED") }
        }

        val adultToken = register("yetiskin", LocalDate.of(1990, 1, 1))
        patch(adultToken, """{"profileVisibility":"PRIVATE"}""").andExpect {
            jsonPath("$.profileVisibility") { value("PRIVATE") }
        }
        patch(adultToken, """{"profileVisibility":"PUBLIC"}""").andExpect {
            jsonPath("$.profileVisibility") { value("PUBLIC") }
        }
    }

    @Test
    fun `kimlik dogrulamasiz guncelleme reddedilir`() {
        mockMvc.patch("/api/v1/users/me") {
            contentType = MediaType.APPLICATION_JSON
            content = """{"bio":"x"}"""
        }.andExpect { status { isUnauthorized() } }
    }

    private fun register(username: String, birthDate: LocalDate): String {
        val body = mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"username":"$username","displayName":"Test","email":"$username@example.com",
                 "password":"GucluParola123!","birthDate":"$birthDate","privacyNoticeAccepted":true}
            """.trimIndent()
        }.andExpect { status { isCreated() } }.andReturn().response.contentAsString
        return JsonPath.read(body, "$.accessToken")
    }

    private fun patch(token: String, json: String) =
        mockMvc.patch("/api/v1/users/me") {
            bearer(token)
            contentType = MediaType.APPLICATION_JSON
            content = json
        }

    private fun org.springframework.test.web.servlet.MockHttpServletRequestDsl.bearer(token: String) {
        header(HttpHeaders.AUTHORIZATION, "Bearer $token")
    }
}
