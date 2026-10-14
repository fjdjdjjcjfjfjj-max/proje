package com.proje.sosyal.android.presentation.editprofile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.proje.sosyal.android.R
import com.proje.sosyal.android.core.ui.components.LoadingView
import com.proje.sosyal.android.core.ui.components.errorText
import com.proje.sosyal.android.core.ui.theme.Spacing
import com.proje.sosyal.android.domain.model.ProfileImageKind
import com.proje.sosyal.android.presentation.profile.ProfileAvatar
import com.proje.sosyal.android.presentation.profile.ProfileBanner

@Composable
fun EditProfileRouteScreen(onBack: () -> Unit, viewModel: EditProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) { if (state.saved) onBack() }

    // Android fotoğraf seçici: galeri izni gerektirmez, yalnızca seçilen fotoğrafa erişim verir.
    val pickAvatar = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onImagePicked(ProfileImageKind.AVATAR, it) }
    }
    val pickBanner = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        uri?.let { viewModel.onImagePicked(ProfileImageKind.BANNER, it) }
    }
    val imagesOnly = PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)

    EditProfileScreen(
        state = state,
        onBack = onBack,
        onSave = viewModel::save,
        onPickAvatar = { pickAvatar.launch(imagesOnly) },
        onPickBanner = { pickBanner.launch(imagesOnly) },
        onRemoveAvatar = { viewModel.removeImage(ProfileImageKind.AVATAR) },
        onRemoveBanner = { viewModel.removeImage(ProfileImageKind.BANNER) },
        onDisplayNameChange = viewModel::onDisplayNameChange,
        onBioChange = viewModel::onBioChange,
        onPrivateChange = viewModel::onPrivateChange,
    )
}

@Composable
fun EditProfileScreen(
    state: EditProfileUiState,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onPickAvatar: () -> Unit,
    onPickBanner: () -> Unit,
    onRemoveAvatar: () -> Unit,
    onRemoveBanner: () -> Unit,
    onDisplayNameChange: (String) -> Unit,
    onBioChange: (String) -> Unit,
    onPrivateChange: (Boolean) -> Unit,
) {
    val profile = state.profile ?: return LoadingView()
    val busy = state.isSaving || state.uploading != null

    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding().imePadding()) {
        // Üst çubuk: geri — başlık — kaydet
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
            }
            Text(
                stringResource(R.string.profile_edit),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onSave, enabled = state.nameValid && !busy) {
                if (state.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.action_save), fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(modifier = Modifier.widthIn(max = 640.dp).fillMaxWidth()) {
                // Kapak + üzerine binen profil fotoğrafı
                Box(modifier = Modifier.fillMaxWidth()) {
                    Box(modifier = Modifier.fillMaxWidth().clickable(enabled = !busy, onClick = onPickBanner)) {
                        ProfileBanner(url = profile.bannerUrl, height = 150.dp)
                        ImageOverlay(
                            label = stringResource(R.string.edit_change_banner),
                            loading = state.uploading == ProfileImageKind.BANNER,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                    Column(
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 95.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(modifier = Modifier.clickable(enabled = !busy, onClick = onPickAvatar)) {
                            ProfileAvatar(name = profile.displayName, imageUrl = profile.avatarUrl, size = 110.dp)
                            if (state.uploading == ProfileImageKind.AVATAR) {
                                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.align(Alignment.BottomEnd).offset(x = (-4).dp, y = (-4).dp),
                                ) {
                                    Icon(
                                        Icons.Filled.Edit,
                                        contentDescription = stringResource(R.string.edit_change_photo),
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.padding(6.dp).size(16.dp),
                                    )
                                }
                            }
                        }
                        TextButton(onClick = onPickAvatar, enabled = !busy) { Text(stringResource(R.string.edit_change_photo)) }
                    }
                }

                // Fotoğrafları kaldırma seçenekleri (yalnızca varsa)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    if (profile.avatarUrl != null) {
                        TextButton(onClick = onRemoveAvatar, enabled = !busy) {
                            Text(stringResource(R.string.edit_remove_photo), color = MaterialTheme.colorScheme.error)
                        }
                    }
                    if (profile.bannerUrl != null) {
                        TextButton(onClick = onRemoveBanner, enabled = !busy) {
                            Text(stringResource(R.string.edit_remove_banner), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                SectionTitle(stringResource(R.string.edit_section_about))
                Column(
                    modifier = Modifier.padding(horizontal = Spacing.md),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    OutlinedTextField(
                        value = state.displayName,
                        onValueChange = onDisplayNameChange,
                        label = { Text(stringResource(R.string.field_display_name)) },
                        singleLine = true,
                        isError = !state.nameValid,
                        supportingText = { Text("${state.displayName.length}/${EditProfileUiState.NAME_MAX}") },
                        enabled = !state.isSaving,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = "@${profile.username}",
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text(stringResource(R.string.field_username)) },
                        supportingText = { Text(stringResource(R.string.edit_username_fixed)) },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.bio,
                        onValueChange = onBioChange,
                        label = { Text(stringResource(R.string.field_bio)) },
                        minLines = 3,
                        maxLines = 6,
                        supportingText = { Text("${state.bio.length}/${EditProfileUiState.BIO_MAX}") },
                        enabled = !state.isSaving,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                SectionTitle(stringResource(R.string.edit_section_privacy))
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Spacing.md),
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(R.string.field_private_profile), style = MaterialTheme.typography.titleMedium)
                            Text(
                                stringResource(
                                    if (profile.isMinor) R.string.field_private_profile_minor else R.string.field_private_profile_hint,
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(Spacing.md))
                        Switch(
                            checked = state.isPrivate,
                            onCheckedChange = onPrivateChange,
                            enabled = !profile.isMinor && !state.isSaving,
                        )
                    }
                }

                state.error?.let {
                    Text(
                        errorText(it, state.serverMessage),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(Spacing.md),
                    )
                }
                Spacer(Modifier.height(Spacing.xl))
            }
        }
    }
}

@Composable
private fun ImageOverlay(label: String, loading: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(50))
            .padding(horizontal = Spacing.md, vertical = Spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
        } else {
            Text(label, color = Color.White, style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(start = Spacing.md, top = Spacing.lg, bottom = Spacing.sm),
    )
}
