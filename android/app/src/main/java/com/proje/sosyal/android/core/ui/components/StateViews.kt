package com.proje.sosyal.android.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.proje.sosyal.android.R
import com.proje.sosyal.android.core.common.AppError
import com.proje.sosyal.android.core.ui.theme.SosyalTheme
import com.proje.sosyal.android.core.ui.theme.Spacing

@Composable
fun LoadingView(modifier: Modifier = Modifier) {
    val label = stringResource(R.string.state_loading)
    CenteredColumn(modifier) {
        CircularProgressIndicator(modifier = Modifier.semantics { contentDescription = label })
    }
}

@Composable
fun EmptyView(title: String, message: String, modifier: Modifier = Modifier) {
    CenteredColumn(modifier) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ErrorView(error: AppError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val (titleRes, messageRes) = when (error) {
        AppError.NETWORK -> R.string.error_network_title to R.string.error_network_message
        AppError.SERVER -> R.string.error_server_title to R.string.error_server_message
        AppError.UNAUTHORIZED, AppError.UNKNOWN -> R.string.error_unknown_title to R.string.error_unknown_message
    }
    CenteredColumn(modifier) {
        Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(Spacing.sm))
        Text(
            stringResource(messageRes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.md))
        Button(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
    }
}

@Composable
private fun CenteredColumn(modifier: Modifier, content: @Composable () -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Geniş ekranlarda metin satırları aşırı uzamasın
        Column(
            modifier = Modifier.widthIn(max = 480.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) { content() }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorViewPreview() {
    SosyalTheme { ErrorView(error = AppError.NETWORK, onRetry = {}) }
}

@Preview(showBackground = true)
@Composable
private fun EmptyViewPreview() {
    SosyalTheme { EmptyView(title = "Önizleme başlığı", message = "Önizleme açıklaması") }
}
