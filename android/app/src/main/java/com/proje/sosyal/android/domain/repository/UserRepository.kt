package com.proje.sosyal.android.domain.repository

import com.proje.sosyal.android.core.common.AppResult
import com.proje.sosyal.android.domain.model.ProfileUpdate
import com.proje.sosyal.android.domain.model.UserProfile

interface UserRepository {
    suspend fun getMe(): AppResult<UserProfile>
    suspend fun updateMe(update: ProfileUpdate): AppResult<UserProfile>
}
