package com.proje.sosyal.android.data.repository

import com.proje.sosyal.android.core.common.AppResult
import com.proje.sosyal.android.core.common.IoDispatcher
import com.proje.sosyal.android.core.network.safeApiCall
import com.proje.sosyal.android.data.remote.UserApi
import com.proje.sosyal.android.data.remote.dto.MeDto
import com.proje.sosyal.android.data.remote.dto.UpdateMeRequestDto
import com.proje.sosyal.android.domain.model.ProfileUpdate
import com.proje.sosyal.android.domain.model.UserProfile
import com.proje.sosyal.android.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject

class UserRepositoryImpl @Inject constructor(
    private val api: UserApi,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : UserRepository {

    override suspend fun getMe(): AppResult<UserProfile> = withContext(ioDispatcher) {
        safeApiCall { api.me().toDomain() }
    }

    override suspend fun updateMe(update: ProfileUpdate): AppResult<UserProfile> = withContext(ioDispatcher) {
        safeApiCall {
            api.updateMe(
                UpdateMeRequestDto(
                    displayName = update.displayName.trim(),
                    bio = update.bio.trim(),
                    profileVisibility = if (update.isPrivate) "PRIVATE" else "PUBLIC",
                ),
            ).toDomain()
        }
    }
}

internal fun MeDto.toDomain() = UserProfile(
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
)
