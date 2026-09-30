package app.yuki.core.network

import app.yuki.core.model.OwnReview
import app.yuki.core.model.RatingSummary
import app.yuki.core.model.Review
import app.yuki.core.model.ReviewDraft
import app.yuki.core.model.ReviewPage
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

interface ReviewRepository {
    val changes: Flow<String>

    suspend fun reviews(slug: String, offset: Int): Result<ReviewPage>

    suspend fun summary(slug: String): Result<RatingSummary>

    suspend fun ownReview(slug: String): Result<OwnReview>

    suspend fun save(slug: String, draft: ReviewDraft): Result<Review>

    suspend fun delete(slug: String): Result<Unit>
}

@Singleton
internal class NetworkReviewRepository @Inject constructor(
    private val remote: ReviewRemoteDataSource,
) : ReviewRepository {
    private val changedSlugs = MutableSharedFlow<String>(extraBufferCapacity = CHANGE_BUFFER)

    override val changes: Flow<String> = changedSlugs.asSharedFlow()

    override suspend fun reviews(slug: String, offset: Int): Result<ReviewPage> =
        runRemote("Load reviews for slug=$slug (offset=$offset)") {
            remote.reviews(slug, offset).toDomain()
        }

    override suspend fun summary(slug: String): Result<RatingSummary> =
        runRemote("Load the rating summary for slug=$slug") {
            remote.summary(slug).toDomain()
        }

    override suspend fun ownReview(slug: String): Result<OwnReview> =
        runRemote("Load the signed-in review for slug=$slug") {
            remote.ownReview(slug).toDomain()
        }

    override suspend fun save(slug: String, draft: ReviewDraft): Result<Review> =
        runRemote("Save the review for slug=$slug") {
            remote.save(slug, draft).review.toDomain()
        }.onSuccess { changedSlugs.emit(slug) }

    override suspend fun delete(slug: String): Result<Unit> =
        runRemote("Delete the review for slug=$slug") {
            remote.delete(slug)
        }.onSuccess { changedSlugs.emit(slug) }

    private companion object {
        const val CHANGE_BUFFER = 8
    }
}
