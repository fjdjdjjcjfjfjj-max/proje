package com.proje.sosyal.android.core.network

import com.proje.sosyal.android.core.session.AuthTokens
import com.proje.sosyal.android.core.session.SessionManager
import com.proje.sosyal.android.data.remote.AuthApi
import com.proje.sosyal.android.data.remote.dto.RefreshRequestDto
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

/**
 * 401 alındığında erişim belirtecini yenileyip isteği bir kez tekrarlar.
 *
 * Yenileme tek seferde yapılır (synchronized): backend kullanılmış bir yenileme belirtecini
 * çalınma olarak yorumlayıp tüm oturumu kapattığı için aynı belirteçle iki eşzamanlı
 * yenileme yapılmamalıdır.
 */
class TokenAuthenticator @Inject constructor(
    private val session: SessionManager,
    private val authApi: AuthApi,
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        val sentToken = response.request.header(AuthInterceptor.HEADER)?.removePrefix(AuthInterceptor.PREFIX)
            ?: return null
        if (response.priorResponse != null) return null // yenilenmiş belirteçle de 401: vazgeç

        synchronized(this) {
            val current = session.currentTokens() ?: return null
            // Başka bir istek bu arada yenilediyse yeni belirteçle tekrar dene.
            if (current.accessToken != sentToken) return response.request.withToken(current.accessToken)

            return when (val result = refresh(current.refreshToken)) {
                is RefreshResult.Success -> {
                    runBlocking { session.save(result.tokens) }
                    response.request.withToken(result.tokens.accessToken)
                }
                RefreshResult.Rejected -> {
                    runBlocking { session.clear() }
                    null
                }
                // Ağ hatası: oturumu kapatma, istek başarısız olsun; kullanıcı tekrar deneyebilir.
                RefreshResult.Unavailable -> null
            }
        }
    }

    private fun refresh(refreshToken: String): RefreshResult = runBlocking {
        try {
            val response = authApi.refresh(RefreshRequestDto(refreshToken))
            RefreshResult.Success(AuthTokens(response.accessToken, response.refreshToken))
        } catch (e: HttpException) {
            if (e.code() == 401 || e.code() == 403) RefreshResult.Rejected else RefreshResult.Unavailable
        } catch (e: IOException) {
            RefreshResult.Unavailable
        }
    }

    private fun Request.withToken(token: String): Request =
        newBuilder().header(AuthInterceptor.HEADER, "${AuthInterceptor.PREFIX}$token").build()

    private sealed interface RefreshResult {
        data class Success(val tokens: AuthTokens) : RefreshResult
        data object Rejected : RefreshResult
        data object Unavailable : RefreshResult
    }
}
