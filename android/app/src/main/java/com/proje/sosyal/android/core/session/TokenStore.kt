package com.proje.sosyal.android.core.session

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

interface TokenStore {
    suspend fun read(): AuthTokens?
    suspend fun write(tokens: AuthTokens)
    suspend fun clear()
}

private val Context.sessionDataStore by preferencesDataStore(name = "session")

/** Belirteçler DataStore'da yalnızca şifreli olarak durur (bkz. [KeystoreTokenCipher]). */
@Singleton
class EncryptedTokenStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val cipher: TokenCipher,
) : TokenStore {

    @Serializable
    private data class Stored(val accessToken: String, val refreshToken: String)

    override suspend fun read(): AuthTokens? {
        val encrypted = context.sessionDataStore.data.first()[KEY] ?: return null
        return runCatching {
            val stored = Json.decodeFromString(Stored.serializer(), cipher.decrypt(encrypted))
            AuthTokens(stored.accessToken, stored.refreshToken)
        }.getOrElse {
            // Anahtar kaybolmuş veya veri bozulmuş: güvenli tarafta kal, oturumu sil.
            clear()
            null
        }
    }

    override suspend fun write(tokens: AuthTokens) {
        val encrypted = cipher.encrypt(Json.encodeToString(Stored.serializer(), Stored(tokens.accessToken, tokens.refreshToken)))
        context.sessionDataStore.edit { it[KEY] = encrypted }
    }

    override suspend fun clear() {
        context.sessionDataStore.edit { it.remove(KEY) }
    }

    private companion object {
        val KEY = stringPreferencesKey("tokens")
    }
}
