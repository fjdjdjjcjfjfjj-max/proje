package com.proje.sosyal.android.data.repository

import com.proje.sosyal.android.core.common.AppError
import com.proje.sosyal.android.core.common.AppResult
import com.proje.sosyal.android.data.remote.SystemApi
import com.proje.sosyal.android.data.remote.dto.SystemStatusDto
import com.proje.sosyal.android.domain.model.ServerStatus
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

class SystemRepositoryImplTest {

    private val api = mockk<SystemApi>()

    @Test
    fun `DTO domain modeline donusturulur`() = runTest {
        coEvery { api.getStatus() } returns SystemStatusDto("UP", "v1", "2026-01-01T00:00:00Z")
        val repository = SystemRepositoryImpl(api, StandardTestDispatcher(testScheduler))

        assertEquals(AppResult.Success(ServerStatus(isUp = true, apiVersion = "v1")), repository.getServerStatus())
    }

    @Test
    fun `IOException ag hatasina donusur`() = runTest {
        coEvery { api.getStatus() } throws IOException("bağlantı yok")
        val repository = SystemRepositoryImpl(api, StandardTestDispatcher(testScheduler))

        assertEquals(AppResult.Failure(AppError.NETWORK), repository.getServerStatus())
    }

    @Test
    fun `5xx sunucu hatasina, 401 yetkisiz hatasina donusur`() = runTest {
        val repository = SystemRepositoryImpl(api, StandardTestDispatcher(testScheduler))

        coEvery { api.getStatus() } throws HttpException(Response.error<Any>(503, "".toResponseBody()))
        assertEquals(AppResult.Failure(AppError.SERVER), repository.getServerStatus())

        coEvery { api.getStatus() } throws HttpException(Response.error<Any>(401, "".toResponseBody()))
        assertEquals(AppResult.Failure(AppError.UNAUTHORIZED), repository.getServerStatus())
    }
}
