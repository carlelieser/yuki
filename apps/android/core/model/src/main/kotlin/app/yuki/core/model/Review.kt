package app.yuki.core.model

import java.time.Instant

const val MIN_REVIEW_RATING = 1
const val MAX_REVIEW_RATING = 5
const val MAX_REVIEW_BODY = 2000

data class ReviewAuthor(
    val id: String,
    val name: String,
    val imageUrl: String?,
)

data class Review(
    val id: String,
    val rating: Int,
    val body: String?,
    val createdAt: Instant?,
    val author: ReviewAuthor,
)

data class RatingBucket(
    val rating: Int,
    val count: Int,
)

data class RatingSummary(
    val average: Double,
    val total: Int,
    val distribution: List<RatingBucket>,
)

data class ReviewPage(
    val results: List<Review>,
    val hasMore: Boolean,
)

data class OwnReview(
    val review: Review?,
    val canReview: Boolean,
)

data class ReviewDraft(
    val rating: Int,
    val body: String,
)
