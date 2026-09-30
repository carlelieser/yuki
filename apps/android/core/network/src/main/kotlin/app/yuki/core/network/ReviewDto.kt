package app.yuki.core.network

import app.yuki.core.model.OwnReview
import app.yuki.core.model.RatingBucket
import app.yuki.core.model.RatingSummary
import app.yuki.core.model.Review
import app.yuki.core.model.ReviewAuthor
import app.yuki.core.model.ReviewPage
import kotlinx.serialization.Serializable

@Serializable
internal data class ReviewAuthorDto(
    val id: String,
    val name: String,
    val image: String? = null,
)

@Serializable
internal data class ReviewDto(
    val id: String,
    val rating: Int,
    val body: String? = null,
    val createdAt: String? = null,
    val author: ReviewAuthorDto,
)

@Serializable
internal data class ReviewPageDto(
    val results: List<ReviewDto> = emptyList(),
    val hasMore: Boolean = false,
)

@Serializable
internal data class RatingBucketDto(
    val rating: Int,
    val count: Int,
)

@Serializable
internal data class RatingSummaryDto(
    val average: Double = 0.0,
    val total: Int = 0,
    val distribution: List<RatingBucketDto> = emptyList(),
)

@Serializable
internal data class OwnReviewDto(
    val review: ReviewDto? = null,
    val canReview: Boolean = false,
)

@Serializable
internal data class SavedReviewDto(
    val review: ReviewDto,
)

@Serializable
internal data class ReviewDraftDto(
    val rating: Int,
    val body: String,
)

@Serializable
internal data class ErrorMessageDto(
    val message: String? = null,
)

internal fun ReviewDto.toDomain(): Review = Review(
    id = id,
    rating = rating,
    body = body?.takeIf(String::isNotBlank),
    createdAt = createdAt?.let(::parseTimestamp),
    author = ReviewAuthor(id = author.id, name = author.name, imageUrl = author.image),
)

internal fun ReviewPageDto.toDomain(): ReviewPage = ReviewPage(
    results = results.map(ReviewDto::toDomain),
    hasMore = hasMore,
)

internal fun RatingSummaryDto.toDomain(): RatingSummary = RatingSummary(
    average = average,
    total = total,
    distribution = distribution.map { bucket -> RatingBucket(bucket.rating, bucket.count) },
)

internal fun OwnReviewDto.toDomain(): OwnReview = OwnReview(
    review = review?.toDomain(),
    canReview = canReview,
)
