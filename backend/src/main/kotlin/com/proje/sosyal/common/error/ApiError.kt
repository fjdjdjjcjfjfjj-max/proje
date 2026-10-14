package com.proje.sosyal.common.error

import java.time.Instant

/** Tüm hata yanıtlarının ortak gövdesi. */
data class ApiError(
    val status: Int,
    val code: String,
    val message: String,
    val path: String,
    val timestamp: Instant,
    val fieldErrors: List<FieldErrorItem> = emptyList(),
) {
    /** Reddedilen değer bilinçli olarak döndürülmez; parola veya kişisel veri içerebilir. */
    data class FieldErrorItem(val field: String, val message: String)
}
