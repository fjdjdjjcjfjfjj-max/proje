package com.proje.sosyal.android.data.remote

import com.proje.sosyal.android.data.remote.dto.SystemStatusDto
import retrofit2.http.GET

interface SystemApi {
    @GET("api/v1/system/status")
    suspend fun getStatus(): SystemStatusDto
}
