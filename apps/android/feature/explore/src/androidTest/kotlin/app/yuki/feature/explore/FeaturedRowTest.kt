package app.yuki.feature.explore

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import app.yuki.core.designsystem.theme.LocalReduceMotion
import app.yuki.core.designsystem.theme.LocalTouchExploration
import app.yuki.core.designsystem.theme.YukiMotion
import app.yuki.core.model.ListingSummary
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

private const val SETTLE_MILLIS = 1_000L

private val ADVANCE_WITH_SETTLE_MILLIS = YukiMotion.CarouselAdvanceMillis + SETTLE_MILLIS

class FeaturedRowTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var settledIndex = 0

    private fun render(
        listings: List<ListingSummary>,
        isReduceMotion: Boolean = false,
        isTouchExploration: Boolean = false,
    ) {
        composeRule.setContent {
            CompositionLocalProvider(
                LocalReduceMotion provides isReduceMotion,
                LocalTouchExploration provides isTouchExploration,
            ) {
                FeaturedRow(
                    listings = listings,
                    onSelect = {},
                    onSettledIndexChanged = { index -> settledIndex = index },
                )
            }
        }
    }

    @Test
    fun theCarouselAdvancesOnItsOwn() {
        composeRule.mainClock.autoAdvance = false

        render(listOf(listing("alpha"), listing("beta"), listing("gamma")))
        composeRule.mainClock.advanceTimeBy(ADVANCE_WITH_SETTLE_MILLIS)

        assertEquals(1, settledIndex)
    }

    @Test
    fun theCarouselHoldsStillWhileTheUserIsTouching() {
        composeRule.mainClock.autoAdvance = false

        render(listOf(listing("alpha"), listing("beta"), listing("gamma")))

        composeRule.onNodeWithTag(FEATURED_ROW_TAG).performTouchInput { down(center) }
        composeRule.mainClock.advanceTimeBy(ADVANCE_WITH_SETTLE_MILLIS)

        assertEquals(0, settledIndex)
    }

    @Test
    fun reduceMotionStopsTheCarouselAdvancing() {
        composeRule.mainClock.autoAdvance = false

        render(
            listOf(listing("alpha"), listing("beta"), listing("gamma")),
            isReduceMotion = true,
        )
        composeRule.mainClock.advanceTimeBy(ADVANCE_WITH_SETTLE_MILLIS)

        assertEquals(0, settledIndex)
    }

    @Test
    fun talkBackStopsTheCarouselAdvancing() {
        composeRule.mainClock.autoAdvance = false

        render(
            listOf(listing("alpha"), listing("beta"), listing("gamma")),
            isTouchExploration = true,
        )
        composeRule.mainClock.advanceTimeBy(ADVANCE_WITH_SETTLE_MILLIS)

        assertEquals(0, settledIndex)
    }
}

private fun listing(slug: String) = ListingSummary(
    id = slug,
    githubRepoId = slug.hashCode().toLong(),
    slug = slug,
    title = slug.replaceFirstChar(Char::uppercase),
    author = "yuki",
    description = null,
    iconUrl = null,
    bannerUrl = null,
    category = null,
    stars = 1,
)
