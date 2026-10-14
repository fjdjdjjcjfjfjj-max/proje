package com.proje.sosyal.common.error

import org.springframework.http.HttpStatus

/**
 * İstemcilerin (Android, web) programatik olarak ayırt edebileceği sabit hata kodları.
 * Mesajlar kullanıcıya gösterilebilir Türkçe metinlerdir ve asla kişisel veri içermez.
 */
enum class ErrorCode(val status: HttpStatus, val defaultMessage: String) {
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Gönderilen bilgiler geçersiz."),
    MALFORMED_REQUEST(HttpStatus.BAD_REQUEST, "İstek gövdesi okunamadı."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "Bu işlem için giriş yapmanız gerekiyor."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "Bu işlem için yetkiniz yok."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "İstenen kaynak bulunamadı."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "Bu HTTP yöntemi desteklenmiyor."),
    UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "İstek içerik türü desteklenmiyor."),
    CONFLICT(HttpStatus.CONFLICT, "İstek mevcut verilerle çakışıyor."),
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Kullanıcı adı/e-posta veya parola hatalı."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Oturumunuzun süresi doldu. Lütfen tekrar giriş yapın."),
    ACCOUNT_SUSPENDED(HttpStatus.FORBIDDEN, "Hesabınız askıya alınmış."),
    USERNAME_TAKEN(HttpStatus.CONFLICT, "Bu kullanıcı adı kullanılıyor."),
    EMAIL_TAKEN(HttpStatus.CONFLICT, "Bu e-posta adresiyle kayıtlı bir hesap var."),
    AGE_NOT_ELIGIBLE(HttpStatus.BAD_REQUEST, "Kayıt olmak için en az 13 yaşında olmalısınız."),
    PROFILE_VISIBILITY_NOT_ALLOWED(HttpStatus.FORBIDDEN, "18 yaşından küçük hesapların profili gizli kalmalıdır."),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "Çok fazla istek gönderildi. Lütfen daha sonra tekrar deneyin."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Beklenmeyen bir hata oluştu."),
}
