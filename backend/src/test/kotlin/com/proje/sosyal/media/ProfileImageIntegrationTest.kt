package com.proje.sosyal.media

import com.jayway.jsonpath.JsonPath
import com.proje.sosyal.support.IntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.mock.web.MockMultipartFile
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.delete
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.multipart
import org.springframework.test.web.servlet.post
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

@IntegrationTest
class ProfileImageIntegrationTest @Autowired constructor(
    private val mockMvc: MockMvc,
    private val jdbc: JdbcTemplate,
    private val storage: MediaStorage,
) {
    @BeforeEach
    fun clean() {
        jdbc.execute("ALTER TABLE moderation_actions DISABLE TRIGGER trg_moderation_actions_append_only")
        jdbc.execute("TRUNCATE moderation_actions, reports, user_blocks, post_likes, comments, posts, refresh_tokens, users CASCADE")
        jdbc.execute("ALTER TABLE moderation_actions ENABLE TRIGGER trg_moderation_actions_append_only")
    }

    @Test
    fun `profil fotografi kare JPEG'e donusturulur ve konum bilgisi silinir`() {
        val token = register("foto")
        val upload = jpegWithExif(width = 1200, height = 800)
        assertThat(String(upload, Charsets.ISO_8859_1)).contains("Exif").contains("GPSKONUM")

        val body = uploadImage(token, "avatar", upload).andExpect {
            status { isOk() }
            jsonPath("$.avatarUrl") { value(org.hamcrest.Matchers.startsWith("/api/v1/media/")) }
        }.andReturn().response.contentAsString
        val url: String = JsonPath.read(body, "$.avatarUrl")

        val served = mockMvc.get(url).andExpect {
            status { isOk() }
            content { contentType(MediaType.IMAGE_JPEG) }
            header { string(HttpHeaders.CACHE_CONTROL, org.hamcrest.Matchers.containsString("immutable")) }
        }.andReturn().response.contentAsByteArray

        val image = ImageIO.read(ByteArrayInputStream(served))
        assertThat(image.width).isEqualTo(512)
        assertThat(image.height).isEqualTo(512)
        val raw = String(served, Charsets.ISO_8859_1)
        assertThat(raw).doesNotContain("Exif").doesNotContain("GPSKONUM")
    }

    @Test
    fun `kapak gorseli 3e1 oranina kirpilir`() {
        val token = register("kapak")
        val body = uploadImage(token, "banner", png(2000, 2000)).andExpect { status { isOk() } }
            .andReturn().response.contentAsString
        val served = mockMvc.get(JsonPath.read<String>(body, "$.bannerUrl")).andReturn().response.contentAsByteArray
        val image = ImageIO.read(ByteArrayInputStream(served))
        assertThat(image.width).isEqualTo(1500)
        assertThat(image.height).isEqualTo(500)
    }

    @Test
    fun `resim olmayan dosya reddedilir`() {
        val token = register("sahte")
        uploadImage(token, "avatar", "<script>alert(1)</script>".toByteArray(), contentType = "image/jpeg").andExpect {
            status { isBadRequest() }
            jsonPath("$.code") { value("UNSUPPORTED_IMAGE") }
        }
    }

    @Test
    fun `yeni fotograf yuklenince eskisi silinir, kaldirinca dosya da silinir`() {
        val token = register("degistir")
        val first = JsonPath.read<String>(
            uploadImage(token, "avatar", png(600, 600)).andReturn().response.contentAsString, "$.avatarUrl",
        )
        val second = JsonPath.read<String>(
            uploadImage(token, "avatar", png(700, 700)).andReturn().response.contentAsString, "$.avatarUrl",
        )
        assertThat(second).isNotEqualTo(first)
        mockMvc.get(first).andExpect { status { isNotFound() } }

        mockMvc.delete("/api/v1/users/me/avatar") { bearer(token) }.andExpect {
            status { isOk() }
            jsonPath("$.avatarUrl") { doesNotExist() }
        }
        mockMvc.get(second).andExpect { status { isNotFound() } }
    }

    @Test
    fun `gecersiz medya anahtari ve kimliksiz yukleme reddedilir`() {
        // Kodlanmış "../" içeren adresi güvenlik duvarı daha erken (400) reddeder; her durumda dosya verilmez.
        mockMvc.get("/api/v1/media/..%2F..%2Fetc%2Fpasswd").andExpect { status { is4xxClientError() } }
        mockMvc.get("/api/v1/media/olmayan.jpg").andExpect { status { isNotFound() } }
        assertThat(storage.read("../../etc/passwd")).isNull()

        mockMvc.multipart(HttpMethod.PUT, "/api/v1/users/me/avatar") {
            file(MockMultipartFile("file", "a.png", "image/png", png(100, 100)))
        }.andExpect { status { isUnauthorized() } }
    }

    // ---- Yardımcılar ----

    private fun uploadImage(token: String, kind: String, bytes: ByteArray, contentType: String = "image/png") =
        mockMvc.multipart(HttpMethod.PUT, "/api/v1/users/me/$kind") {
            file(MockMultipartFile("file", "resim", contentType, bytes))
            bearer(token)
        }

    private fun png(width: Int, height: Int): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        image.createGraphics().apply { color = Color(30, 60, 120); fillRect(0, 0, width, height); dispose() }
        return ByteArrayOutputStream().also { ImageIO.write(image, "png", it) }.toByteArray()
    }

    /** SOI'den hemen sonra, içinde "GPSKONUM" geçen sahte bir EXIF (APP1) bölümü eklenmiş JPEG. */
    private fun jpegWithExif(width: Int, height: Int): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        image.createGraphics().apply { color = Color.ORANGE; fillRect(0, 0, width, height); dispose() }
        val jpeg = ByteArrayOutputStream().also { ImageIO.write(image, "jpeg", it) }.toByteArray()
        val payload = "Exif\u0000\u0000GPSKONUM:41.0082,28.9784".toByteArray(Charsets.ISO_8859_1)
        val length = payload.size + 2
        val app1 = byteArrayOf(0xFF.toByte(), 0xE1.toByte(), (length shr 8).toByte(), (length and 0xFF).toByte()) + payload
        return jpeg.copyOfRange(0, 2) + app1 + jpeg.copyOfRange(2, jpeg.size)
    }

    private fun register(username: String): String {
        val body = mockMvc.post("/api/v1/auth/register") {
            contentType = MediaType.APPLICATION_JSON
            content = """
                {"username":"$username","displayName":"Test","email":"$username@example.com",
                 "password":"GucluParola123!","birthDate":"1995-01-01","privacyNoticeAccepted":true}
            """.trimIndent()
        }.andReturn().response.contentAsString
        return JsonPath.read(body, "$.accessToken")
    }

    private fun org.springframework.test.web.servlet.MockHttpServletRequestDsl.bearer(token: String) {
        header(HttpHeaders.AUTHORIZATION, "Bearer $token")
    }

    private fun org.springframework.test.web.servlet.MockMultipartHttpServletRequestDsl.bearer(token: String) {
        header(HttpHeaders.AUTHORIZATION, "Bearer $token")
    }
}
