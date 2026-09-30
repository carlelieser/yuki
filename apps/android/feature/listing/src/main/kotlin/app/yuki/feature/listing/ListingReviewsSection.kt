package app.yuki.feature.listing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.yuki.core.designsystem.component.CollectionEmpty
import app.yuki.core.designsystem.component.EmptyContent
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.component.SeeAllButton
import app.yuki.core.designsystem.component.SectionHeader
import app.yuki.core.designsystem.component.StatusCard
import app.yuki.core.designsystem.component.StatusContent
import app.yuki.core.designsystem.component.StatusTone
import app.yuki.core.designsystem.component.YukiButton
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.component.YukiTextButton
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.UiState

const val REVIEWS_SECTION_KEY = "reviews"
const val REVIEWS_LOADING_TAG = "reviewsLoading"
const val REVIEW_PROMPT_TAG = "reviewPrompt"

data class ReviewSectionState(
    val reviews: UiState<ListingReviews> = UiState.Loading,
    val prompt: ReviewPrompt = ReviewPrompt.Hidden,
)

data class ReviewCallbacks(
    val onSeeAll: () -> Unit = {},
    val onWrite: () -> Unit = {},
    val onSignIn: () -> Unit = {},
    val onRetry: () -> Unit = {},
)

internal fun LazyListScope.reviewSection(state: ReviewSectionState, callbacks: ReviewCallbacks) {
    val loaded = (state.reviews as? UiState.Success)?.data
    val onSeeAll = callbacks.onSeeAll.takeIf { loaded?.hasMore == true }

    item(key = REVIEWS_SECTION_KEY) { ReviewSectionHeader(onSeeAll = onSeeAll) }

    when (val reviews = state.reviews) {
        UiState.Loading -> item { ReviewsLoading() }
        is UiState.Failure -> item {
            FailureState(
                reason = reviews.reason,
                missingMessage = stringResource(R.string.listing_reviews_missing),
                onRetry = callbacks.onRetry,
                modifier = Modifier.padding(horizontal = YukiSpacing.Large),
            )
        }
        is UiState.Success -> loadedReviews(reviews.data, state.prompt, callbacks)
    }
}

private fun LazyListScope.loadedReviews(
    reviews: ListingReviews,
    prompt: ReviewPrompt,
    callbacks: ReviewCallbacks,
) {
    if (reviews.summary.total > 0) item { RatingSummaryCard(summary = reviews.summary) }

    item { ReviewPromptRow(prompt = prompt, callbacks = callbacks) }

    if (reviews.summary.total == 0) {
        item { NoReviews() }
        return
    }

    val ownId = (prompt as? ReviewPrompt.Edit)?.review?.id
    val others = reviews.preview.filterNot { review -> review.id == ownId }
    items(items = others, key = { review -> "$REVIEWS_SECTION_KEY/${review.id}" }) { review ->
        ReviewCard(review = review)
    }
}

@Composable
private fun ReviewSectionHeader(onSeeAll: (() -> Unit)?) {
    val title = stringResource(R.string.listing_section_reviews)

    SectionHeader(
        title = title,
        action = onSeeAll?.let { seeAll ->
            { SeeAllButton(label = title, onClick = seeAll) }
        },
    )
}

@Composable
private fun ReviewPromptRow(prompt: ReviewPrompt, callbacks: ReviewCallbacks) {
    val modifier = Modifier
        .padding(horizontal = YukiSpacing.Large)
        .testTag(REVIEW_PROMPT_TAG)

    when (prompt) {
        ReviewPrompt.Hidden -> Unit
        ReviewPrompt.SignIn -> YukiTextButton(
            label = stringResource(R.string.listing_reviews_sign_in),
            onClick = callbacks.onSignIn,
            modifier = modifier,
        )
        ReviewPrompt.InstallRequired -> StatusCard(
            content = StatusContent(
                title = stringResource(R.string.listing_reviews_install_required_title),
                description = stringResource(R.string.listing_reviews_install_required_description),
                tone = StatusTone.Informative,
            ),
            modifier = modifier,
        )
        ReviewPrompt.Write -> YukiButton(
            label = stringResource(R.string.listing_reviews_write),
            onClick = callbacks.onWrite,
            modifier = modifier.fillMaxWidth(),
        )
        is ReviewPrompt.Edit -> ReviewCard(review = prompt.review, onEdit = callbacks.onWrite)
    }
}

@Composable
private fun NoReviews() {
    CollectionEmpty(
        content = EmptyContent(
            title = stringResource(R.string.listing_reviews_empty_title),
            description = stringResource(R.string.listing_reviews_empty_description),
            icon = YukiIcons.Star,
        ),
    )
}

@Composable
private fun ReviewsLoading() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(YukiSpacing.Large)
            .testTag(REVIEWS_LOADING_TAG),
        contentAlignment = Alignment.Center,
    ) {
        YukiLoadingIndicator()
    }
}
