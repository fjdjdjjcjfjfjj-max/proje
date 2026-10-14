package com.proje.sosyal.android.core.update

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdatePolicyTest {

    private fun manifest(code: Int = 10, url: String = "https://github.com/sahip/depo/releases/download/android-v10/sosyal.apk", sha: String = "a".repeat(64)) =
        UpdateManifest(versionCode = code, versionName = "0.1.$code", apkUrl = url, sha256 = sha)

    @Test
    fun `yalnizca daha yuksek surum yeni sayilir`() {
        assertTrue(UpdatePolicy.isNewer(manifest(code = 11), installedVersionCode = 10))
        assertFalse(UpdatePolicy.isNewer(manifest(code = 10), installedVersionCode = 10))
        assertFalse(UpdatePolicy.isNewer(manifest(code = 9), installedVersionCode = 10))
    }

    @Test
    fun `yalnizca izinli HTTPS adresi ve gecerli ozet kabul edilir`() {
        assertTrue(UpdatePolicy.isTrustedSource(manifest()))
        assertFalse(UpdatePolicy.isTrustedSource(manifest(url = "http://github.com/x/sosyal.apk")))
        assertFalse(UpdatePolicy.isTrustedSource(manifest(url = "https://kotu-site.example/sosyal.apk")))
        assertFalse(UpdatePolicy.isTrustedSource(manifest(url = "https://github.com.kotu.example/sosyal.apk")))
        assertFalse(UpdatePolicy.isTrustedSource(manifest(sha = "kisa")))
        assertFalse(UpdatePolicy.isTrustedSource(manifest(sha = "A".repeat(64))))
    }
}
