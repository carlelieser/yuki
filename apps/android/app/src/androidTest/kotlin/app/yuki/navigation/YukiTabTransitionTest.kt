package app.yuki.navigation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.NavHostController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private const val EXPLORE_TAG = "exploreStub"
private const val LIBRARY_TAG = "libraryStub"
private const val LISTING_TAG = "listingStub"
private const val SETTLE_MILLIS = 1_000L
private const val MID_TRANSITION_MILLIS = 90L

class YukiTabTransitionTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private lateinit var navController: NavHostController

    private fun setContent() {
        composeRule.setContent {
            navController = rememberNavController()

            NavHost(
                navController = navController,
                startDestination = ExploreRoute,
                modifier = Modifier.fillMaxSize(),
                enterTransition = { forwardEnter() },
                exitTransition = { forwardExit() },
                popEnterTransition = { backEnter() },
                popExitTransition = { backExit() },
            ) {
                tabStub<ExploreRoute>(EXPLORE_TAG)
                tabStub<LibraryRoute>(LIBRARY_TAG)
                composable<ListingRoute> {
                    Text(text = "listing", modifier = Modifier.testTag(LISTING_TAG))
                }
            }
        }
        composeRule.mainClock.autoAdvance = false
        settle()
    }

    private inline fun <reified T : Any> NavGraphBuilder.tabStub(tag: String) {
        composable<T>(
            enterTransition = { tabEnter() },
            exitTransition = { tabExit() },
            popEnterTransition = { tabPopEnter() },
            popExitTransition = { tabPopExit() },
        ) {
            Text(text = tag, modifier = Modifier.testTag(tag))
        }
    }

    private fun settle() {
        composeRule.mainClock.advanceTimeBy(SETTLE_MILLIS)
        composeRule.waitForIdle()
    }

    private fun navigate(block: () -> Unit) {
        composeRule.runOnUiThread(block)
        composeRule.waitForIdle()
    }

    private fun currentRouteIsExplore(): Boolean =
        navController.currentDestination?.route?.contains("ExploreRoute") == true

    private fun exploreLeftEdge() =
        composeRule.onNodeWithTag(EXPLORE_TAG).getUnclippedBoundsInRoot().left

    @Test
    fun poppingTheListingSlidesExploreBackIn() {
        setContent()
        val restingLeft = exploreLeftEdge()

        navigate { navController.navigate(ListingRoute("ghost")) }
        settle()
        composeRule.onNodeWithTag(LISTING_TAG).assertIsDisplayed()

        navigate { navController.popBackStack() }
        composeRule.mainClock.advanceTimeBy(MID_TRANSITION_MILLIS)

        val slidOffset = exploreLeftEdge() - restingLeft
        assertTrue(
            "Explore should slide horizontally while the listing pops, was $slidOffset",
            slidOffset.value != 0f,
        )
    }

    @Test
    fun poppingTheListingLandsBackOnExplore() {
        setContent()

        navigate { navController.navigate(ListingRoute("ghost")) }
        settle()

        navigate { navController.popBackStack() }
        settle()

        composeRule.onNodeWithTag(EXPLORE_TAG).assertIsDisplayed()
        assertTrue(currentRouteIsExplore())
    }

    @Test
    fun movingBetweenTabsKeepsExploreInPlace() {
        setContent()
        val restingLeft = exploreLeftEdge()

        navigate { navController.navigate(LibraryRoute) }
        composeRule.mainClock.advanceTimeBy(MID_TRANSITION_MILLIS)

        assertEquals(restingLeft.value, exploreLeftEdge().value, 0.5f)
    }
}
