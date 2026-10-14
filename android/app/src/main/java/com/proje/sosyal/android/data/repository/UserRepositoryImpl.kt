package com.proje.sosyal.android.data.repository

import com.proje.sosyal.android.BuildConfig
import com.proje.sosyal.android.core.common.AppResult
import com.proje.sosyal.android.core.common.IoDispatcher
import com.proje.sosyal.android.core.network.safeApiCall
import com.proje.sosyal.android.core.session.SessionManager
import com.proje.sosyal.android.core.session.SessionState
import com.proje.sosyal.android.data.remote.UserApi
import com.proje.sosyal.android.data.remote.dto.MeDto
import com.proje.sosyal.android.data.remote.dto.UpdateMeRequestDto
import com.proje.sosyal.android.domain.model.ProfileImageKind
import com.proje.sosyal.android.domain.model.ProfileUpdate
import com.proje.sosyal.android.domain.model.UserProfile
import com.proje.sosyal.android.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val api: UserApi,
    session: SessionManager,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UserRepository {

    private val _me = MutableStateFlow<UserProfile?>(null)
    override val me: StateFlow<UserProfile?> = _me.asStateFlow()

    init {
        // Başka hesapla giriş yapıldığında önceki kullanıcının bilgileri bir an bile görünmesin.
        CoroutineScope(SupervisorJob() + ioDispatcher).launch {
            session.state.collect { if (it == SessionState.LoggedOut) _me.value = null }
        }
    }

    override suspend fun getMe(): AppResult<UserProfile> = call { api.me() }

    override suspend fun updateMe(update: ProfileUpdate): AppResult<UserProfile> = call {
        api.updateMe(
            UpdateMeRequestDto(
                displayName = update.displayName.trim(),
                bio = update.bio.trim(),
                profileVisibility = if (update.isPrivate) "PRIVATE" else "PUBLIC",
            ),
        )
    }

    override suspend fun uploadImage(kind: ProfileImageKind, jpeg: ByteArray): AppResult<UserProfile> = call {
        val part = MultipartBody.Part.createFormData(
            "file",
            "${kind.path}.jpg",
            jpeg.toRequestBody("image/jpeg".toMediaType()),
        )
        api.uploadImage(kind.path, part)
    }

    override suspend fun removeImage(kind: ProfileImageKind): AppResult<UserProfile> = call { api.deleteImage(kind.path) }

    private suspend fun call(block: suspend () -> MeDto): AppResult<UserProfile> = withContext(ioDispatcher) {
        safeApiCall { block().toDomain() }.also { if (it is AppResult.Success) _me.value = it.data }
    }

    private val ProfileImageKind.path: String
        get() = when (this) {
            ProfileImageKind.AVATAR -> "avatar"
            ProfileImageKind.BANNER -> "banner"
        }
}

internal fun MeDto.toDomain(baseUrl: String = BuildConfig.API_BASE_URL) = UserProfile(
    id = id,
    username = username,
    displayName = displayName,
    email = email,
    bio = bio,
    isMinor = ageGroup == "MINOR",
    isAgeVerified = ageVerificationStatus == "VERIFIED",
    isProfilePrivate = profileVisibility == "PRIVATE",
    postCount = stats.postCount,
    likesReceived = stats.likesReceived,
    avatarUrl = avatarUrl?.let { absoluteUrl(baseUrl, it) },
    bannerUrl = bannerUrl?.let { absoluteUrl(baseUrl, it) },
)

internal fun absoluteUrl(baseUrl: String, path: String): String = baseUrl.trimEnd('/') + "/" + path.trimStart('/')
