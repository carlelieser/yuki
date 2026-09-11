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
    Search("Search", SearchRoute::class),
    Library("Library", LibraryRoute::class),
    Updates("Updates", UpdatesRoute::class),
}

@Composable
private fun YukiTab.icon(): ImageVector = when (this) {
    YukiTab.Explore -> YukiIcons.Explore
    YukiTab.Search -> YukiIcons.Search
    YukiTab.Library -> YukiIcons.GridView
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
    YukiTab.Search -> SearchRoute
    YukiTab.Library -> LibraryRoute
    YukiTab.Updates -> UpdatesRoute
}

internal fun NavDestination?.selectedTab(): YukiTab? =
    this?.let { destination ->
        YukiTab.entries.firstOrNull { tab -> destination.isInTab(tab) }
    }

private fun NavDestination.isInTab(tab: YukiTab): Boolean =
    hierarchy.any { ancestor -> ancestor.hasRoute(tab.route) }
