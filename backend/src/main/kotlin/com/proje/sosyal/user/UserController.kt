package com.proje.sosyal.user

import com.proje.sosyal.common.error.ApiException
import com.proje.sosyal.common.error.ErrorCode
import com.proje.sosyal.config.OpenApiConfig
import com.proje.sosyal.user.dto.MeResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Kullanıcılar")
@SecurityRequirement(name = OpenApiConfig.BEARER_SCHEME)
class UserController(private val users: UserRepository) {

    @GetMapping("/me")
    @Operation(summary = "Oturum sahibinin hesap bilgileri")
    @Transactional(readOnly = true)
    fun me(@AuthenticationPrincipal userId: UUID): MeResponse {
        // Kimlik, istemcinin gönderdiği bir parametreden değil, doğrulanmış belirteçten gelir (IDOR'a karşı).
        val user = users.findById(userId).orElseThrow { ApiException(ErrorCode.UNAUTHORIZED) }
        return MeResponse.from(user)
    }
}
