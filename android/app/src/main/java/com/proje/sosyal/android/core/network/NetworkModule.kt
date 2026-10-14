package com.proje.sosyal.android.core.network

import com.proje.sosyal.android.BuildConfig
import com.proje.sosyal.android.data.remote.AuthApi
import com.proje.sosyal.android.data.remote.SystemApi
import com.proje.sosyal.android.data.remote.UserApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/** Belirteç eklemeyen, yalnızca kimlik doğrulama uç noktaları için kullanılan istemci. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PublicApi

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json {
        ignoreUnknownKeys = true   // Backend yeni alan eklediğinde eski uygulama sürümleri bozulmasın
        explicitNulls = false
    }

    @Provides
    @Singleton
    @PublicApi
    fun providePublicOkHttpClient(): OkHttpClient = baseClient().build()

    @Provides
    @Singleton
    fun provideOkHttpClient(
        @PublicApi publicClient: OkHttpClient,
        authInterceptor: AuthInterceptor,
        tokenAuthenticator: TokenAuthenticator,
    ): OkHttpClient =
        publicClient.newBuilder()
            .addInterceptor(authInterceptor)
            .authenticator(tokenAuthenticator)
            .build()

    @Provides
    @Singleton
    @PublicApi
    fun providePublicRetrofit(@PublicApi client: OkHttpClient, json: Json): Retrofit = retrofit(client, json)

    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient, json: Json): Retrofit = retrofit(client, json)

    @Provides
    @Singleton
    fun provideAuthApi(@PublicApi retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideUserApi(retrofit: Retrofit): UserApi = retrofit.create(UserApi::class.java)

    @Provides
    @Singleton
    fun provideSystemApi(retrofit: Retrofit): SystemApi = retrofit.create(SystemApi::class.java)

    private fun baseClient(): OkHttpClient.Builder =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .apply {
                if (BuildConfig.DEBUG) {
                    // BASIC: yalnızca yöntem, URL, durum kodu. Gövde ve başlıklar (belirteçler, parolalar,
                    // kişisel veriler) loglanmaz. Release derlemesinde hiç log yoktur.
                    addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BASIC))
                }
            }

    private fun retrofit(client: OkHttpClient, json: Json): Retrofit =
        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
}
