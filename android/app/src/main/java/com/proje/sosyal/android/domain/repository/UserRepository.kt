package com.proje.sosyal.android.domain.repository

import com.proje.sosyal.android.core.common.AppResult
import com.proje.sosyal.android.domain.model.ProfileImageKind
import com.proje.sosyal.android.domain.model.ProfileUpdate
import com.proje.sosyal.android.domain.model.UserProfile
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {
    /** Son bilinen profil; profil ve düzenleme ekranları aynı veriyi izler. Çıkışta temizlenir. */
    val me: StateFlow<UserProfile?>

    suspend fun getMe(): AppResult<UserProfile>
    suspend fun updateMe(update: ProfileUpdate): AppResult<UserProfile>

    /** [jpeg]: cihazda küçültülmüş JPEG. Sunucu yeniden işler ve üst veriyi siler. */
    suspend fun uploadImage(kind: ProfileImageKind, jpeg: ByteArray): AppResult<UserProfile>
    suspend fun removeImage(kind: ProfileImageKind): AppResult<UserProfile>
}
