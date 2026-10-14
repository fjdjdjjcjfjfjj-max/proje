package com.proje.sosyal.android.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.proje.sosyal.android.presentation.home.HomeRouteScreen
import com.proje.sosyal.android.presentation.placeholder.NotImplementedScreen
import com.proje.sosyal.android.presentation.profile.ProfileRouteScreen

/**
 * Uygulama iskeleti. NavigationSuiteScaffold telefonlarda alt navigasyon çubuğu,
 * tablet/geniş ekranlarda yan navigasyon rayı gösterir.
 */
@Composable
fun SosyalAppShell(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    // navigationSuiteItems composable bir kapsam değildir; etiketler önceden çözülür.
    val labels = TopLevelDestination.entries.associateWith { stringResource(it.labelRes) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            TopLevelDestination.entries.forEach { destination ->
                val selected = currentDestination?.hierarchy?.any { it.hasRoute(destination.route::class) } == true
                val label = labels.getValue(destination)
                if (destination == TopLevelDestination.CREATE) {
                    // TikTok tarzı: ortada yazısız, belirgin "+" düğmesi. Seçim göstergesi düğmenin kendisidir.
                    item(
                        selected = false,
                        onClick = { navController.navigateToTopLevel(destination) },
                        icon = { CreateButton(contentDescription = label) },
                        label = null,
                    )
                } else {
                    item(
                        selected = selected,
                        onClick = { navController.navigateToTopLevel(destination) },
                        icon = { Icon(destination.icon, contentDescription = label) },
                        label = { Text(label, maxLines = 1) },
                    )
                }
            }
        },
    ) {
        Scaffold { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = HomeRoute,
                modifier = Modifier.padding(innerPadding),
            ) {
                composable<HomeRoute> { HomeRouteScreen() }
                composable<ExploreRoute> { NotImplementedScreen(stringResource(R.string.nav_explore)) }
                composable<CreatePostRoute> { NotImplementedScreen(stringResource(R.string.nav_create)) }
                composable<NotificationsRoute> { NotImplementedScreen(stringResource(R.string.nav_notifications)) }
                composable<ProfileRoute> { ProfileRouteScreen() }
            }
        }
    }
}

@Composable
private fun CreateButton(contentDescription: String) {
    Box(
        modifier = Modifier
            .size(width = 52.dp, height = 34.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Add,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(26.dp),
        )
    }
}

private fun NavHostController.navigateToTopLevel(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
