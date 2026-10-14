package com.proje.sosyal.android.core.network

import com.proje.sosyal.android.core.common.AppError
import com.proje.sosyal.android.core.common.AppResult
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException
import java.io.IOException

/** Retrofit çağrısını [AppResult]'a çevirir. Yanıt gövdesi veya istisna mesajı loglanmaz. */
suspend fun <T> safeApiCall(block: suspend () -> T): AppResult<T> =
    try {
        AppResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: IOException) {
        AppResult.Failure(AppError.NETWORK)
    } catch (e: HttpException) {
        AppResult.Failure(
            when (e.code()) {
                401 -> AppError.UNAUTHORIZED
                in 500..599 -> AppError.SERVER
                else -> AppError.UNKNOWN
            },
        )
    } catch (e: SerializationException) {
        AppResult.Failure(AppError.UNKNOWN)
    }
