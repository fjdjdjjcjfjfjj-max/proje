package com.proje.sosyal.android.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.proje.sosyal.android.R
import androidx.compose.ui.unit.dp
import com.proje.sosyal.android.presentation.editprofile.EditProfileRouteScreen
import com.proje.sosyal.android.presentation.home.HomeRouteScreen
import com.proje.sosyal.android.presentation.placeholder.NotImplementedScreen
import com.proje.sosyal.android.presentation.profile.ProfileRouteScreen
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState

/**
 * Uygulama iskeleti: içerik tüm ekranı kaplar, yüzen cam alt çubuk içeriğin üzerinde durur.
 * Geniş ekranlarda çubuk ortalanır ve genişliği sınırlanır.
 */
@Composable
fun SosyalAppShell(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val selected = TopLevelDestination.entries.firstOrNull { destination ->
        currentDestination?.hierarchy?.any { it.hasRoute(destination.route::class) } == true
    }
    val hazeState = rememberHazeState()
    // Tam ekran alt sayfalarda (profil düzenleme) alt çubuk gizlenir.
    val showBar = currentDestination?.hasRoute(EditProfileRoute::class) != true

    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(LocalBottomBarInset provides if (showBar) bottomBarInset() else 0.dp) {
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                modifier = Modifier
                    .fillMaxSize()
                    // Çubuğun arkasında bulanıklaştırılacak içerik
                    .hazeSource(hazeState)
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding(),
            ) {
                composable<HomeRoute> { HomeRouteScreen() }
                composable<ExploreRoute> { NotImplementedScreen(stringResource(R.string.nav_explore)) }
                composable<CreatePostRoute> { NotImplementedScreen(stringResource(R.string.nav_create)) }
                composable<NotificationsRoute> { NotImplementedScreen(stringResource(R.string.nav_notifications)) }
                composable<ProfileRoute> { ProfileRouteScreen(onEditProfile = { navController.navigate(EditProfileRoute) }) }
                composable<EditProfileRoute> { EditProfileRouteScreen(onBack = { navController.popBackStack() }) }
            }
        }

        if (showBar) {
            FloatingNavBar(
                destinations = TopLevelDestination.entries,
                selected = selected,
                onSelect = navController::navigateToTopLevel,
                hazeState = hazeState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
