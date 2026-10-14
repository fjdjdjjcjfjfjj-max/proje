package com.proje.sosyal.common.error

/** Servis katmanından fırlatılan, istemciye güvenle iletilebilecek iş kuralı hatası. */
open class ApiException(
    val errorCode: ErrorCode,
    override val message: String = errorCode.defaultMessage,
) : RuntimeException(message)
