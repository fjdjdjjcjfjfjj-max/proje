package com.proje.sosyal.android.presentation.auth

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.proje.sosyal.android.R
import com.proje.sosyal.android.core.ui.components.errorText
import com.proje.sosyal.android.core.ui.theme.SosyalTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Composable
fun RegisterRoute(onGoLogin: () -> Unit, viewModel: RegisterViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    RegisterScreen(
        state = state,
        actions = RegisterActions(
            onUsernameChange = viewModel::onUsernameChange,
            onDisplayNameChange = viewModel::onDisplayNameChange,
            onEmailChange = viewModel::onEmailChange,
            onPasswordChange = viewModel::onPasswordChange,
            onBirthDateChange = viewModel::onBirthDateChange,
            onPrivacyAcceptedChange = viewModel::onPrivacyAcceptedChange,
            onSubmit = viewModel::submit,
            onGoLogin = onGoLogin,
        ),
    )
}

data class RegisterActions(
    val onUsernameChange: (String) -> Unit,
    val onDisplayNameChange: (String) -> Unit,
    val onEmailChange: (String) -> Unit,
    val onPasswordChange: (String) -> Unit,
    val onBirthDateChange: (LocalDate) -> Unit,
    val onPrivacyAcceptedChange: (Boolean) -> Unit,
    val onSubmit: () -> Unit,
    val onGoLogin: () -> Unit,
)

@Composable
fun RegisterScreen(state: RegisterUiState, actions: RegisterActions) {
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var showPrivacy by rememberSaveable { mutableStateOf(false) }
    val enabled = !state.isSubmitting

    AuthFormContainer {
        Text(stringResource(R.string.register_title), style = MaterialTheme.typography.headlineSmall)

        FormTextField(
            value = state.username,
            onValueChange = actions.onUsernameChange,
            label = stringResource(R.string.field_username),
            supportingText = stringResource(R.string.field_username_hint),
            errorText = state.fieldErrors[RegisterField.USERNAME]?.let { stringResource(it) },
            enabled = enabled,
        )
        FormTextField(
            value = state.displayName,
            onValueChange = actions.onDisplayNameChange,
            label = stringResource(R.string.field_display_name),
            errorText = state.fieldErrors[RegisterField.DISPLAY_NAME]?.let { stringResource(it) },
            enabled = enabled,
        )
        FormTextField(
            value = state.email,
            onValueChange = actions.onEmailChange,
            label = stringResource(R.string.field_email),
            keyboardType = KeyboardType.Email,
            errorText = state.fieldErrors[RegisterField.EMAIL]?.let { stringResource(it) },
            enabled = enabled,
        )
        PasswordField(
            value = state.password,
            onValueChange = actions.onPasswordChange,
            label = stringResource(R.string.field_password),
            supportingText = stringResource(R.string.field_password_hint),
            errorText = state.fieldErrors[RegisterField.PASSWORD]?.let { stringResource(it) },
            enabled = enabled,
        )

        // Doğum tarihi: elle yazım yerine tarih seçici (biçim hatalarını önler)
        OutlinedTextField(
            value = state.birthDate?.format(DISPLAY_FORMAT).orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(stringResource(R.string.field_birth_date)) },
            isError = state.fieldErrors.containsKey(RegisterField.BIRTH_DATE),
            supportingText = {
                Text(
                    state.fieldErrors[RegisterField.BIRTH_DATE]?.let { stringResource(it) }
                        ?: stringResource(R.string.field_birth_date_hint),
                )
            },
            trailingIcon = {
                TextButton(onClick = { showDatePicker = true }, enabled = enabled) {
                    Text(stringResource(R.string.action_select))
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(
                checked = state.privacyNoticeAccepted,
                onCheckedChange = actions.onPrivacyAcceptedChange,
                enabled = enabled,
            )
            Text(stringResource(R.string.privacy_consent), style = MaterialTheme.typography.bodyMedium)
        }
        TextButton(onClick = { showPrivacy = true }) { Text(stringResource(R.string.action_read_privacy)) }
        FormError(state.fieldErrors[RegisterField.PRIVACY]?.let { stringResource(it) })

        // Alanla ilişkilendirilen hatalar (ör. kullanıcı adı alınmış) alanın altında zaten gösterilir.
        val showGeneralError = state.error != null && state.fieldErrors.isEmpty()
        FormError(if (showGeneralError) errorText(state.error!!, state.serverMessage) else null)

        Button(onClick = actions.onSubmit, enabled = enabled, modifier = Modifier.fillMaxWidth()) {
            if (state.isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(stringResource(R.string.action_register))
            }
        }
        TextButton(onClick = actions.onGoLogin, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_go_login))
        }
    }

    if (showDatePicker) {
        BirthDatePickerDialog(
            initial = state.birthDate,
            onConfirm = {
                actions.onBirthDateChange(it)
                showDatePicker = false
            },
            onDismiss = { showDatePicker = false },
        )
    }
    if (showPrivacy) {
        AlertDialog(
            onDismissRequest = { showPrivacy = false },
            title = { Text(stringResource(R.string.privacy_title)) },
            text = {
                Text(
                    stringResource(R.string.privacy_body),
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                )
            },
            confirmButton = { TextButton(onClick = { showPrivacy = false }) { Text(stringResource(R.string.action_ok)) } },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BirthDatePickerDialog(initial: LocalDate?, onConfirm: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val today = LocalDate.now()
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        yearRange = (today.year - 100)..today.year,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    pickerState.selectedDateMillis?.let {
                        onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
                enabled = pickerState.selectedDateMillis != null,
            ) { Text(stringResource(R.string.action_ok)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    ) {
        DatePicker(state = pickerState)
    }
}

private val DISPLAY_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")

@Preview(showBackground = true, heightDp = 1000)
@Composable
private fun RegisterScreenPreview() {
    SosyalTheme {
        RegisterScreen(
            state = RegisterUiState(
                username = "ornek",
                fieldErrors = mapOf(RegisterField.PASSWORD to R.string.validation_password),
            ),
            actions = RegisterActions({}, {}, {}, {}, {}, {}, {}, {}),
        )
    }
}
