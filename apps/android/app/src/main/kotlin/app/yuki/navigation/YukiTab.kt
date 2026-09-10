package app.yuki.navigation

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.component.YukiNavDestination
import kotlin.reflect.KClass

internal enum class YukiTab(
    val label: String,
    val route: KClass<*>,
) {
    Explore("Explore", ExploreRoute::class),
    Browse("Browse", SearchRoute::class),
    Library("Library", LibraryRoute::class),
    Updates("Updates", UpdatesRoute::class),
}

@Composable
private fun YukiTab.icon(): ImageVector = when (this) {
    YukiTab.Explore -> YukiIcons.Explore
    YukiTab.Browse -> YukiIcons.GridView
    YukiTab.Library -> YukiIcons.Library
    YukiTab.Updates -> YukiIcons.Update
}

@Composable
internal fun YukiTab.toNavDestination(): YukiNavDestination {
    val icon = icon()
    return YukiNavDestination(
        label = label,
        icon = { Icon(imageVector = icon, contentDescription = label) },
    )
}

internal fun YukiTab.startRoute(): Any = when (this) {
    YukiTab.Explore -> ExploreRoute
    YukiTab.Browse -> SearchRoute
    YukiTab.Library -> LibraryRoute
    YukiTab.Updates -> UpdatesRoute
}

internal fun NavDestination?.selectedTab(): YukiTab? =
    this?.let { destination ->
        YukiTab.entries.firstOrNull { tab -> destination.isInTab(tab) }
    }

private fun NavDestination.isInTab(tab: YukiTab): Boolean =
    hierarchy.any { ancestor -> ancestor.hasRoute(tab.route) }
