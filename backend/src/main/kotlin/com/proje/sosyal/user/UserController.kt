package com.proje.sosyal.user

import com.proje.sosyal.config.OpenApiConfig
import com.proje.sosyal.user.dto.MeResponse
import com.proje.sosyal.user.dto.UpdateMeRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import com.proje.sosyal.media.ImageProcessor
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/** Kimlik her zaman doğrulanmış belirteçten gelir; istemcinin gönderdiği bir kimliğe güvenilmez (IDOR'a karşı). */
@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Kullanıcılar")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
class UserController(private val userService: UserService) {

    @GetMapping("/me")
    @Operation(summary = "Oturum sahibinin hesap bilgileri ve profil sayaçları")
    fun me(@AuthenticationPrincipal userId: UUID): MeResponse = userService.me(userId)

    @PatchMapping("/me")
    @Operation(summary = "Görünen ad, biyografi ve profil görünürlüğünü günceller")
    fun updateMe(@AuthenticationPrincipal userId: UUID, @Valid @RequestBody request: UpdateMeRequest): MeResponse =
        userService.updateMe(userId, request)

    @PutMapping("/me/avatar", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Profil fotoğrafı yükler (JPEG/PNG, en fazla 8 MB; 512x512 kırpılır, üst veri silinir)")
    fun uploadAvatar(@AuthenticationPrincipal userId: UUID, @RequestPart("file") file: MultipartFile): MeResponse =
        userService.replaceImage(userId, ImageProcessor.Kind.AVATAR, file)

    @DeleteMapping("/me/avatar")
    @Operation(summary = "Profil fotoğrafını kaldırır")
    fun deleteAvatar(@AuthenticationPrincipal userId: UUID): MeResponse =
        userService.removeImage(userId, ImageProcessor.Kind.AVATAR)

    @PutMapping("/me/banner", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    @Operation(summary = "Kapak görseli yükler (JPEG/PNG, en fazla 8 MB; 1500x500 kırpılır, üst veri silinir)")
    fun uploadBanner(@AuthenticationPrincipal userId: UUID, @RequestPart("file") file: MultipartFile): MeResponse =
        userService.replaceImage(userId, ImageProcessor.Kind.BANNER, file)

    @DeleteMapping("/me/banner")
    @Operation(summary = "Kapak görselini kaldırır")
    fun deleteBanner(@AuthenticationPrincipal userId: UUID): MeResponse =
        userService.removeImage(userId, ImageProcessor.Kind.BANNER)
}
