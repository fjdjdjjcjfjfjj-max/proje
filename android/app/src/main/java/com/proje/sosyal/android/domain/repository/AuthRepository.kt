package com.proje.sosyal.android.domain.repository

import com.proje.sosyal.android.core.common.AppResult
import com.proje.sosyal.android.domain.model.RegistrationForm

interface AuthRepository {
    suspend fun login(identifier: String, password: String): AppResult<Unit>
    suspend fun register(form: RegistrationForm): AppResult<Unit>

    /** Sunucuya ulaşılamasa bile yerel oturum her durumda silinir. */
    suspend fun logout()
}
