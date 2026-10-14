package com.proje.sosyal.android.presentation.editprofile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proje.sosyal.android.core.common.AppError
import com.proje.sosyal.android.core.common.AppResult
import com.proje.sosyal.android.core.media.ImageCompressor
import com.proje.sosyal.android.domain.model.ProfileImageKind
import com.proje.sosyal.android.domain.model.ProfileUpdate
import com.proje.sosyal.android.domain.model.UserProfile
import com.proje.sosyal.android.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val profile: UserProfile? = null,
    val displayName: String = "",
    val bio: String = "",
    val isPrivate: Boolean = false,
    /** Şu an yüklenen görsel türü (ilerleme göstergesi için). */
    val uploading: ProfileImageKind? = null,
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: AppError? = null,
    val serverMessage: String? = null,
) {
    val nameValid: Boolean get() = displayName.trim().length in 1..NAME_MAX
    val hasChanges: Boolean
        get() = profile != null && (
            displayName.trim() != profile.displayName ||
                bio.trim() != profile.bio.orEmpty() ||
                isPrivate != profile.isProfilePrivate
            )

    companion object {
        const val NAME_MAX = 50
        const val BIO_MAX = 300
    }
}

/**
 * Fotoğraflar seçildiği anda yüklenir (TikTok gibi); metin alanları "Kaydet" ile gönderilir.
 */
@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val users: UserRepository,
    private val compressor: ImageCompressor,
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileUiState())
    val state: StateFlow<EditProfileUiState> = _state.asStateFlow()

    init {
        users.me.value?.let(::fill) ?: viewModelScope.launch {
            when (val result = users.getMe()) {
                is AppResult.Success -> fill(result.data)
                is AppResult.Failure -> _state.update { it.copy(error = result.error) }
            }
        }
    }

    fun onDisplayNameChange(value: String) {
        if (value.length <= EditProfileUiState.NAME_MAX) _state.update { it.copy(displayName = value, error = null) }
    }

    fun onBioChange(value: String) {
        if (value.length <= EditProfileUiState.BIO_MAX) _state.update { it.copy(bio = value, error = null) }
    }

    fun onPrivateChange(value: Boolean) {
        // 18 yaş altı için gizli profil zorunlu; kural backend'de de uygulanır.
        if (_state.value.profile?.isMinor == true) return
        _state.update { it.copy(isPrivate = value) }
    }

    fun onImagePicked(kind: ProfileImageKind, uri: Uri) {
        if (_state.value.uploading != null) return
        _state.update { it.copy(uploading = kind, error = null, serverMessage = null) }
        viewModelScope.launch {
            val jpeg = try {
                compressor.toJpeg(uri, maxSide = if (kind == ProfileImageKind.BANNER) 2400 else 1200)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update { it.copy(uploading = null, error = AppError.VALIDATION, serverMessage = null) }
                return@launch
            }
            handleImageResult(users.uploadImage(kind, jpeg))
        }
    }

    fun removeImage(kind: ProfileImageKind) {
        if (_state.value.uploading != null) return
        _state.update { it.copy(uploading = kind, error = null) }
        viewModelScope.launch { handleImageResult(users.removeImage(kind)) }
    }

    fun save() {
        val s = _state.value
        if (!s.nameValid || s.isSaving) return
        if (!s.hasChanges) {
            _state.update { it.copy(saved = true) }
            return
        }
        _state.update { it.copy(isSaving = true, error = null, serverMessage = null) }
        viewModelScope.launch {
            when (val result = users.updateMe(ProfileUpdate(s.displayName, s.bio, s.isPrivate))) {
                is AppResult.Success -> _state.update { it.copy(isSaving = false, saved = true, profile = result.data) }
                is AppResult.Failure -> _state.update {
                    it.copy(isSaving = false, error = result.error, serverMessage = result.message)
                }
            }
        }
    }

    private fun handleImageResult(result: AppResult<UserProfile>) {
        _state.update {
            when (result) {
                // Yalnızca görseller güncellenir; kullanıcının yazdığı ama kaydetmediği metin korunur.
                is AppResult.Success -> it.copy(uploading = null, profile = result.data)
                is AppResult.Failure -> it.copy(uploading = null, error = result.error, serverMessage = result.message)
            }
        }
    }

    private fun fill(profile: UserProfile) {
        _state.update {
            it.copy(
                profile = profile,
                displayName = profile.displayName,
                bio = profile.bio.orEmpty(),
                isPrivate = profile.isProfilePrivate || profile.isMinor,
            )
        }
    }
}
