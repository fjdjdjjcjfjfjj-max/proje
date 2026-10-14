package com.proje.sosyal.auth

import com.jayway.jsonpath.JsonPath
import com.proje.sosyal.support.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.ResultActionsDsl
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.LocalDate

@IntegrationTest
class AuthIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val jdbc: JdbcTemplate,
) {

    @BeforeEach
    fun clean() {
        jdbc.execute("ALTER TABLE moderation_actions DISABLE TRIGGER trg_moderation_actions_append_only")
        jdbc.execute("TRUNCATE moderation_actions, reports, user_blocks, post_likes, comments, posts, refresh_tokens, users CASCADE")
        jdbc.execute("ALTER TABLE moderation_actions ENABLE TRIGGER trg_moderation_actions_append_only")
    }

    // ---- Kayıt ----

    @Test
    fun `yetiskin kayit olur, belirtec alir ve me uc noktasina erisir`() {
        val body = register("Ayse.Yilmaz", "ayse@example.com", adultBirthDate()).andExpect {
            status { isCreated() }
            jsonPath("$.tokenType") { value("Bearer") }
            jsonPath("$.expiresIn") { value(900) }
            jsonPath("$.user.username") { value("ayse.yilmaz") } // küçük harfe çevrilir
            jsonPath("$.user.ageGroup") { value("ADULT") }
            jsonPath("$.user.profileVisibility") { value("PUBLIC") }
            jsonPath("$.user.ageVerificationStatus") { value("UNVERIFIED") }
            jsonPath("$.user.role") { value("USER") }
            jsonPath("$.user.passwordHash") { doesNotExist() }
            jsonPath("$.user.adultFrom") { doesNotExist() }
        }.andReturn().response.contentAsString

        mockMvc.get("/api/v1/users/me") { bearer(JsonPath.read(body, "$.accessToken")) }.andExpect {
            status { isOk() }
            jsonPath("$.username") { value("ayse.yilmaz") }
            jsonPath("$.email") { value("ayse@example.com") }
        }
    }

    @Test
    fun `18 yas alti kullanici MINOR olur ve profili gizli baslar`() {
        register("genc_kullanici", "genc@example.com", LocalDate.now().minusYears(15)).andExpect {
            status { isCreated() }
            jsonPath("$.user.ageGroup") { value("MINOR") }
            jsonPath("$.user.profileVisibility") { value("PRIVATE") }
        }
        val adultFrom = jdbc.queryForObject(
            "SELECT adult_from FROM users WHERE username = 'genc_kullanici'", LocalDate::class.java,
        )
        assertThat(adultFrom).isNotNull()
        assertThat(adultFrom!!.dayOfMonth).isEqualTo(1)
    }

    @Test
    fun `13 yas alti kayit reddedilir ve hesap olusmaz`() {
        register("cocuk", "cocuk@example.com", LocalDate.now().minusYears(10)).andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("AGE_NOT_ELIGIBLE") }
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM users", Int::class.java)).isZero()
    }

    @Test
    fun `parola hash'lenerek saklanir, dogum tarihi saklanmaz`() {
        register("mehmet", "mehmet@example.com", adultBirthDate()).andExpect { status { isCreated() } }
        val hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE username = 'mehmet'", String::class.java)
        assertThat(hash).startsWith("{bcrypt}\$2").doesNotContain(PASSWORD)
        val columns = jdbc.queryForList(
            "SELECT column_name FROM information_schema.columns WHERE table_name = 'users'", String::class.java,
        )
        assertThat(columns).noneMatch { it?.contains("birth") == true }
    }

    @Test
    fun `ayni kullanici adi veya e-posta ikinci kez kullanilamaz`() {
        register("zeynep", "zeynep@example.com", adultBirthDate()).andExpect { status { isCreated() } }
        register("ZEYNEP", "baska@example.com", adultBirthDate()).andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("USERNAME_TAKEN") }
        }
        register("zeynep2", "Zeynep@Example.com", adultBirthDate()).andExpect {
            status { isConflict() }
            jsonPath("$.code") { value("EMAIL_TAKEN") }
        }
    }

    @Test
    fun `gecersiz kayit istegi alan hatalariyla reddedilir ve parola geri donmez`() {
        mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"username":"a!","displayName":" ","email":"gecersiz","password":"kisa",
                 "birthDate":"2000-01-01","privacyNoticeAccepted":false}
            """.trimIndent()
        }.andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("VALIDATION_FAILED") }
            jsonPath("$.fieldErrors[*].field") {
                value(org.hamcrest.Matchers.hasItems("username", "displayName", "email", "password", "privacyNoticeAccepted"))
            }
            content { string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("kisa"))) }
        }
    }

    // ---- Giriş ----

    @Test
    fun `kullanici adi veya e-posta ile giris yapilir`() {
        register("ali", "ali@example.com", adultBirthDate())
        login("ALI", PASSWORD).andExpect { status { isOk() }; jsonPath("$.user.username") { value("ali") } }
        login("Ali@Example.com", PASSWORD).andExpect { status { isOk() } }
    }

    @Test
    fun `yanlis parola ve olmayan kullanici ayni yaniti alir`() {
        register("veli", "veli@example.com", adultBirthDate())
        val wrongPassword = login("veli", "YanlisParola123").andExpect { status { isUnauthorized() } }
            .andReturn().response.contentAsString
        val unknownUser = login("olmayan_kisi", "YanlisParola123").andExpect { status { isUnauthorized() } }
            .andReturn().response.contentAsString
        assertThat(JsonPath.read<String>(wrongPassword, "$.code")).isEqualTo("INVALID_CREDENTIALS")
        assertThat(JsonPath.read<String>(unknownUser, "$.message"))
            .isEqualTo(JsonPath.read<String>(wrongPassword, "$.message"))
    }

    @Test
    fun `art arda hatali girislerden sonra hesap gecici olarak kilitlenir`() {
        register("hedef", "hedef@example.com", adultBirthDate())
        repeat(5) { login("hedef", "YanlisParola123").andExpect { status { isUnauthorized() } } }
        // Doğru parola bile kilit süresince kabul edilmez
        login("hedef", PASSWORD).andExpect {
            status { isTooManyRequests() }
            jsonPath("$.code") { value("TOO_MANY_REQUESTS") }
            header { exists(HttpHeaders.RETRY_AFTER) }
        }
    }

    @Test
    fun `askiya alinan kullanici giris yapamaz ve mevcut belirteci gecersiz olur`() {
        val body = register("askida", "askida@example.com", adultBirthDate()).andReturn().response.contentAsString
        jdbc.update("UPDATE users SET status = 'SUSPENDED' WHERE username = 'askida'")

        login("askida", PASSWORD).andExpect {
            status { isForbidden() }
            jsonPath("$.code") { value("ACCOUNT_SUSPENDED") }
        }
        mockMvc.get("/api/v1/users/me") { bearer(JsonPath.read(body, "$.accessToken")) }
            .andExpect { status { isUnauthorized() } }
        refresh(JsonPath.read(body, "$.refreshToken")).andExpect { status { isUnauthorized() } }
    }

    // ---- Belirteçler ----

    @Test
    fun `yenileme belirteci rotasyona ugrar, eski belirtec tekrar kullanilinca aile iptal edilir`() {
        val first = register("rotasyon", "rot@example.com", adultBirthDate()).andReturn().response.contentAsString
        val oldRefresh: String = JsonPath.read(first, "$.refreshToken")

        val second = refresh(oldRefresh).andExpect { status { isOk() } }.andReturn().response.contentAsString
        val newRefresh: String = JsonPath.read(second, "$.refreshToken")
        assertThat(newRefresh).isNotEqualTo(oldRefresh)

        // Çalınmış eski belirteçle deneme -> reddedilir ve aile iptal edilir
        refresh(oldRefresh).andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("INVALID_REFRESH_TOKEN") }
        }
        refresh(newRefresh).andExpect { status { isUnauthorized() } }
    }

    @Test
    fun `cikis yenileme belirtecini iptal eder`() {
        val body = register("cikis", "cikis@example.com", adultBirthDate()).andReturn().response.contentAsString
        val refreshToken: String = JsonPath.read(body, "$.refreshToken")

        mockMvc.post("/api/v1/auth/logout") { json("""{"refreshToken":"$refreshToken"}""") }
            .andExpect { status { isNoContent() } }
        refresh(refreshToken).andExpect { status { isUnauthorized() } }
        // Bilinmeyen belirteçle çıkış da sessizce başarılı
        mockMvc.post("/api/v1/auth/logout") { json("""{"refreshToken":"bilinmeyen"}""") }
            .andExpect { status { isNoContent() } }
    }

    @Test
    fun `yenileme belirteci veritabaninda acik metin olarak saklanmaz`() {
        val body = register("hash", "hash@example.com", adultBirthDate()).andReturn().response.contentAsString
        val refreshToken: String = JsonPath.read(body, "$.refreshToken")
        val stored = jdbc.queryForList("SELECT token_hash FROM refresh_tokens", String::class.java)
        assertThat(stored).hasSize(1).doesNotContain(refreshToken)
        assertThat(stored.single()).isEqualTo(TokenService.hash(refreshToken))
    }

    @Test
    fun `gecersiz veya oynanmis erisim belirteci reddedilir`() {
        val body = register("imza", "imza@example.com", adultBirthDate()).andReturn().response.contentAsString
        val token: String = JsonPath.read(body, "$.accessToken")
        val tampered = token.dropLast(4) + if (token.endsWith("AAAA")) "BBBB" else "AAAA"

        mockMvc.get("/api/v1/users/me") { bearer(tampered) }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.code") { value("UNAUTHORIZED") }
        }
        mockMvc.get("/api/v1/users/me") { bearer("rastgele.metin.degil") }.andExpect { status { isUnauthorized() } }
        mockMvc.get("/api/v1/users/me").andExpect { status { isUnauthorized() } }
    }

    // ---- Yardımcılar ----

    private fun adultBirthDate(): LocalDate = LocalDate.of(1995, 5, 20)

    private fun register(username: String, email: String, birthDate: LocalDate): ResultActionsDsl =
        mockMvc.post("/api/v1/auth/register") {
            json(
                """
                {"username":"$username","displayName":"Test Kullanıcı","email":"$email",
                 "password":"$PASSWORD","birthDate":"$birthDate","privacyNoticeAccepted":true}
                """.trimIndent(),
            )
        }

    private fun login(identifier: String, password: String): ResultActionsDsl =
        mockMvc.post("/api/v1/auth/login") { json("""{"identifier":"$identifier","password":"$password"}""") }

    private fun refresh(token: String): ResultActionsDsl =
        mockMvc.post("/api/v1/auth/refresh") { json("""{"refreshToken":"$token"}""") }

    private fun org.springframework.test.web.servlet.MockHttpServletRequestDsl.json(body: String) {
        contentType = MediaType.APPLICATION_JSON
        content = body
    }

    private fun org.springframework.test.web.servlet.MockHttpServletRequestDsl.bearer(token: String) {
        header(HttpHeaders.AUTHORIZATION, "Bearer $token")
    }

    companion object {
        private const val PASSWORD = "GucluParola123!"
    }
}
