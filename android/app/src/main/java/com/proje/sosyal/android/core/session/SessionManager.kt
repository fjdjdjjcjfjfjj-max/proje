package com.proje.sosyal.android.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Oturumun tek doğruluk kaynağı. Navigasyon [state]'i izler: oturum kapanınca (çıkış veya
 * yenileme belirteci geçersiz) kullanıcı otomatik olarak giriş ekranına döner.
 */
@Singleton
class SessionManager @Inject constructor(
    private val store: TokenStore,
) {
    private val mutex = Mutex()
    private val _state = MutableStateFlow<SessionState>(SessionState.Loading)
    val state: StateFlow<SessionState> = _state.asStateFlow()

    @Volatile
    private var tokens: AuthTokens? = null

    /** OkHttp iş parçacıklarından okunur; bellekteki kopyayı döner. */
    fun currentTokens(): AuthTokens? = tokens

    suspend fun restore() = mutex.withLock {
        if (_state.value != SessionState.Loading) return@withLock
        tokens = store.read()
        _state.value = if (tokens != null) SessionState.LoggedIn else SessionState.LoggedOut
    }

    suspend fun save(newTokens: AuthTokens) = mutex.withLock {
        store.write(newTokens)
        tokens = newTokens
        _state.value = SessionState.LoggedIn
    }

    suspend fun clear() = mutex.withLock {
        store.clear()
        tokens = null
        _state.value = SessionState.LoggedOut
    }
}
