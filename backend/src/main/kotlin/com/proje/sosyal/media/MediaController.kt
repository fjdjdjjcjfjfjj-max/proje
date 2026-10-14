package com.proje.sosyal.media

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.CacheControl
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration

/**
 * Profil görsellerini sunar. Anahtarlar rastgele UUID olduğundan tahmin edilemez; dosya içeriği
 * değişmez (yeni yükleme = yeni anahtar), bu yüzden uzun süre önbelleğe alınabilir.
 */
@RestController
@RequestMapping("/api/v1/media")
@Tag(name = "Medya")
class MediaController(private val storage: MediaStorage) {

    @GetMapping("/{key}")
    @Operation(summary = "Profil fotoğrafı veya kapak görseli")
    fun get(@PathVariable key: String): ResponseEntity<ByteArray> {
        val bytes = storage.read(key) ?: return ResponseEntity.notFound().build()
        return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .cacheControl(CacheControl.maxAge(Duration.ofDays(365)).cachePublic().immutable())
            .header("X-Content-Type-Options", "nosniff")
            .body(bytes)
    }
}
