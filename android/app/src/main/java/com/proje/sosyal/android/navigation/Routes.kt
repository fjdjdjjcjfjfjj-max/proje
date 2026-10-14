package com.proje.sosyal.android.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.ui.graphics.vector.ImageVector
import com.proje.sosyal.android.R
import kotlinx.serialization.Serializable

/** Tür güvenli Navigation Compose rotaları. */
@Serializable data object HomeRoute
@Serializable data object ExploreRoute
@Serializable data object CreatePostRoute
@Serializable data object NotificationsRoute
@Serializable data object ProfileRoute

/** Seçiliyken dolu, değilken çizgi simge (iOS tarzı). */
enum class TopLevelDestination(
    val route: Any,
    @param:StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    HOME(HomeRoute, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home),
    EXPLORE(ExploreRoute, R.string.nav_explore, Icons.Filled.Search, Icons.Outlined.Search),
    CREATE(CreatePostRoute, R.string.nav_create, Icons.Filled.Add, Icons.Filled.Add),
    NOTIFICATIONS(NotificationsRoute, R.string.nav_notifications, Icons.Filled.Notifications, Icons.Outlined.Notifications),
    PROFILE(ProfileRoute, R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person),
}
