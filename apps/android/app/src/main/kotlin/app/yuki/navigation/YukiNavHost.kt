package app.yuki.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import app.yuki.feature.explore.ExploreRoute as ExploreScreenRoute
import app.yuki.feature.library.LibraryScreen
import app.yuki.feature.listing.ListingRoute as ListingScreenRoute
import app.yuki.feature.updates.UpdatesScreen
import app.yuki.settings.SettingsDestination

@Composable
internal fun YukiNavHost(
    navController: NavHostController,
    navigator: YukiNavigator,
    bottomBarPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = ExploreRoute,
        modifier = modifier,
        enterTransition = { forwardEnter() },
        exitTransition = { forwardExit() },
        popEnterTransition = { backEnter() },
        popExitTransition = { backExit() },
    ) {
        exploreDestination(navigator, bottomBarPadding)
        libraryDestination(navigator, bottomBarPadding)
        updatesDestination(navigator, bottomBarPadding)
        listingDestination(navigator)
        settingsDestination(navigator)
    }
}

private fun NavGraphBuilder.exploreDestination(
    navigator: YukiNavigator,
    bottomBarPadding: PaddingValues,
) {
    composable<ExploreRoute>(
        enterTransition = { tabEnter() },
        exitTransition = { tabExit() },
        popEnterTransition = { tabEnter() },
        popExitTransition = { tabExit() },
    ) {
        ExploreScreenRoute(
            onListingSelected = navigator::openListing,
            onSettingsClick = navigator::openSettings,
            contentPadding = bottomBarPadding,
        )
    }
}

private fun NavGraphBuilder.libraryDestination(
    navigator: YukiNavigator,
    bottomBarPadding: PaddingValues,
) {
    composable<LibraryRoute>(
        enterTransition = { tabEnter() },
        exitTransition = { tabExit() },
        popEnterTransition = { tabEnter() },
        popExitTransition = { tabExit() },
    ) {
        LibraryScreen(
            onListingClick = navigator::openListing,
            onExploreClick = { navigator.selectTab(YukiTab.Explore) },
            contentPadding = bottomBarPadding,
        )
    }
}

private fun NavGraphBuilder.updatesDestination(
    navigator: YukiNavigator,
    bottomBarPadding: PaddingValues,
) {
    composable<UpdatesRoute>(
        enterTransition = { tabEnter() },
        exitTransition = { tabExit() },
        popEnterTransition = { tabEnter() },
        popExitTransition = { tabExit() },
    ) {
        UpdatesScreen(
            onListingClick = navigator::openListing,
            contentPadding = bottomBarPadding,
        )
    }
}

private fun NavGraphBuilder.listingDestination(navigator: YukiNavigator) {
    composable<ListingRoute> { entry ->
        ListingScreenRoute(
            slug = entry.toRoute<ListingRoute>().slug,
            onBackClick = navigator::navigateUp,
        )
    }
}

private fun NavGraphBuilder.settingsDestination(navigator: YukiNavigator) {
    composable<SettingsRoute> {
        SettingsDestination(onBackClick = navigator::navigateUp)
    }
}
