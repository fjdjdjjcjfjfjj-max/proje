package com.proje.sosyal.common.error

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.MaxUploadSizeExceededException
import org.springframework.web.multipart.MultipartException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.servlet.resource.NoResourceFoundException

/**
 * Merkezi hata yönetimi.
 *
 * Kurallar:
 *  - İstemciye yalnızca [ErrorCode] mesajları döner; istisna mesajları, SQL hataları ve
 *    yığın izleri asla yanıta yazılmaz.
 *  - Loglara istek gövdesi, reddedilen alan değerleri veya kısıt ihlali detayları
 *    (ör. "Key (email)=(...) already exists") yazılmaz; bunlar kişisel veri içerebilir.
 */
@RestControllerAdvice
class GlobalExceptionHandler(private val errors: ApiErrorFactory) {

    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(ApiException::class)
    fun handleApi(ex: ApiException, request: HttpServletRequest): ResponseEntity<ApiError> {
        val response = respond(ex.errorCode, request, ex.message)
        if (ex is RateLimitExceededException) {
            return ResponseEntity.status(response.statusCode)
                .header(HttpHeaders.RETRY_AFTER, ex.retryAfterSeconds.toString())
                .body(response.body)
        }
        return response
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidation(ex: MethodArgumentNotValidException, request: HttpServletRequest): ResponseEntity<ApiError> {
        val fieldErrors = ex.bindingResult.fieldErrors.map {
            ApiError.FieldErrorItem(it.field, it.defaultMessage ?: "Geçersiz değer.")
        }
        return respond(ErrorCode.VALIDATION_FAILED, request, fieldErrors = fieldErrors)
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleMethodValidation(ex: HandlerMethodValidationException, request: HttpServletRequest): ResponseEntity<ApiError> {
        val fieldErrors = ex.parameterValidationResults.flatMap { result ->
            val name = result.methodParameter.parameterName ?: "parameter"
            result.resolvableErrors.map { ApiError.FieldErrorItem(name, it.defaultMessage ?: "Geçersiz değer.") }
        }
        return respond(ErrorCode.VALIDATION_FAILED, request, fieldErrors = fieldErrors)
    }

    @ExceptionHandler(
        MethodArgumentTypeMismatchException::class,
        MissingServletRequestParameterException::class,
    )
    fun handleBadParameter(ex: Exception, request: HttpServletRequest) =
        respond(ErrorCode.VALIDATION_FAILED, request)

    @ExceptionHandler(MaxUploadSizeExceededException::class)
    fun handleTooLarge(ex: MaxUploadSizeExceededException, request: HttpServletRequest) =
        respond(ErrorCode.FILE_TOO_LARGE, request)

    @ExceptionHandler(MultipartException::class, MissingServletRequestPartException::class)
    fun handleMultipart(ex: Exception, request: HttpServletRequest) =
        respond(ErrorCode.MALFORMED_REQUEST, request)

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleUnreadable(ex: HttpMessageNotReadableException, request: HttpServletRequest) =
        respond(ErrorCode.MALFORMED_REQUEST, request)

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNotFound(ex: NoResourceFoundException, request: HttpServletRequest) =
        respond(ErrorCode.NOT_FOUND, request)

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethod(ex: HttpRequestMethodNotSupportedException, request: HttpServletRequest) =
        respond(ErrorCode.METHOD_NOT_ALLOWED, request)

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleMediaType(ex: HttpMediaTypeNotSupportedException, request: HttpServletRequest) =
        respond(ErrorCode.UNSUPPORTED_MEDIA_TYPE, request)

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException, request: HttpServletRequest) =
        respond(ErrorCode.FORBIDDEN, request)

    @ExceptionHandler(DataIntegrityViolationException::class)
    fun handleIntegrity(ex: DataIntegrityViolationException, request: HttpServletRequest): ResponseEntity<ApiError> {
        // Kısıt detayı kişisel veri içerebileceği için yalnızca istisna türü loglanır.
        log.warn("Veri bütünlüğü ihlali: path={} type={}", request.requestURI, ex.mostSpecificCause.javaClass.simpleName)
        return respond(ErrorCode.CONFLICT, request)
    }

    @ExceptionHandler(Exception::class)
    fun handleUnexpected(ex: Exception, request: HttpServletRequest): ResponseEntity<ApiError> {
        log.error("Beklenmeyen hata: path={} type={}", request.requestURI, ex.javaClass.name, ex)
        return respond(ErrorCode.INTERNAL_ERROR, request)
    }

    private fun respond(
        code: ErrorCode,
        request: HttpServletRequest,
        message: String = code.defaultMessage,
        fieldErrors: List<ApiError.FieldErrorItem> = emptyList(),
    ): ResponseEntity<ApiError> =
        ResponseEntity.status(code.status).body(errors.create(code, request.requestURI, message, fieldErrors))
}
