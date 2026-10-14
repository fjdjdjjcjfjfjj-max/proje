package com.proje.sosyal.user.dto

import com.proje.sosyal.user.ProfileVisibility
import jakarta.validation.constraints.Size

/**
 * Kısmi güncelleme: gönderilmeyen (null) alan değişmez. `bio` için boş metin biyografiyi siler.
 * Rol, durum, yaş grubu gibi alanlar bilinçli olarak bu istekte yoktur.
 */
data class UpdateMeRequest(
    @field:Size(min = 1, max = 50, message = "Görünen ad 1-50 karakter olmalı.")
    val displayName: String? = null,

    @field:Size(max = 300, message = "Biyografi en fazla 300 karakter olabilir.")
    val bio: String? = null,

    val profileVisibility: ProfileVisibility? = null,
)
