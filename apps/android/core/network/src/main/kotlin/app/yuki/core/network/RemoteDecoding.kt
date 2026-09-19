package app.yuki.core.network

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

internal suspend inline fun <reified T> HttpResponse.decode(operation: String): T {
    if (!status.isSuccess()) throw RemoteRequestException(statusFailure(this), operation)

    return try {
        body<T>()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        throw RemoteRequestException(thrownFailure(error), operation, error)
    }
}
