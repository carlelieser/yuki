package app.yuki.navigation

import androidx.compose.foundation.layout.padding
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
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = ExploreRoute,
        modifier = modifier,
    ) {
        exploreDestination(navigator)
        libraryDestination(navigator)
        updatesDestination(navigator)
        listingDestination()
        settingsDestination(navigator)
    }
}

private fun NavGraphBuilder.exploreDestination(navigator: YukiNavigator) {
    composable<ExploreRoute> {
        ExploreTopBarScaffold(onSettingsClick = navigator::openSettings) { contentPadding ->
            ExploreScreenRoute(
                onListingSelected = navigator::openListing,
                modifier = Modifier.padding(contentPadding),
            )
        }
    }
}

private fun NavGraphBuilder.libraryDestination(navigator: YukiNavigator) {
    composable<LibraryRoute> {
        LibraryScreen(
            onListingClick = navigator::openListing,
            onExploreClick = { navigator.selectTab(YukiTab.Explore) },
        )
    }
}

private fun NavGraphBuilder.updatesDestination(navigator: YukiNavigator) {
    composable<UpdatesRoute> {
        UpdatesScreen(onListingClick = navigator::openListing)
    }
}

private fun NavGraphBuilder.listingDestination() {
    composable<ListingRoute> { entry ->
        ListingScreenRoute(slug = entry.toRoute<ListingRoute>().slug)
    }
}

private fun NavGraphBuilder.settingsDestination(navigator: YukiNavigator) {
    composable<SettingsRoute> {
        SettingsDestination(onBackClick = navigator::navigateUp)
    }
}
