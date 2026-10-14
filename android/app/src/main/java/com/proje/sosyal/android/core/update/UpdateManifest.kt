package com.proje.sosyal.android.core.update

import kotlinx.serialization.Serializable
import java.net.URI

/** CI'ın her sürümle birlikte yayınladığı `version.json`. */
@Serializable
data class UpdateManifest(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    /** APK'nın SHA-256 özeti (küçük harf hex). İndirilen dosya bununla doğrulanır. */
    val sha256: String,
)

object UpdatePolicy {
    /** Güncelleme dosyası yalnızca bu adreslerden (HTTPS) indirilir. */
    private val ALLOWED_HOSTS = setOf("github.com")
    private val SHA256_HEX = Regex("^[0-9a-f]{64}$")

    fun isNewer(manifest: UpdateManifest, installedVersionCode: Int): Boolean =
        manifest.versionCode > installedVersionCode

    fun isTrustedSource(manifest: UpdateManifest): Boolean {
        val uri = runCatching { URI(manifest.apkUrl) }.getOrNull() ?: return false
        return uri.scheme == "https" && uri.host in ALLOWED_HOSTS && SHA256_HEX.matches(manifest.sha256)
    }
}
