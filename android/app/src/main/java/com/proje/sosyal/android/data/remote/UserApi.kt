package com.proje.sosyal.android.data.remote

import com.proje.sosyal.android.data.remote.dto.MeDto
import retrofit2.http.GET

interface UserApi {
    @GET("api/v1/users/me")
    suspend fun me(): MeDto
}
