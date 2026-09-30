package app.yuki.feature.listing

import app.yuki.core.auth.AuthSession
import app.yuki.core.auth.SessionStore
import app.yuki.core.model.AuthAccount
import app.yuki.core.model.LibraryEntry
import app.yuki.core.model.OwnReview
import app.yuki.core.model.RatingBucket
import app.yuki.core.model.RatingSummary
import app.yuki.core.model.Review
import app.yuki.core.model.ReviewAuthor
import app.yuki.core.model.ReviewDraft
import app.yuki.core.model.ReviewPage
import app.yuki.core.network.LibraryRepository
import app.yuki.core.network.ReviewRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

internal fun review(
    id: String,
    rating: Int = 4,
    body: String? = "Solid",
    authorId: String = "user-$id",
): Review = Review(
    id = id,
    rating = rating,
    body = body,
    createdAt = Instant.parse("2026-01-01T00:00:00Z"),
    author = ReviewAuthor(id = authorId, name = "Reviewer $id", imageUrl = null),
)

internal fun ratingSummary(total: Int = 1, average: Double = 4.0): RatingSummary = RatingSummary(
    average = average,
    total = total,
    distribution = (5 downTo 1).map { rating -> RatingBucket(rating, if (rating == 4) total else 0) },
)

internal val SIGNED_IN = AuthSession(
    token = "token-1",
    account = AuthAccount(id = "user-1", name = "Ada", email = "ada@test", imageUrl = null),
)

internal class FakeSessionStore(initial: AuthSession? = null) : SessionStore {
    private val stored = MutableStateFlow(initial)

    override val session: Flow<AuthSession?> = stored.asStateFlow()

    override suspend fun read(): AuthSession? = stored.value

    override suspend fun store(session: AuthSession) {
        stored.value = session
    }

    override suspend fun updateAccount(account: AuthAccount) {
        stored.value = stored.value?.copy(account = account)
    }

    override suspend fun clear() {
        stored.value = null
    }
}

internal class FakeReviewRepository(
    var summaryResult: Result<RatingSummary> = Result.success(ratingSummary()),
    var pages: Map<Int, Result<ReviewPage>> = mapOf(0 to Result.success(ReviewPage(emptyList(), false))),
    var ownResult: Result<OwnReview> = Result.success(OwnReview(review = null, canReview = true)),
) : ReviewRepository {
    private val changedSlugs = MutableSharedFlow<String>(extraBufferCapacity = 8)

    override val changes: Flow<String> = changedSlugs

    var saveResult: Result<Review> = Result.success(review("saved"))
    var deleteResult: Result<Unit> = Result.success(Unit)

    val saved: MutableList<ReviewDraft> = mutableListOf()
    var deleteCount: Int = 0
        private set
    var summaryCallCount: Int = 0
        private set
    var ownCallCount: Int = 0
        private set

    suspend fun announce(slug: String) {
        changedSlugs.emit(slug)
    }

    override suspend fun reviews(slug: String, offset: Int): Result<ReviewPage> =
        pages[offset] ?: error("No page stubbed for offset=$offset")

    override suspend fun summary(slug: String): Result<RatingSummary> {
        summaryCallCount += 1
        return summaryResult
    }

    override suspend fun ownReview(slug: String): Result<OwnReview> {
        ownCallCount += 1
        return ownResult
    }

    override suspend fun save(slug: String, draft: ReviewDraft): Result<Review> {
        saved.add(draft)
        return saveResult
    }

    override suspend fun delete(slug: String): Result<Unit> {
        deleteCount += 1
        return deleteResult
    }
}

internal class RecordingLibraryRepository(
    var recordResult: Result<Unit> = Result.success(Unit),
) : LibraryRepository {
    val recorded: MutableList<Pair<String, String>> = mutableListOf()

    override suspend fun library(): Result<List<LibraryEntry>> =
        error("library is not used by the review composer")

    override suspend fun record(slug: String, versionTag: String): Result<Unit> {
        recorded.add(slug to versionTag)
        return recordResult
    }
}
