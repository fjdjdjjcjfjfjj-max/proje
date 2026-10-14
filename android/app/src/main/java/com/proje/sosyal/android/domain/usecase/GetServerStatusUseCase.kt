package com.proje.sosyal.android.domain.usecase

import com.proje.sosyal.android.core.common.AppResult
import com.proje.sosyal.android.domain.model.ServerStatus
import com.proje.sosyal.android.domain.repository.SystemRepository
import javax.inject.Inject

class GetServerStatusUseCase @Inject constructor(
    private val repository: SystemRepository,
) {
    suspend operator fun invoke(): AppResult<ServerStatus> = repository.getServerStatus()
}
