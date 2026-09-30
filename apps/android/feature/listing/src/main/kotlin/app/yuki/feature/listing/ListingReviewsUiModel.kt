package app.yuki.feature.listing

import app.yuki.core.model.InstallState
import app.yuki.core.model.OwnReview
import app.yuki.core.model.RatingSummary
import app.yuki.core.model.Review

const val REVIEW_PREVIEW_COUNT = 3

data class ListingReviews(
    val summary: RatingSummary,
    val preview: List<Review>,
    val hasMore: Boolean,
)

data class ReviewViewer(
    val isSignedIn: Boolean,
    val own: OwnReview? = null,
)

sealed interface ReviewPrompt {
    data object Hidden : ReviewPrompt

    data object SignIn : ReviewPrompt

    data object InstallRequired : ReviewPrompt

    data object Write : ReviewPrompt

    data class Edit(val review: Review) : ReviewPrompt
}

private fun InstallState?.isOnDevice(): Boolean =
    this is InstallState.Installed || this is InstallState.UpdateAvailable

fun reviewPrompt(viewer: ReviewViewer, installState: InstallState?): ReviewPrompt {
    if (!viewer.isSignedIn) return ReviewPrompt.SignIn

    val own = viewer.own ?: return ReviewPrompt.Hidden
    own.review?.let { review -> return ReviewPrompt.Edit(review) }

    if (installState.isOnDevice()) return ReviewPrompt.Write

    return ReviewPrompt.InstallRequired
}
