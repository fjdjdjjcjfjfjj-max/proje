package com.proje.sosyal.android.data.remote

import com.proje.sosyal.android.data.remote.dto.AuthResponseDto
import com.proje.sosyal.android.data.remote.dto.LoginRequestDto
import com.proje.sosyal.android.data.remote.dto.RefreshRequestDto
import com.proje.sosyal.android.data.remote.dto.RegisterRequestDto
import retrofit2.http.Body
import retrofit2.http.POST

/** Kimlik doğrulama uç noktaları. Belirteç eklemeyen ayrı bir OkHttp istemcisiyle çağrılır. */
interface AuthApi {
    @POST("api/v1/auth/register")
    suspend fun register(@Body body: RegisterRequestDto): AuthResponseDto

    @POST("api/v1/auth/login")
    suspend fun login(@Body body: LoginRequestDto): AuthResponseDto

    @POST("api/v1/auth/refresh")
    suspend fun refresh(@Body body: RefreshRequestDto): AuthResponseDto

    @POST("api/v1/auth/logout")
    suspend fun logout(@Body body: RefreshRequestDto)
}
