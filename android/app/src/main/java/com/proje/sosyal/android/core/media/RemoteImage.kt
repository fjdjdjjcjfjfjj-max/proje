package com.proje.sosyal.android.core.media

import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.proje.sosyal.android.core.common.IoDispatcher
import com.proje.sosyal.android.core.network.PublicApi
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Profil görselleri için küçük bir yükleyici (yalnızca birkaç görsel olduğu için ayrı bir
 * kütüphane eklenmedi). Belirteç göndermeyen istemci kullanılır; adresler zaten herkese açıktır.
 */
@Singleton
class RemoteImageLoader @Inject constructor(
    @param:PublicApi private val client: OkHttpClient,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    private val cache = object : LruCache<String, ImageBitmap>(16 * 1024 * 1024) {
        override fun sizeOf(key: String, value: ImageBitmap) = value.width * value.height * 4
    }

    suspend fun load(url: String): ImageBitmap? {
        cache.get(url)?.let { return it }
        return withContext(ioDispatcher) {
            runCatching {
                client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                    if (!response.isSuccessful) return@use null
                    val bytes = response.body?.bytes() ?: return@use null
                    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
                }
            }.getOrNull()?.also { cache.put(url, it) }
        }
    }
}

@EntryPoint
@InstallIn(SingletonComponent::class)
interface RemoteImageLoaderEntryPoint {
    fun loader(): RemoteImageLoader
}

/** [url] null ise ya da yüklenemezse [placeholder] gösterilir. */
@Composable
fun RemoteImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    placeholder: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val image by produceState<ImageBitmap?>(initialValue = null, url) {
        value = url?.let {
            EntryPointAccessors.fromApplication(context.applicationContext, RemoteImageLoaderEntryPoint::class.java)
                .loader().load(it)
        }
    }
    val bitmap = image
    if (bitmap != null) {
        Image(bitmap = bitmap, contentDescription = contentDescription, modifier = modifier, contentScale = contentScale)
    } else {
        placeholder()
    }
}
