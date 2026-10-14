package com.proje.sosyal.common.error

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Instant

@Component
class ApiErrorFactory(private val clock: Clock) {
    fun create(
        code: ErrorCode,
        path: String,
        message: String = code.defaultMessage,
        fieldErrors: List<ApiError.FieldErrorItem> = emptyList(),
    ): ApiError =
        ApiError(
            status = code.status.value(),
            code = code.name,
            message = message,
            path = path,
            timestamp = Instant.now(clock),
            fieldErrors = fieldErrors,
        )
}
