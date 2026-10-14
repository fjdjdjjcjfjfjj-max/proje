package com.proje.sosyal.android.core.network

import com.proje.sosyal.android.core.common.AppError
import com.proje.sosyal.android.core.common.AppResult
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class SafeApiCallTest {

    private fun httpError(status: Int, body: String) =
        HttpException(Response.error<Any>(status, body.toResponseBody("application/json".toMediaType())))

    @Test
    fun `backend hata kodu ve mesaji cozulur`() = runTest {
        val result = safeApiCall<Unit> {
            throw httpError(409, """{"status":409,"code":"USERNAME_TAKEN","message":"Bu kullanıcı adı kullanılıyor."}""")
        }
        assertEquals(AppResult.Failure(AppError.USERNAME_TAKEN, "Bu kullanıcı adı kullanılıyor."), result)
    }

    @Test
    fun `alan hatasi varsa ilk alan mesaji kullanilir`() = runTest {
        val result = safeApiCall<Unit> {
            throw httpError(
                400,
                """{"code":"VALIDATION_FAILED","message":"Gönderilen bilgiler geçersiz.",
                   "fieldErrors":[{"field":"password","message":"Parola 10-64 karakter olmalı."}]}""",
            )
        }
        assertEquals(AppResult.Failure(AppError.VALIDATION, "Parola 10-64 karakter olmalı."), result)
    }

    @Test
    fun `cozulemeyen govdede HTTP durum koduna gore eslenir`() = runTest {
        assertEquals(AppError.SERVER, (safeApiCall<Unit> { throw httpError(502, "<html>") } as AppResult.Failure).error)
        assertEquals(AppError.TOO_MANY_REQUESTS, (safeApiCall<Unit> { throw httpError(429, "") } as AppResult.Failure).error)
        assertEquals(AppError.UNAUTHORIZED, errorFor(401, null))
    }
}
