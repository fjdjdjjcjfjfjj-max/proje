package com.proje.sosyal.android.data.repository

import com.proje.sosyal.android.core.common.AppResult
import com.proje.sosyal.android.core.common.IoDispatcher
import com.proje.sosyal.android.core.network.safeApiCall
import com.proje.sosyal.android.data.remote.SystemApi
import com.proje.sosyal.android.domain.model.ServerStatus
import com.proje.sosyal.android.domain.repository.SystemRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class SystemRepositoryImpl @Inject constructor(
    private val api: SystemApi,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : SystemRepository {

    override suspend fun getServerStatus(): AppResult<ServerStatus> = withContext(ioDispatcher) {
        safeApiCall {
            val dto = api.getStatus()
            ServerStatus(isUp = dto.status == "UP", apiVersion = dto.apiVersion)
        }
    }
}
