package com.proje.sosyal.android.data.remote

import com.proje.sosyal.android.data.remote.dto.MeDto
import com.proje.sosyal.android.data.remote.dto.UpdateMeRequestDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH

interface UserApi {
    @GET("api/v1/users/me")
    suspend fun me(): MeDto

    @PATCH("api/v1/users/me")
    suspend fun updateMe(@Body body: UpdateMeRequestDto): MeDto
}
