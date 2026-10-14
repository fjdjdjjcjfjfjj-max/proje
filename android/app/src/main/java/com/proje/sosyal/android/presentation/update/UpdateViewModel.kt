package com.proje.sosyal.android.presentation.update

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.proje.sosyal.android.core.update.AppUpdater
import com.proje.sosyal.android.core.update.UpdateManifest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

sealed interface UpdateUiState {
    data object None : UpdateUiState
    data class Available(val manifest: UpdateManifest) : UpdateUiState
    data class Downloading(val manifest: UpdateManifest, val progress: Float) : UpdateUiState
    data class ReadyToInstall(val manifest: UpdateManifest, val apk: File) : UpdateUiState
    data class Failed(val manifest: UpdateManifest) : UpdateUiState
}

@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val updater: AppUpdater,
) : ViewModel() {

    private val _state = MutableStateFlow<UpdateUiState>(UpdateUiState.None)
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            updater.checkForUpdate()?.let { _state.value = UpdateUiState.Available(it) }
        }
    }

    fun dismiss() {
        if (_state.value !is UpdateUiState.Downloading) _state.value = UpdateUiState.None
    }

    fun download() {
        val manifest = when (val s = _state.value) {
            is UpdateUiState.Available -> s.manifest
            is UpdateUiState.Failed -> s.manifest
            else -> return
        }
        _state.value = UpdateUiState.Downloading(manifest, 0f)
        viewModelScope.launch {
            _state.value = try {
                val apk = updater.download(manifest) { progress ->
                    _state.value = UpdateUiState.Downloading(manifest, progress)
                }
                UpdateUiState.ReadyToInstall(manifest, apk)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                UpdateUiState.Failed(manifest)
            }
        }
    }

    fun canInstall(): Boolean = updater.canInstall()
    fun installPermissionIntent() = updater.installPermissionIntent()
    fun installIntent(apk: File) = updater.installIntent(apk)
}
