package com.proje.sosyal.android.presentation.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.proje.sosyal.android.R
import com.proje.sosyal.android.core.ui.theme.Spacing

/** Yeni sürüm varsa her ekranın üzerinde gösterilir. Kullanıcı "Sonra" diyerek erteleyebilir. */
@Composable
fun UpdatePrompt(viewModel: UpdateViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    when (val s = state) {
        UpdateUiState.None -> Unit
        is UpdateUiState.Available, is UpdateUiState.Failed -> {
            val manifest = if (s is UpdateUiState.Available) s.manifest else (s as UpdateUiState.Failed).manifest
            AlertDialog(
                onDismissRequest = viewModel::dismiss,
                title = { Text(stringResource(R.string.update_title)) },
                text = {
                    Text(
                        if (s is UpdateUiState.Failed) stringResource(R.string.update_failed)
                        else stringResource(R.string.update_message, manifest.versionName),
                    )
                },
                confirmButton = {
                    TextButton(onClick = viewModel::download) {
                        Text(stringResource(if (s is UpdateUiState.Failed) R.string.action_retry else R.string.update_action))
                    }
                },
                dismissButton = { TextButton(onClick = viewModel::dismiss) { Text(stringResource(R.string.update_later)) } },
            )
        }
        is UpdateUiState.Downloading -> AlertDialog(
            onDismissRequest = {},
            title = { Text(stringResource(R.string.update_downloading)) },
            text = {
                Column(modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm)) {
                    if (s.progress >= 0f) {
                        LinearProgressIndicator(progress = { s.progress }, modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            confirmButton = {},
        )
        is UpdateUiState.ReadyToInstall -> AlertDialog(
            onDismissRequest = viewModel::dismiss,
            title = { Text(stringResource(R.string.update_ready_title)) },
            text = {
                Text(
                    stringResource(
                        if (viewModel.canInstall()) R.string.update_ready_message else R.string.update_permission_message,
                    ),
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    // İzin yoksa önce ayarlar açılır; kullanıcı döndüğünde aynı düğmeyle kurulum başlar.
                    val intent = if (viewModel.canInstall()) viewModel.installIntent(s.apk) else viewModel.installPermissionIntent()
                    context.startActivity(intent)
                }) {
                    Text(
                        stringResource(
                            if (viewModel.canInstall()) R.string.update_install else R.string.update_open_settings,
                        ),
                    )
                }
            },
            dismissButton = { TextButton(onClick = viewModel::dismiss) { Text(stringResource(R.string.update_later)) } },
        )
    }
}
