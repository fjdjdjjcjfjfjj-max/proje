package com.proje.sosyal.android.core.common

/** Domain katmanına sızan hata türleri; ağ kütüphanesi istisnaları burada soyutlanır. */
enum class AppError {
    NETWORK,
    SERVER,
    UNAUTHORIZED,
    UNKNOWN,
}

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Failure(val error: AppError) : AppResult<Nothing>
}
