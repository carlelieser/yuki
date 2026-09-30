package app.yuki.feature.listing

import androidx.annotation.StringRes
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import app.yuki.core.designsystem.R as DesignR
import app.yuki.core.model.FailureReason
import app.yuki.core.model.UiState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ListingReviewsSectionTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun text(@StringRes id: Int, vararg args: Any): String =
        InstrumentationRegistry.getInstrumentation().targetContext.resources.getString(id, *args)

    private fun setReviews(state: ReviewSectionState, callbacks: ReviewCallbacks = ReviewCallbacks()) {
        val base = noopCallbacks()

        composeRule.setContent {
            ListingScreen(
                state = ListingScreenState(
                    listing = UiState.Success(detail(versions = emptyList()).toUiModel()),
                    installStatus = idleStatus(),
                    reviews = state,
                ),
                callbacks = base.copy(callbacks = base.callbacks.copy(reviews = callbacks)),
                onBackClick = {},
            )
        }
    }

    private fun loaded(count: Int, hasMore: Boolean = false): UiState<ListingReviews> {
        val reviews = (1..count).map { index -> review("r$index") }
        return UiState.Success(
            ListingReviews(summary = ratingSummary(total = count), preview = reviews, hasMore = hasMore),
        )
    }

    private fun scrollTo(label: String) {
        composeRule.onNode(hasTestTag(LISTING_DETAIL_TAG)).performScrollToNode(hasText(label))
    }

    @Test
    fun showsTheSummaryAndPreviewedReviews() {
        setReviews(ReviewSectionState(reviews = loaded(count = 3)))

        composeRule.onNode(hasTestTag(LISTING_DETAIL_TAG)).performScrollToNode(hasTestTag(RATING_SUMMARY_TAG))
        scrollTo("Review r3")
        composeRule.onNodeWithText("Review r3").assertIsDisplayed()
    }

    @Test
    fun invitesTheFirstReviewWhenThereAreNone() {
        setReviews(ReviewSectionState(reviews = loaded(count = 0), prompt = ReviewPrompt.Write))

        scrollTo(text(R.string.listing_reviews_empty_description))
        composeRule.onNodeWithText(text(R.string.listing_reviews_empty_description)).assertIsDisplayed()
        composeRule.onAllNodesWithTag(RATING_SUMMARY_TAG).assertCountEquals(0)
    }

    @Test
    fun seeAllAppearsOnlyWhenMoreReviewsExist() {
        var openCount = 0
        val seeAll = text(DesignR.string.designsystem_section_see_all, text(R.string.listing_section_reviews))
        setReviews(
            state = ReviewSectionState(reviews = loaded(count = 3, hasMore = true)),
            callbacks = ReviewCallbacks(onSeeAll = { openCount += 1 }),
        )

        scrollTo(text(R.string.listing_section_reviews))
        composeRule.onNodeWithContentDescription(seeAll).performClick()

        assertEquals(1, openCount)
    }

    @Test
    fun offersNoSeeAllForAShortList() {
        setReviews(ReviewSectionState(reviews = loaded(count = 2)))

        scrollTo(text(R.string.listing_section_reviews))
        composeRule.onAllNodesWithContentDescription(
            text(DesignR.string.designsystem_section_see_all, text(R.string.listing_section_reviews)),
        ).assertCountEquals(0)
    }

    @Test
    fun aSignedOutViewerIsSentToSignIn() {
        var signInCount = 0
        setReviews(
            state = ReviewSectionState(reviews = loaded(count = 1), prompt = ReviewPrompt.SignIn),
            callbacks = ReviewCallbacks(onSignIn = { signInCount += 1 }),
        )

        scrollTo(text(R.string.listing_reviews_sign_in))
        composeRule.onNodeWithText(text(R.string.listing_reviews_sign_in)).performClick()

        assertEquals(1, signInCount)
    }

    @Test
    fun anUninstalledAppAsksForAnInstallFirst() {
        setReviews(ReviewSectionState(reviews = loaded(count = 1), prompt = ReviewPrompt.InstallRequired))

        scrollTo(text(R.string.listing_reviews_install_required))
        composeRule.onAllNodesWithText(text(R.string.listing_reviews_write)).assertCountEquals(0)
    }

    @Test
    fun anInstalledAppOpensTheComposer() {
        var writeCount = 0
        setReviews(
            state = ReviewSectionState(reviews = loaded(count = 1), prompt = ReviewPrompt.Write),
            callbacks = ReviewCallbacks(onWrite = { writeCount += 1 }),
        )

        scrollTo(text(R.string.listing_reviews_write))
        composeRule.onNodeWithText(text(R.string.listing_reviews_write)).performClick()

        assertEquals(1, writeCount)
    }

    @Test
    fun theViewersReviewIsShownOnceWithAnEditAction() {
        var editCount = 0
        val own = review("r1")
        setReviews(
            state = ReviewSectionState(reviews = loaded(count = 2), prompt = ReviewPrompt.Edit(own)),
            callbacks = ReviewCallbacks(onWrite = { editCount += 1 }),
        )

        scrollTo("Review r2")
        composeRule.onAllNodesWithText("Review r1").assertCountEquals(1)
        composeRule.onNodeWithContentDescription(text(R.string.listing_reviews_edit)).performClick()

        assertEquals(1, editCount)
    }

    @Test
    fun aFailedLoadCanBeRetried() {
        var retryCount = 0
        setReviews(
            state = ReviewSectionState(reviews = UiState.Failure(FailureReason.Offline)),
            callbacks = ReviewCallbacks(onRetry = { retryCount += 1 }),
        )

        scrollTo(text(DesignR.string.designsystem_failure_retry))
        composeRule.onNodeWithText(text(DesignR.string.designsystem_failure_retry)).performClick()

        assertEquals(1, retryCount)
    }
}
