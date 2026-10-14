package com.proje.sosyal.android.presentation.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.AssistChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.proje.sosyal.android.R
import com.proje.sosyal.android.core.ui.components.ErrorView
import com.proje.sosyal.android.core.ui.components.LoadingView
import com.proje.sosyal.android.core.ui.theme.SosyalTheme
import com.proje.sosyal.android.core.ui.theme.Spacing
import com.proje.sosyal.android.domain.model.UserProfile

@Composable
fun ProfileRouteScreen(viewModel: ProfileViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        ProfileUiState.Loading -> LoadingView()
        is ProfileUiState.Error -> ErrorView(error = s.error, onRetry = viewModel::retry)
        is ProfileUiState.Loaded -> ProfileContent(profile = s.profile, onLogout = viewModel::logout)
    }
}

@Composable
fun ProfileContent(profile: UserProfile, onLogout: () -> Unit) {
    Column(
        modifier = Modifier
            .widthIn(max = 600.dp)
            .fillMaxWidth()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Text(profile.displayName, style = MaterialTheme.typography.headlineSmall)
        Text(
            "@${profile.username}",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        profile.bio?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
        AssistChip(
            onClick = {},
            label = {
                Text(stringResource(if (profile.isProfilePrivate) R.string.profile_private else R.string.profile_public))
            },
        )
        // Yaş beyanı doğrulanmadıysa "doğrulanmış" iddiasında bulunulmaz.
        Text(
            stringResource(if (profile.isAgeVerified) R.string.profile_age_verified else R.string.profile_age_unverified),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (profile.isMinor) {
            Text(stringResource(R.string.profile_minor_notice), style = MaterialTheme.typography.bodyMedium)
        }
        OutlinedButton(onClick = onLogout, modifier = Modifier.padding(top = Spacing.lg)) {
            Text(stringResource(R.string.action_logout))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ProfilePreview() {
    SosyalTheme {
        ProfileContent(
            UserProfile("id", "ornek", "Örnek Kullanıcı", "ornek@example.com", null, false, false, false),
            onLogout = {},
        )
    }
}
