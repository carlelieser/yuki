package app.yuki.core.network

import app.yuki.core.model.FailureReason
import app.yuki.core.model.OwnReview
import app.yuki.core.model.RatingBucket
import app.yuki.core.model.RatingSummary
import app.yuki.core.model.Review
import app.yuki.core.model.ReviewAuthor
import app.yuki.core.model.ReviewDraft
import app.yuki.core.model.failureReason
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import java.time.Instant
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private const val REVIEW_JSON = """
{"id":"review-1","rating":4,"body":"Solid launcher","createdAt":"2026-01-02T03:04:05.000Z",
"author":{"id":"user-1","name":"Ada","image":"https://cdn.test/ada.png"}}
"""

private val REVIEW = Review(
    id = "review-1",
    rating = 4,
    body = "Solid launcher",
    createdAt = Instant.parse("2026-01-02T03:04:05Z"),
    author = ReviewAuthor(id = "user-1", name = "Ada", imageUrl = "https://cdn.test/ada.png"),
)

private class RecordedCall(val status: HttpStatusCode, val body: String)

@OptIn(ExperimentalCoroutinesApi::class)
class ReviewRepositoryTest {
    private val requests = mutableListOf<HttpRequestData>()

    private fun repository(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): ReviewRepository = repositoryAnswering(RecordedCall(status, body))

    private fun repositoryAnswering(call: RecordedCall): ReviewRepository = NetworkReviewRepository(
        ReviewRemoteDataSource(
            YukiHttpClient.create(
                BASE_URL,
                MockEngine { request ->
                    requests.add(request)
                    respond(
                        content = call.body,
                        status = call.status,
                        headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
                    )
                },
                AuthTokenSource { "token-1" },
            ),
        ),
    )

    @Test
    fun `reads a page of reviews from the requested offset`() = runTest {
        val page = repository("""{"results":[$REVIEW_JSON],"hasMore":true}""")
            .reviews("aurora", offset = 10)
            .getOrThrow()

        assertEquals(listOf(REVIEW), page.results)
        assertTrue(page.hasMore)
        assertEquals("/api/listings/aurora/reviews", requests.single().url.encodedPath)
        assertEquals("10", requests.single().url.parameters["offset"])
    }

    @Test
    fun `the first page sends no offset`() = runTest {
        repository("""{"results":[],"hasMore":false}""").reviews("aurora", offset = 0)

        assertNull(requests.single().url.parameters["offset"])
    }

    @Test
    fun `reads the rating summary with its distribution`() = runTest {
        val summary = repository(
            """{"average":4.5,"total":2,"distribution":[{"rating":5,"count":1},{"rating":4,"count":1}]}""",
        ).summary("aurora").getOrThrow()

        assertEquals(
            RatingSummary(4.5, 2, listOf(RatingBucket(5, 1), RatingBucket(4, 1))),
            summary,
        )
        assertEquals("/api/listings/aurora/reviews/summary", requests.single().url.encodedPath)
    }

    @Test
    fun `reads the signed-in review and eligibility`() = runTest {
        val own = repository("""{"review":$REVIEW_JSON,"canReview":true}""").ownReview("aurora").getOrThrow()

        assertEquals(OwnReview(review = REVIEW, canReview = true), own)
        assertEquals("/api/listings/aurora/reviews/mine", requests.single().url.encodedPath)
    }

    @Test
    fun `reads a missing signed-in review as none`() = runTest {
        val own = repository("""{"review":null,"canReview":false}""").ownReview("aurora").getOrThrow()

        assertEquals(OwnReview(review = null, canReview = false), own)
    }

    @Test
    fun `saving puts the draft with the bearer token`() = runTest {
        val saved = repository("""{"review":$REVIEW_JSON}""")
            .save("aurora", ReviewDraft(rating = 4, body = "Solid launcher"))
            .getOrThrow()

        val request = requests.single()
        assertEquals(REVIEW, saved)
        assertEquals(HttpMethod.Put, request.method)
        assertEquals("Bearer token-1", request.headers["Authorization"])
        assertEquals(
            """{"rating":4,"body":"Solid launcher"}""",
            (request.body as TextContent).text,
        )
    }

    @Test
    fun `a refused save carries the server explanation`() = runTest {
        val failure = repository(
            body = """{"message":"Download this app before reviewing it."}""",
            status = HttpStatusCode.Forbidden,
        ).save("aurora", ReviewDraft(rating = 4, body = "")).exceptionOrNull()!!

        assertEquals(
            FailureReason.Rejected("Download this app before reviewing it."),
            failure.failureReason(),
        )
    }

    @Test
    fun `a signed-out save reads as unauthorized`() = runTest {
        val failure = repository("""{"message":"Sign in"}""", HttpStatusCode.Unauthorized)
            .save("aurora", ReviewDraft(rating = 4, body = ""))
            .exceptionOrNull()!!

        assertEquals(FailureReason.Unauthorized, failure.failureReason())
    }

    @Test
    fun `deleting removes the signed-in review`() = runTest {
        repository("", HttpStatusCode.NoContent).delete("aurora").getOrThrow()

        assertEquals(HttpMethod.Delete, requests.single().method)
        assertEquals("/api/listings/aurora/reviews/mine", requests.single().url.encodedPath)
    }

    @Test
    fun `saving and deleting announce the changed listing`() = runTest {
        val repository = repository("""{"review":$REVIEW_JSON}""")
        val changed = mutableListOf<String>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.changes.toList(changed)
        }

        repository.save("aurora", ReviewDraft(rating = 4, body = ""))
        repository.delete("aurora")
        collector.cancel()

        assertEquals(listOf("aurora", "aurora"), changed)
    }

    @Test
    fun `a failed save announces nothing`() = runTest {
        val repository = repository("""{"message":"Choose a rating."}""", HttpStatusCode.BadRequest)
        val changed = mutableListOf<String>()
        val collector = launch(UnconfinedTestDispatcher(testScheduler)) {
            repository.changes.toList(changed)
        }

        repository.save("aurora", ReviewDraft(rating = 0, body = ""))
        collector.cancel()

        assertEquals(emptyList<String>(), changed)
    }
}
