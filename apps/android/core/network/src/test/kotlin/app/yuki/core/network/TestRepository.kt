package app.yuki.core.network

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import java.io.IOException

internal const val BASE_URL = "https://yuki.test/"

internal fun repositoryReturning(body: String): ListingRepository =
    repositoryWith(MockEngine { respondJson(body) })

internal fun repositoryFailingWith(status: HttpStatusCode): ListingRepository =
    repositoryWith(MockEngine { respondError(status) })

internal fun repositoryOffline(): ListingRepository =
    repositoryWith(MockEngine { throw IOException("Network is unreachable") })

internal fun repositoryRecording(
    requests: MutableList<HttpRequestData>,
    body: String,
): ListingRepository = repositoryWith(
    MockEngine { request ->
        requests.add(request)
        respondJson(body)
    },
)

internal fun repositoryWith(engine: MockEngine): ListingRepository =
    NetworkListingRepository(ListingRemoteDataSource(YukiHttpClient.create(BASE_URL, engine)))

private fun MockRequestHandleScope.respondJson(body: String) = respond(
    content = body,
    status = HttpStatusCode.OK,
    headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
)
