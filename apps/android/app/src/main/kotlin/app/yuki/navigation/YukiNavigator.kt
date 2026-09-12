package app.yuki.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import app.yuki.core.model.ListingCategory

internal class YukiNavigator(
    private val navController: NavHostController,
    private val onListingOpened: () -> Unit,
) {
    fun selectTab(tab: YukiTab) {
        navController.navigate(tab.startRoute()) {
            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openListing(slug: String) {
        onListingOpened()
        navController.navigate(ListingRoute(slug))
    }

    fun openSettings() {
        navController.navigate(SettingsRoute)
    }

    fun openCategory(category: ListingCategory) {
        navController.navigate(CategoryRoute(category = category.wireValue))
    }

    fun openScreenshots(slug: String, startIndex: Int) {
        navController.navigate(ScreenshotRoute(slug = slug, startIndex = startIndex))
    }

    fun navigateUp() {
        navController.navigateUp()
    }
}

@Composable
internal fun rememberYukiNavigator(
    navController: NavHostController,
    onListingOpened: () -> Unit,
): YukiNavigator {
    val currentCallback by rememberUpdatedState(onListingOpened)

    return remember(navController) {
        YukiNavigator(navController) { currentCallback() }
    }
}
