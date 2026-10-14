package com.proje.sosyal.android.core.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import com.proje.sosyal.android.BuildConfig
import com.proje.sosyal.android.core.common.IoDispatcher
import com.proje.sosyal.android.core.network.PublicApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Play Store dışı dağıtım için uygulama içi güncelleme.
 *
 * Güvenlik: Android, yeni APK'yı ancak yüklü uygulamayla aynı anahtarla imzalanmışsa kurar;
 * bu yüzden adres ele geçirilse bile sahte bir sürüm mevcut uygulamanın yerine geçemez.
 * Ek olarak dosya SHA-256 ile doğrulanır ve yalnızca izinli HTTPS adreslerinden indirilir.
 * Oturum belirteci içermeyen ([PublicApi]) istemci kullanılır.
 */
@Singleton
class AppUpdater @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:PublicApi private val client: OkHttpClient,
    private val json: Json,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) {
    val isEnabled: Boolean get() = BuildConfig.UPDATE_MANIFEST_URL.isNotBlank()

    /** Yeni ve güvenilir bir sürüm varsa döner; yoksa ya da kontrol başarısızsa null. */
    suspend fun checkForUpdate(): UpdateManifest? = withContext(ioDispatcher) {
        if (!isEnabled) return@withContext null
        runCatching {
            client.newCall(Request.Builder().url(BuildConfig.UPDATE_MANIFEST_URL).build()).execute().use { response ->
                if (!response.isSuccessful) return@use null
                json.decodeFromString(UpdateManifest.serializer(), response.body?.string() ?: return@use null)
            }
        }.getOrNull()?.takeIf {
            UpdatePolicy.isTrustedSource(it) && UpdatePolicy.isNewer(it, BuildConfig.VERSION_CODE)
        }
    }

    /** APK'yı indirir ve özetini doğrular. [onProgress] 0..1 arası değer alır (boyut bilinmiyorsa -1). */
    suspend fun download(manifest: UpdateManifest, onProgress: (Float) -> Unit): File = withContext(ioDispatcher) {
        val dir = File(context.cacheDir, "updates").apply { mkdirs() }
        dir.listFiles()?.forEach { it.delete() } // eski indirmeleri temizle
        val target = File(dir, "sosyal-${manifest.versionCode}.apk")
        val digest = MessageDigest.getInstance("SHA-256")

        client.newCall(Request.Builder().url(manifest.apkUrl).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("İndirme başarısız: HTTP ${response.code}")
            val body = response.body ?: throw IOException("Boş yanıt")
            val total = body.contentLength()
            var read = 0L
            body.byteStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        digest.update(buffer, 0, n)
                        read += n
                        onProgress(if (total > 0) read.toFloat() / total else -1f)
                    }
                }
            }
        }
        // java.util.HexFormat Android 14+ gerektirir; minSdk 26 için elle çevrilir.
        val actual = digest.digest().joinToString("") { "%02x".format(it) }
        if (actual != manifest.sha256) {
            target.delete()
            throw IOException("Güncelleme dosyası doğrulanamadı")
        }
        target
    }

    /** Android 8+: kullanıcı bu uygulamaya "bilinmeyen uygulamaları yükleme" izni vermiş mi? */
    fun canInstall(): Boolean = context.packageManager.canRequestPackageInstalls()

    fun installPermissionIntent(): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))

    fun installIntent(apk: File): Intent {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.updates", apk)
        return Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
}
