package com.proje.sosyal.android.data.repository

import com.proje.sosyal.android.core.common.AppResult
import com.proje.sosyal.android.core.common.IoDispatcher
import com.proje.sosyal.android.core.network.safeApiCall
import com.proje.sosyal.android.core.session.AuthTokens
import com.proje.sosyal.android.core.session.SessionManager
import com.proje.sosyal.android.data.remote.AuthApi
import com.proje.sosyal.android.data.remote.dto.AuthResponseDto
import com.proje.sosyal.android.data.remote.dto.LoginRequestDto
import com.proje.sosyal.android.data.remote.dto.RefreshRequestDto
import com.proje.sosyal.android.data.remote.dto.RegisterRequestDto
import com.proje.sosyal.android.domain.model.RegistrationForm
import com.proje.sosyal.android.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val session: SessionManager,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : AuthRepository {

    override suspend fun login(identifier: String, password: String): AppResult<Unit> =
        authenticate { api.login(LoginRequestDto(identifier.trim(), password)) }

    override suspend fun register(form: RegistrationForm): AppResult<Unit> =
        authenticate {
            api.register(
                RegisterRequestDto(
                    username = form.username.trim(),
                    displayName = form.displayName.trim(),
                    email = form.email.trim(),
                    password = form.password,
                    birthDate = form.birthDate.toString(),
                    privacyNoticeAccepted = form.privacyNoticeAccepted,
                ),
            )
        }

    override suspend fun logout() = withContext(ioDispatcher) {
        session.currentTokens()?.let { tokens ->
            // Sunucu tarafında oturumu iptal et; başarısız olsa da yerel oturum silinir.
            safeApiCall { api.logout(RefreshRequestDto(tokens.refreshToken)) }
        }
        session.clear()
    }

    private suspend fun authenticate(call: suspend () -> AuthResponseDto): AppResult<Unit> =
        withContext(ioDispatcher) {
            when (val result = safeApiCall { call() }) {
                is AppResult.Success -> {
                    session.save(AuthTokens(result.data.accessToken, result.data.refreshToken))
                    AppResult.Success(Unit)
                }
                is AppResult.Failure -> result
            }
        }
}
