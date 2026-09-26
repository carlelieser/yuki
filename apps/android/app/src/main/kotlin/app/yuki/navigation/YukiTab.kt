package app.yuki.navigation

import androidx.annotation.StringRes
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import app.yuki.R
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.component.YukiNavDestination
import kotlin.reflect.KClass

internal enum class YukiTab(
    @StringRes val label: Int,
    val route: KClass<*>,
) {
    Explore(R.string.app_tab_explore, ExploreRoute::class),
    Search(R.string.app_tab_search, SearchRoute::class),
    Library(R.string.app_tab_library, LibraryRoute::class),
    Updates(R.string.app_tab_updates, UpdatesRoute::class),
}

@Composable
private fun YukiTab.icon(): ImageVector = when (this) {
    YukiTab.Explore -> YukiIcons.Explore
    YukiTab.Search -> YukiIcons.Search
    YukiTab.Library -> YukiIcons.GridView
    YukiTab.Updates -> YukiIcons.Update
}

@Composable
internal fun YukiTab.toNavDestination(badgeCount: Int = 0): YukiNavDestination {
    val icon = icon()
    val label = stringResource(label)
    val description = if (badgeCount > 0) {
        pluralStringResource(R.plurals.app_tab_updates_pending, badgeCount, label, badgeCount)
    } else {
        label
    }

    return YukiNavDestination(
        label = label,
        icon = { Icon(imageVector = icon, contentDescription = description) },
        badgeCount = badgeCount,
    )
}

internal fun YukiTab.badgeCountFrom(pendingUpdates: Int): Int =
    if (this == YukiTab.Updates) pendingUpdates else 0

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
