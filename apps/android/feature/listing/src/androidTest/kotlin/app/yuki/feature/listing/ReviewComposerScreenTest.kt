package app.yuki.feature.listing

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import app.yuki.core.designsystem.R as DesignR
import app.yuki.core.designsystem.component.FAILURE_STATE_TAG
import app.yuki.core.designsystem.component.LocalYukiSnackbarHostState
import app.yuki.core.designsystem.component.YUKI_SNACKBAR_TAG
import app.yuki.core.designsystem.component.YukiSnackbarHost
import app.yuki.core.model.FailureReason
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ReviewComposerScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun text(@StringRes id: Int): String =
        InstrumentationRegistry.getInstrumentation().targetContext.resources.getString(id)

    private fun stars(count: Int): String = InstrumentationRegistry.getInstrumentation().targetContext
        .resources.getQuantityString(DesignR.plurals.designsystem_rating_stars, count, count)

    private fun setScreen(
        state: ReviewComposerState,
        callbacks: ReviewComposerCallbacks = ReviewComposerCallbacks(),
    ) {
        val hostState = SnackbarHostState()

        composeRule.setContent {
            CompositionLocalProvider(LocalYukiSnackbarHostState provides hostState) {
                Box {
                    ReviewComposerScreen(state = state, callbacks = callbacks)
                    YukiSnackbarHost(hostState = hostState)
                }
            }
        }
    }

    @Test
    fun aNewReviewOffersPostWithoutDelete() {
        setScreen(ReviewComposerState(isLoading = false))

        composeRule.onNodeWithText(text(R.string.listing_composer_title_write)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.listing_composer_post)).assertIsDisplayed()
        composeRule.onAllNodesWithText(text(R.string.listing_composer_delete)).assertCountEquals(0)
    }

    @Test
    fun anExistingReviewOffersUpdateAndDelete() {
        setScreen(ReviewComposerState(isLoading = false, isEditing = true, rating = 3, body = "Okay"))

        composeRule.onNodeWithText(text(R.string.listing_composer_title_edit)).assertIsDisplayed()
        composeRule.onNodeWithText("Okay").assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.listing_composer_update)).assertIsDisplayed()
        composeRule.onNodeWithText(text(R.string.listing_composer_delete)).assertIsDisplayed()
    }

    @Test
    fun tappingAStarAndPostingReachTheCallbacks() {
        val ratings = mutableListOf<Int>()
        var submitCount = 0
        setScreen(
            state = ReviewComposerState(isLoading = false),
            callbacks = ReviewComposerCallbacks(
                onRatingChange = { ratings.add(it) },
                onSubmit = { submitCount += 1 },
            ),
        )

        composeRule.onNodeWithContentDescription(stars(4)).performClick()
        composeRule.onNodeWithText(text(R.string.listing_composer_post)).performClick()

        assertEquals(listOf(4), ratings)
        assertEquals(1, submitCount)
    }

    @Test
    fun asksForARatingWhenMissing() {
        setScreen(ReviewComposerState(isLoading = false, isRatingMissing = true))

        composeRule.onNodeWithText(text(R.string.listing_composer_rating_required)).assertIsDisplayed()
    }

    @Test
    fun reportsARefusalInTheSnackbarInsteadOfInline() {
        var shownCount = 0
        setScreen(
            state = ReviewComposerState(
                isLoading = false,
                failure = FailureReason.Rejected("Download this app before reviewing it."),
            ),
            callbacks = ReviewComposerCallbacks(onFailureShown = { shownCount += 1 }),
        )

        composeRule.onNode(
            hasText("Download this app before reviewing it.") and
                hasAnyAncestor(hasTestTag(YUKI_SNACKBAR_TAG)),
        ).assertIsDisplayed()
        composeRule.onAllNodesWithTag(FAILURE_STATE_TAG).assertCountEquals(0)
    }

    @Test
    fun confirmingDeletionReachesTheCallback() {
        var confirmCount = 0
        setScreen(
            state = ReviewComposerState(isLoading = false, isEditing = true, rating = 3, isConfirmingDelete = true),
            callbacks = ReviewComposerCallbacks(onDeleteConfirmed = { confirmCount += 1 }),
        )

        composeRule.onNodeWithTag(REVIEW_DELETE_DIALOG_TAG).assertIsDisplayed()
        composeRule.onNode(
            hasText(text(R.string.listing_composer_delete_confirm)) and
                hasAnyAncestor(hasTestTag(REVIEW_DELETE_DIALOG_TAG)),
        ).performClick()

        assertEquals(1, confirmCount)
    }
}
