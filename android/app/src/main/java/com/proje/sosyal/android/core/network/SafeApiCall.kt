package com.proje.sosyal.android.core.network

import com.proje.sosyal.android.core.common.AppError
import com.proje.sosyal.android.core.common.AppResult
import com.proje.sosyal.android.data.remote.dto.ApiErrorDto
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

private val errorJson = Json { ignoreUnknownKeys = true }

/** Retrofit çağrısını [AppResult]'a çevirir. Yanıt gövdesi veya istisna mesajı loglanmaz. */
suspend fun <T> safeApiCall(block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        AppResult.Failure(AppError.NETWORK)
    } catch (e: HttpException) {
        val body = parseErrorBody(e)
        AppResult.Failure(errorFor(e.code(), body?.code), userMessage(body))
    } catch (e: SerializationException) {
        AppResult.Failure(AppError.UNKNOWN)
    }

private fun parseErrorBody(e: HttpException): ApiErrorDto? =
    runCatching {
        e.response()?.errorBody()?.string()?.let { errorJson.decodeFromString(ApiErrorDto.serializer(), it) }
    }.getOrNull()

/** Alan hatası varsa ilk alan mesajı daha açıklayıcıdır (ör. "Parola 10-64 karakter olmalı."). */
private fun userMessage(body: ApiErrorDto?): String? =
    body?.fieldErrors?.firstOrNull()?.message ?: body?.message

internal fun errorFor(httpStatus: Int, code: String?): AppError =
    when (code) {
        "VALIDATION_FAILED", "MALFORMED_REQUEST" -> AppError.VALIDATION
        "INVALID_CREDENTIALS" -> AppError.INVALID_CREDENTIALS
        "ACCOUNT_SUSPENDED" -> AppError.ACCOUNT_SUSPENDED
        "USERNAME_TAKEN" -> AppError.USERNAME_TAKEN
        "EMAIL_TAKEN" -> AppError.EMAIL_TAKEN
        "AGE_NOT_ELIGIBLE" -> AppError.AGE_NOT_ELIGIBLE
        "TOO_MANY_REQUESTS" -> AppError.TOO_MANY_REQUESTS
        "UNAUTHORIZED", "INVALID_REFRESH_TOKEN" -> AppError.UNAUTHORIZED
        else -> when (httpStatus) {
            401 -> AppError.UNAUTHORIZED
            429 -> AppError.TOO_MANY_REQUESTS
            in 500..599 -> AppError.SERVER
            else -> AppError.UNKNOWN
        }
    }
