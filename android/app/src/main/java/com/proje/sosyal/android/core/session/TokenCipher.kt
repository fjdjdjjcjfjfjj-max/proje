package com.proje.sosyal.android.core.session

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

interface TokenCipher {
    fun encrypt(plain: String): String
    fun decrypt(encoded: String): String
}

/**
 * Oturum belirteçlerini Android Keystore'da tutulan, cihaz dışına çıkarılamayan bir AES-256 anahtarıyla
 * (GCM) şifreler. Uygulama yeniden kurulursa veya anahtar kaybolursa çözme başarısız olur ve
 * kullanıcı yeniden giriş yapar.
 */
@Singleton
class KeystoreTokenCipher @Inject constructor() : TokenCipher {

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    override fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION).apply { init(Cipher.ENCRYPT_MODE, key()) }
        val encrypted = cipher.doFinal(plain.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    override fun decrypt(encoded: String): String {
        val bytes = Base64.decode(encoded, Base64.NO_WRAP)
        val iv = bytes.copyOfRange(0, IV_BYTES)
        val cipher = Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(TAG_BITS, iv))
        }
        return String(cipher.doFinal(bytes, IV_BYTES, bytes.size - IV_BYTES), Charsets.UTF_8)
    }

    private fun key(): SecretKey =
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.secretKey ?: generateKey()

    private fun generateKey(): SecretKey =
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE).apply {
            init(
                KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build(),
            )
        }.generateKey()

    private companion object {
        const val ANDROID_KEYSTORE = "AndroidKeyStore"
        const val KEY_ALIAS = "sosyal_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val IV_BYTES = 12
        const val TAG_BITS = 128
    }
}
