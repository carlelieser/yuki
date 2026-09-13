package app.yuki.navigation

import android.content.Context
import android.net.Uri
import androidx.navigation.NavController
import androidx.navigation.NavDeepLinkRequest
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

private const val SLUG = "nightsky-aurora"

@RunWith(AndroidJUnit4::class)
class ListingDeepLinkTest {
    @Test
    fun aListingLinkOpensThatListing() {
        val match = match("https://yukistore.org/listings/$SLUG")

        assertNotNull("Expected a listing link to be handled by the app", match)
        assertEquals(true, match?.destination?.hasRoute<ListingRoute>())
        assertEquals(SLUG, match?.matchingArgs?.getString("slug"))
    }

    @Test
    fun aDownloadLinkOpensTheListingItBelongsTo() {
        val match = match("https://yukistore.org/listings/$SLUG/download/v1.2.0")

        assertNotNull("Expected a download link to be handled by the app", match)
        assertEquals(true, match?.destination?.hasRoute<ListingRoute>())
        assertEquals(SLUG, match?.matchingArgs?.getString("slug"))
    }

    @Test
    fun aLinkToAnotherSectionIsLeftToTheBrowser() {
        assertNull(match("https://yukistore.org/browse"))
    }

    @Test
    fun aLinkToAnotherSiteIsLeftToTheBrowser() {
        assertNull(match("https://example.com/listings/$SLUG"))
    }

    private fun match(url: String): NavDestination.DeepLinkMatch? {
        val request = NavDeepLinkRequest.Builder.fromUri(Uri.parse(url)).build()
        return graph().matchDeepLink(request)
    }

    private fun graph(): NavGraph {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val controller = NavController(context).apply {
            navigatorProvider.addNavigator(ComposeNavigator())
        }

        return controller.createGraph(startDestination = ExploreRoute) {
            composable<ExploreRoute> { }
            composable<ListingRoute>(deepLinks = listingDeepLinks()) { }
        }
    }
}
