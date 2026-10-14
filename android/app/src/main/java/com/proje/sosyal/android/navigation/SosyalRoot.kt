package com.proje.sosyal.android.navigation

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.proje.sosyal.android.core.session.SessionState
import com.proje.sosyal.android.core.ui.components.LoadingView
import com.proje.sosyal.android.presentation.auth.LoginRoute
import com.proje.sosyal.android.presentation.auth.RegisterRoute
import com.proje.sosyal.android.presentation.update.UpdatePrompt
import kotlinx.serialization.Serializable

@Serializable data object LoginDestination
@Serializable data object RegisterDestination

/**
 * Oturum durumuna göre giriş akışını veya ana uygulamayı gösterir. Oturum kapandığında
 * (çıkış ya da geçersiz yenileme belirteci) kullanıcı kendiliğinden giriş ekranına döner.
 */
@Composable
fun SosyalRoot(viewModel: SessionViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (state) {
        SessionState.Loading -> LoadingView()
        SessionState.LoggedOut -> AuthNavHost()
        SessionState.LoggedIn -> SosyalAppShell()
    }
    UpdatePrompt()
}

@Composable
private fun AuthNavHost() {
    val navController = rememberNavController()
    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = LoginDestination,
            modifier = Modifier.safeDrawingPadding(),
        ) {
            composable<LoginDestination> {
                LoginRoute(onGoRegister = { navController.navigate(RegisterDestination) { launchSingleTop = true } })
            }
            composable<RegisterDestination> {
                RegisterRoute(onGoLogin = {
                    if (!navController.popBackStack()) navController.navigate(LoginDestination)
                })
            }
        }
    }
}
