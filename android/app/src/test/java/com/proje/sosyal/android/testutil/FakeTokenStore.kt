package com.proje.sosyal.android.testutil

import com.proje.sosyal.android.core.session.AuthTokens
import com.proje.sosyal.android.core.session.TokenStore

class FakeTokenStore(var stored: AuthTokens? = null) : TokenStore {
    override suspend fun read(): AuthTokens? = stored
    override suspend fun write(tokens: AuthTokens) { stored = tokens }
    override suspend fun clear() { stored = null }
}
