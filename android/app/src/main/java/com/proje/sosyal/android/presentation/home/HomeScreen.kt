package com.proje.sosyal.android.presentation.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.proje.sosyal.android.R
import com.proje.sosyal.android.core.common.AppError
import com.proje.sosyal.android.core.ui.components.EmptyView
import com.proje.sosyal.android.core.ui.components.ErrorView
import com.proje.sosyal.android.core.ui.components.LoadingView
import com.proje.sosyal.android.core.ui.theme.SosyalTheme
import com.proje.sosyal.android.core.ui.theme.Spacing

@Composable
fun HomeRouteScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeScreen(state = state, onRetry = viewModel::retry)
}

@Composable
fun HomeScreen(state: HomeUiState, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    when (state) {
        HomeUiState.Loading -> LoadingView(modifier)
        is HomeUiState.Error -> ErrorView(error = state.error, onRetry = onRetry, modifier = modifier)
        is HomeUiState.Connected -> Column(modifier.fillMaxSize()) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.home_connected, state.apiVersion),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.sm),
                )
            }
            EmptyView(
                title = stringResource(R.string.home_feed_empty_title),
                message = stringResource(R.string.home_feed_empty_message),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeConnectedPreview() {
    SosyalTheme { HomeScreen(state = HomeUiState.Connected("v1"), onRetry = {}) }
}

@Preview(showBackground = true)
@Composable
private fun HomeErrorPreview() {
    SosyalTheme { HomeScreen(state = HomeUiState.Error(AppError.NETWORK), onRetry = {}) }
}
