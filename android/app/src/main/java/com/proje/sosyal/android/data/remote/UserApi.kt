package com.proje.sosyal.android.data.remote

import com.proje.sosyal.android.data.remote.dto.MeDto
import com.proje.sosyal.android.data.remote.dto.UpdateMeRequestDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface UserApi {
    @GET("api/v1/users/me")
    suspend fun me(): MeDto

    @PATCH("api/v1/users/me")
    suspend fun updateMe(@Body body: UpdateMeRequestDto): MeDto

    /** [kind]: "avatar" veya "banner". */
    @Multipart
    @PUT("api/v1/users/me/{kind}")
    suspend fun uploadImage(@Path("kind") kind: String, @Part file: MultipartBody.Part): MeDto

    @DELETE("api/v1/users/me/{kind}")
    suspend fun deleteImage(@Path("kind") kind: String): MeDto
}
