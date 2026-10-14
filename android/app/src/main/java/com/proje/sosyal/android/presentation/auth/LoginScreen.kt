package com.proje.sosyal.android.presentation.auth

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.proje.sosyal.android.R
import com.proje.sosyal.android.core.ui.components.errorText
import com.proje.sosyal.android.core.ui.theme.SosyalTheme

@Composable
fun LoginRoute(onGoRegister: () -> Unit, viewModel: LoginViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LoginScreen(
        state = state,
        onIdentifierChange = viewModel::onIdentifierChange,
        onPasswordChange = viewModel::onPasswordChange,
        onSubmit = viewModel::submit,
        onGoRegister = onGoRegister,
    )
}

@Composable
fun LoginScreen(
    state: LoginUiState,
    onIdentifierChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onGoRegister: () -> Unit,
) {
    AuthFormContainer {
        Text(stringResource(R.string.login_title), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(R.string.login_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FormTextField(
            value = state.identifier,
            onValueChange = onIdentifierChange,
            label = stringResource(R.string.field_identifier),
            keyboardType = KeyboardType.Email,
            enabled = !state.isSubmitting,
        )
        PasswordField(
            value = state.password,
            onValueChange = onPasswordChange,
            label = stringResource(R.string.field_password),
            imeAction = ImeAction.Done,
            enabled = !state.isSubmitting,
        )
        FormError(state.error?.let { errorText(it, state.serverMessage) })
        Button(onClick = onSubmit, enabled = state.canSubmit, modifier = Modifier.fillMaxWidth()) {
            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.action_login))
            }
        }
        TextButton(onClick = onGoRegister, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_go_register))
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    SosyalTheme {
        LoginScreen(LoginUiState(identifier = "ornek_kullanici"), {}, {}, {}, {})
    }
}
