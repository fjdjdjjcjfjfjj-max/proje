package com.proje.sosyal.android.core.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.proje.sosyal.android.core.common.AppError
import com.proje.sosyal.android.core.ui.theme.SosyalTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StateViewsTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun agHatasindaMesajVeTekrarDeneButonuGosterilir() {
        var retryCount = 0
        composeRule.setContent {
            SosyalTheme { ErrorView(error = AppError.NETWORK, onRetry = { retryCount++ }) }
        }

        composeRule.onNodeWithText("Bağlantı kurulamadı").assertIsDisplayed()
        composeRule.onNodeWithText("Tekrar dene").assertIsDisplayed().performClick()

        assertEquals(1, retryCount)
    }

    @Test
    fun bosDurumBaslikVeAciklamaGosterir() {
        composeRule.setContent {
            SosyalTheme { EmptyView(title = "Boş başlık", message = "Boş açıklama") }
        }

        composeRule.onNodeWithText("Boş başlık").assertIsDisplayed()
        composeRule.onNodeWithText("Boş açıklama").assertIsDisplayed()
    }
}
