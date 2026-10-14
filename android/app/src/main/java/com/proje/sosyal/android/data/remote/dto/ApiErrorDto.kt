package com.proje.sosyal.android.data.remote.dto

import kotlinx.serialization.Serializable

/** Backend'in ortak hata gövdesi (docs/api.md -> Hata biçimi). */
@Serializable
data class ApiErrorDto(
    val status: Int = 0,
    val code: String = "",
    val message: String? = null,
    val fieldErrors: List<FieldErrorDto> = emptyList(),
) {
    @Serializable
    data class FieldErrorDto(val field: String, val message: String)
}
