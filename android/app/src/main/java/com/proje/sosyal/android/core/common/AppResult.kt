package com.proje.sosyal.android.core.common

/** Domain katmanına sızan hata türleri; ağ kütüphanesi istisnaları burada soyutlanır. */
enum class AppError {
    NETWORK,
    SERVER,
    UNAUTHORIZED,
    VALIDATION,
    INVALID_CREDENTIALS,
    ACCOUNT_SUSPENDED,
    USERNAME_TAKEN,
    EMAIL_TAKEN,
    AGE_NOT_ELIGIBLE,
    TOO_MANY_REQUESTS,
    UNKNOWN,
}

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>

    /**
     * [message]: backend'in kullanıcıya gösterilebilir Türkçe mesajı (varsa). Kişisel veri içermez;
     * yoksa arayüz [error] için yerel metni gösterir.
     */
    data class Failure(val error: AppError, val message: String? = null) : AppResult<Nothing>
}
