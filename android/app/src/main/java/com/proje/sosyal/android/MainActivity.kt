package com.proje.sosyal.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.proje.sosyal.android.core.ui.theme.SosyalTheme
import com.proje.sosyal.android.navigation.SosyalAppShell
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SosyalTheme {
                SosyalAppShell()
            }
        }
    }
}
