package app.yuki.core.network

import app.yuki.core.model.FailureAware
import app.yuki.core.model.FailureReason
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException

class RemoteRequestException(
    override val reason: FailureReason,
    operation: String,
    cause: Throwable? = null,
) : Exception("$operation failed: ${describe(reason)}", cause), FailureAware

private fun describe(reason: FailureReason): String = when (reason) {
    FailureReason.Offline -> "the device is offline"
    FailureReason.NotFound -> "the server returned 404"
    FailureReason.Unauthorized -> "the server returned 401"
    FailureReason.EmailNotVerified -> "the email address is not verified"
    FailureReason.AccountExists -> "an account already uses that email address"
    is FailureReason.Rejected -> reason.explanation
    is FailureReason.Server -> "the server returned ${reason.status}"
    is FailureReason.Unexpected -> "an unexpected error occurred"
}

internal fun statusFailure(response: HttpResponse): FailureReason = when (response.status) {
    HttpStatusCode.NotFound -> FailureReason.NotFound
    HttpStatusCode.Unauthorized -> FailureReason.Unauthorized
    else -> FailureReason.Server(response.status.value)
}

internal fun thrownFailure(cause: Throwable): FailureReason = when (cause) {
    is IOException -> FailureReason.Offline
    is SerializationException -> FailureReason.Unexpected(cause)
    else -> FailureReason.Unexpected(cause)
}

internal suspend fun <T> runRemote(operation: String, block: suspend () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (failure: RemoteRequestException) {
    Result.failure(failure)
} catch (error: Throwable) {
    Result.failure(RemoteRequestException(thrownFailure(error), operation, error))
}
