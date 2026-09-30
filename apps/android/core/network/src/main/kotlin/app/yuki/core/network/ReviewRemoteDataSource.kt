package app.yuki.core.network

import app.yuki.core.model.FailureReason
import app.yuki.core.model.ReviewDraft
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class ReviewRemoteDataSource @Inject constructor(
    private val client: HttpClient,
) {
    suspend fun reviews(slug: String, offset: Int): ReviewPageDto {
        val response = client.get("api/listings/$slug/reviews") {
            if (offset > 0) parameter("offset", offset)
        }
        return response.decode("Load reviews for slug=$slug (offset=$offset)")
    }

    suspend fun summary(slug: String): RatingSummaryDto {
        val response = client.get("api/listings/$slug/reviews/summary")
        return response.decode("Load the rating summary for slug=$slug")
    }

    suspend fun ownReview(slug: String): OwnReviewDto {
        val response = client.get(ownReviewPath(slug))
        return response.decode("Load the signed-in review for slug=$slug")
    }

    suspend fun save(slug: String, draft: ReviewDraft): SavedReviewDto {
        val operation = "Save the review for slug=$slug"
        val response = client.put(ownReviewPath(slug)) {
            contentType(ContentType.Application.Json)
            setBody(ReviewDraftDto(rating = draft.rating, body = draft.body))
        }

        if (!response.status.isSuccess()) {
            throw RemoteRequestException(response.reviewFailure(), operation)
        }

        return response.decode(operation)
    }

    suspend fun delete(slug: String) {
        val response = client.delete(ownReviewPath(slug))

        if (!response.status.isSuccess()) {
            throw RemoteRequestException(statusFailure(response), "Delete the review for slug=$slug")
        }
    }

    private fun ownReviewPath(slug: String): String = "api/listings/$slug/reviews/mine"
}

private val REJECTED_STATUSES = setOf(HttpStatusCode.BadRequest, HttpStatusCode.Forbidden)

private suspend fun HttpResponse.reviewFailure(): FailureReason {
    if (status !in REJECTED_STATUSES) return statusFailure(this)

    val explanation = runCatching { body<ErrorMessageDto>() }.getOrNull()?.message

    return explanation
        ?.takeIf(String::isNotBlank)
        ?.let(FailureReason::Rejected)
        ?: statusFailure(this)
}
