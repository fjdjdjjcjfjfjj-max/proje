package com.proje.sosyal.android.core.network

import com.proje.sosyal.android.core.session.SessionManager
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** Oturum varsa isteklere `Authorization: Bearer` başlığı ekler. */
class AuthInterceptor @Inject constructor(
    private val session: SessionManager,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = session.currentTokens()?.accessToken
        val request = if (token != null) {
            chain.request().newBuilder().header(HEADER, "$PREFIX$token").build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }

    companion object {
        const val HEADER = "Authorization"
        const val PREFIX = "Bearer "
    }
}
