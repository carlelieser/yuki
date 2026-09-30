package app.yuki.feature.listing

import app.yuki.core.model.RatingBucket
import app.yuki.core.model.RatingSummary
import app.yuki.core.model.Review
import app.yuki.core.model.ReviewAuthor
import java.time.Instant

internal fun review(id: String, body: String? = "Review $id"): Review = Review(
    id = id,
    rating = 4,
    body = body,
    createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    author = ReviewAuthor(id = "user-$id", name = "Reviewer $id", imageUrl = null),
)

internal fun ratingSummary(total: Int): RatingSummary = RatingSummary(
    average = if (total == 0) 0.0 else 4.0,
    total = total,
    distribution = (5 downTo 1).map { rating -> RatingBucket(rating, if (rating == 4) total else 0) },
)
