package com.proje.sosyal.media

import com.proje.sosyal.common.error.ApiException
import com.proje.sosyal.common.error.ErrorCode
import org.springframework.stereotype.Component
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO
import javax.imageio.ImageWriteParam
import javax.imageio.stream.ImageInputStream

/**
 * Yüklenen dosyayı güvenilmeyen veri olarak ele alır:
 *  - Uzantıya/Content-Type'a değil, dosyanın gerçekten çözülebilen bir resim olmasına bakılır.
 *  - Çözmeden önce boyutlar okunur; aşırı büyük ("decompression bomb") dosyalar reddedilir.
 *  - Resim yeniden çizilip JPEG olarak kodlanır: EXIF/GPS konumu dahil tüm üst veri atılır,
 *    dosyaya gömülü olabilecek başka içerik de taşınmaz.
 */
@Component
class ImageProcessor {

    enum class Kind(val width: Int, val height: Int) {
        AVATAR(512, 512),
        BANNER(1500, 500),
    }

    fun process(input: InputStream, kind: Kind): ByteArray {
        val source = ImageIO.createImageInputStream(input) ?: throw unsupported()
        val image = source.use { decodeSafely(it) }
        val cropped = cropToAspect(image, kind.width.toDouble() / kind.height)
        val scaled = scale(cropped, kind.width, kind.height)
        return encodeJpeg(scaled)
    }

    private fun decodeSafely(stream: ImageInputStream): BufferedImage {
        val reader = ImageIO.getImageReaders(stream).asSequence().firstOrNull() ?: throw unsupported()
        try {
            if (reader.formatName.lowercase() !in ALLOWED_FORMATS) throw unsupported()
            reader.setInput(stream, true, true)
            val width = reader.getWidth(0)
            val height = reader.getHeight(0)
            if (width < MIN_SIDE || height < MIN_SIDE || width.toLong() * height > MAX_PIXELS) throw unsupported()
            return reader.read(0) ?: throw unsupported()
        } catch (e: ApiException) {
            throw e
        } catch (e: Exception) {
            throw unsupported()
        } finally {
            reader.dispose()
        }
    }

    private fun cropToAspect(image: BufferedImage, aspect: Double): BufferedImage {
        val current = image.width.toDouble() / image.height
        return if (current > aspect) {
            val w = (image.height * aspect).toInt()
            image.getSubimage((image.width - w) / 2, 0, w, image.height)
        } else {
            val h = (image.width / aspect).toInt()
            image.getSubimage(0, (image.height - h) / 2, image.width, h)
        }
    }

    private fun scale(image: BufferedImage, width: Int, height: Int): BufferedImage {
        // Küçük resimler büyütülmez; yalnızca hedef boyuta kadar küçültülür.
        val targetW = minOf(width, image.width)
        val targetH = minOf(height, image.height)
        val out = BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_RGB)
        val g = out.createGraphics()
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
            g.drawImage(image, 0, 0, targetW, targetH, java.awt.Color.WHITE, null)
        } finally {
            g.dispose()
        }
        return out
    }

    private fun encodeJpeg(image: BufferedImage): ByteArray {
        val writer = ImageIO.getImageWritersByFormatName("jpeg").next()
        val bytes = ByteArrayOutputStream()
        try {
            ImageIO.createImageOutputStream(bytes).use { output ->
                writer.output = output
                val params = writer.defaultWriteParam.apply {
                    compressionMode = ImageWriteParam.MODE_EXPLICIT
                    compressionQuality = JPEG_QUALITY
                }
                writer.write(null, IIOImage(image, null, null), params)
            }
        } finally {
            writer.dispose()
        }
        return bytes.toByteArray()
    }

    private fun unsupported() = ApiException(ErrorCode.UNSUPPORTED_IMAGE)

    private companion object {
        val ALLOWED_FORMATS = setOf("jpeg", "jpg", "png")
        const val MIN_SIDE = 16
        const val MAX_PIXELS = 40_000_000L
        const val JPEG_QUALITY = 0.85f
    }
}
