package com.proje.sosyal.android.presentation.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.proje.sosyal.android.R
import com.proje.sosyal.android.core.ui.theme.Spacing

/** Formlar geniş ekranlarda ortalanır ve okunabilir genişlikte kalır; klavye açılınca kaydırılabilir. */
@Composable
internal fun AuthFormContainer(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .fillMaxWidth()
                .padding(horizontal = Spacing.lg, vertical = Spacing.xl),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) { content() }
    }
}

@Composable
internal fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    errorText: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = errorText != null,
        supportingText = (errorText ?: supportingText)?.let { text -> { Text(text) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
internal fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    errorText: String? = null,
    imeAction: ImeAction = ImeAction.Done,
    enabled: Boolean = true,
) {
    var visible by rememberSaveable { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        isError = errorText != null,
        supportingText = (errorText ?: supportingText)?.let { text -> { Text(text) } },
        visualTransformation = if (visible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = imeAction, autoCorrectEnabled = false),
        trailingIcon = {
            TextButton(onClick = { visible = !visible }) {
                Text(stringResource(if (visible) R.string.action_hide_password else R.string.action_show_password))
            }
        },
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
internal fun FormError(message: String?) {
    if (message != null) {
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
    }
}
