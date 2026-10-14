package com.proje.sosyal.android.presentation.placeholder

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.proje.sosyal.android.R
import com.proje.sosyal.android.core.ui.components.EmptyView

/**
 * Henüz geliştirilmemiş sekmeler için dürüst bilgilendirme ekranı.
 * Sahte içerik göstermez; özelliğin hazır olmadığını açıkça belirtir.
 */
@Composable
fun NotImplementedScreen(featureName: String, modifier: Modifier = Modifier) {
    EmptyView(
        title = stringResource(R.string.coming_soon_title, featureName),
        message = stringResource(R.string.coming_soon_message),
        modifier = modifier,
    )
}
