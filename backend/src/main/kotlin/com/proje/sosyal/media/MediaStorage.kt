package com.proje.sosyal.media

import org.springframework.stereotype.Component
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.UUID

/** Yerel diskte medya deposu. İleride S3 uyumlu bir depoya taşınabilecek şekilde tek noktada toplanır. */
@Component
class MediaStorage(properties: MediaProperties) {

    private val root: Path = Path.of(properties.dir).toAbsolutePath().normalize().also { Files.createDirectories(it) }

    fun save(jpeg: ByteArray): String {
        val key = "${UUID.randomUUID()}.jpg"
        val temp = Files.createTempFile(root, "upload-", ".tmp")
        try {
            Files.write(temp, jpeg)
            Files.move(temp, resolve(key), StandardCopyOption.ATOMIC_MOVE)
        } finally {
            Files.deleteIfExists(temp)
        }
        return key
    }

    fun read(key: String): ByteArray? = resolveOrNull(key)?.takeIf { Files.isRegularFile(it) }?.let(Files::readAllBytes)

    fun delete(key: String?) {
        key?.let(::resolveOrNull)?.let(Files::deleteIfExists)
    }

    private fun resolveOrNull(key: String): Path? = if (KEY.matches(key)) resolve(key) else null

    /** Yalnızca biçimi doğrulanmış anahtarlar çözülür: dizin dışına çıkma (path traversal) mümkün değildir. */
    private fun resolve(key: String): Path {
        require(KEY.matches(key)) { "Geçersiz medya anahtarı" }
        return root.resolve(key).normalize().also { check(it.parent == root) }
    }

    companion object {
        val KEY = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.jpg$")

        fun publicUrl(key: String?): String? = key?.let { "/api/v1/media/$it" }
    }
}
