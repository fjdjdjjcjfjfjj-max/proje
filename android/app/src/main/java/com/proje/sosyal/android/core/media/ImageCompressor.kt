package com.proje.sosyal.android.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import com.proje.sosyal.android.core.common.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import kotlin.math.max

/**
 * Seçilen fotoğrafı yüklemeden önce cihazda küçültür ve JPEG'e çevirir: hem mobil veriyi hem
 * sunucu sınırını (8 MB) korur. Konum gibi üst veriler yeni JPEG'e taşınmaz; sunucu da ayrıca siler.
 */
class ImageCompressor @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    suspend fun toJpeg(uri: Uri, maxSide: Int = 2048): ByteArray = withContext(ioDispatcher) {
        val bitmap = decode(uri, maxSide)
        try {
            ByteArrayOutputStream().use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
                out.toByteArray()
            }
        } finally {
            bitmap.recycle()
        }
    }

    private fun decode(uri: Uri, maxSide: Int): Bitmap =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder EXIF yönünü uygular (yan dönmüş fotoğraf sorunu olmaz).
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                val scale = maxSide.toFloat() / max(info.size.width, info.size.height)
                if (scale < 1f) decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, bounds) }
            var sample = 1
            while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
            val options = BitmapFactory.Options().apply { inSampleSize = sample }
            context.contentResolver.openInputStream(uri).use { BitmapFactory.decodeStream(it, null, options) }
                ?: throw IllegalArgumentException("Fotoğraf okunamadı")
        }
}
