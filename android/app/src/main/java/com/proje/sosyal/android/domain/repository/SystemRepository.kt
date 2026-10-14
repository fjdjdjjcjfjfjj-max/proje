package com.proje.sosyal.android.domain.repository

import com.proje.sosyal.android.core.common.AppResult
import com.proje.sosyal.android.domain.model.ServerStatus

interface SystemRepository {
    suspend fun getServerStatus(): AppResult<ServerStatus>
}
