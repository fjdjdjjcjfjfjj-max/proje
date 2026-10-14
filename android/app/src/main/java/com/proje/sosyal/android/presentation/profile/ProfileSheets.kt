package com.proje.sosyal.android.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.proje.sosyal.android.R
import com.proje.sosyal.android.core.ui.components.errorText
import com.proje.sosyal.android.core.ui.theme.Spacing
import com.proje.sosyal.android.domain.model.ProfileUpdate
import com.proje.sosyal.android.domain.model.UserProfile

private const val BIO_MAX = 300
private const val NAME_MAX = 50

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileSheet(
    profile: UserProfile,
    edit: EditState,
    onSave: (ProfileUpdate) -> Unit,
    onDismiss: () -> Unit,
) {
    var displayName by rememberSaveable { mutableStateOf(profile.displayName) }
    var bio by rememberSaveable { mutableStateOf(profile.bio.orEmpty()) }
    var isPrivate by rememberSaveable { mutableStateOf(profile.isProfilePrivate) }
    val nameValid = displayName.trim().length in 1..NAME_MAX

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(stringResource(R.string.profile_edit), style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = displayName,
                onValueChange = { if (it.length <= NAME_MAX) displayName = it },
                label = { Text(stringResource(R.string.field_display_name)) },
                singleLine = true,
                isError = !nameValid,
                supportingText = if (!nameValid) {
                    { Text(stringResource(R.string.validation_display_name)) }
                } else {
                    null
                },
                enabled = !edit.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = bio,
                onValueChange = { if (it.length <= BIO_MAX) bio = it },
                label = { Text(stringResource(R.string.field_bio)) },
                minLines = 3,
                maxLines = 6,
                supportingText = { Text("${bio.length}/$BIO_MAX") },
                enabled = !edit.isSaving,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
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
                // 18 yaş altı için kilitli; kural backend'de de zorlanır.
                Switch(
                    checked = isPrivate || profile.isMinor,
                    onCheckedChange = { isPrivate = it },
                    enabled = !profile.isMinor && !edit.isSaving,
                )
            }
            edit.error?.let {
                Text(errorText(it, edit.serverMessage), color = MaterialTheme.colorScheme.error)
            }
            Button(
                onClick = { onSave(ProfileUpdate(displayName, bio, isPrivate || profile.isMinor)) },
                enabled = nameValid && !edit.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (edit.isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.action_save))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileMenuSheet(profile: UserProfile, onLogout: () -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg)
                .padding(bottom = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(stringResource(R.string.profile_account_section), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(R.string.profile_account_email),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(profile.email, style = MaterialTheme.typography.bodyLarge)
            // Yaş beyanı doğrulanmadıysa "doğrulanmış" iddiasında bulunulmaz.
            Text(
                stringResource(if (profile.isAgeVerified) R.string.profile_age_verified else R.string.profile_age_unverified),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.sm))
            OutlinedButton(
                onClick = onLogout,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.action_logout))
            }
            Spacer(Modifier.height(Spacing.sm))
        }
    }
}
