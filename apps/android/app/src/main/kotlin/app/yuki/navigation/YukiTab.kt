package app.yuki.navigation

import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.component.YukiNavDestination
import kotlin.reflect.KClass

internal enum class YukiTab(
    val label: String,
    val icon: ImageVector,
    val route: KClass<*>,
) {
    Explore("Explore", YukiTabIcons.Explore, ExploreRoute::class),
    Browse("Browse", YukiIcons.Search, SearchRoute::class),
    Library("Library", YukiTabIcons.Library, LibraryRoute::class),
    Updates("Updates", YukiTabIcons.Updates, UpdatesRoute::class),
}

internal fun YukiTab.toNavDestination(): YukiNavDestination = YukiNavDestination(
    label = label,
    icon = { Icon(imageVector = icon, contentDescription = label) },
)

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
