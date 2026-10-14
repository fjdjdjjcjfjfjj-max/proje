package com.proje.sosyal.android.data.repository

import com.proje.sosyal.android.core.common.AppError
import com.proje.sosyal.android.core.common.AppResult
import com.proje.sosyal.android.core.session.AuthTokens
import com.proje.sosyal.android.core.session.SessionManager
import com.proje.sosyal.android.core.session.SessionState
import com.proje.sosyal.android.data.remote.AuthApi
import com.proje.sosyal.android.data.remote.dto.AuthResponseDto
import com.proje.sosyal.android.data.remote.dto.MeDto
import com.proje.sosyal.android.testutil.FakeTokenStore
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class AuthRepositoryImplTest {

    private val api = mockk<AuthApi>()
    private val store = FakeTokenStore()
    private val session = SessionManager(store)

    private val response = AuthResponseDto(
        accessToken = "access",
        refreshToken = "refresh",
        expiresIn = 900,
        refreshExpiresIn = 2_592_000,
        user = MeDto("id", "ayse", "Ayşe", "a@example.com", null, "USER", "ADULT", "UNVERIFIED", "PUBLIC", "2026-01-01T00:00:00Z"),
    )

    @Test
    fun `basarili giris belirtecleri kaydeder ve oturumu acar`() = runTest {
        coEvery { api.login(any()) } returns response
        val repo = AuthRepositoryImpl(api, session, StandardTestDispatcher(testScheduler))

        assertEquals(AppResult.Success(Unit), repo.login(" ayse ", "GucluParola123"))
        assertEquals(AuthTokens("access", "refresh"), store.stored)
        assertEquals(SessionState.LoggedIn, session.state.value)
        coVerify { api.login(match { it.identifier == "ayse" }) }
    }

    @Test
    fun `basarisiz giris oturum acmaz`() = runTest {
        coEvery { api.login(any()) } throws HttpException(Response.error<Any>(401, "".toResponseBody()))
        val repo = AuthRepositoryImpl(api, session, StandardTestDispatcher(testScheduler))

        assertEquals(AppError.UNAUTHORIZED, (repo.login("ayse", "yanlis") as AppResult.Failure).error)
        assertNull(store.stored)
    }

    @Test
    fun `cikis sunucuya ulasilamasa da yerel oturumu siler`() = runTest {
        session.save(AuthTokens("access", "refresh"))
        coEvery { api.logout(any()) } throws IOException("ağ yok")
        val repo = AuthRepositoryImpl(api, session, StandardTestDispatcher(testScheduler))

        repo.logout()

        assertNull(store.stored)
        assertEquals(SessionState.LoggedOut, session.state.value)
        coVerify { api.logout(match { it.refreshToken == "refresh" }) }
    }
}
