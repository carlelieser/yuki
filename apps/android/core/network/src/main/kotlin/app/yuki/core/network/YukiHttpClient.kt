package app.yuki.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

private const val BEARER_SCHEME = "Bearer"

private fun bearerPlugin(tokens: AuthTokenSource) = createClientPlugin("YukiBearer") {
    onRequest { request, _ ->
        val token = tokens.token() ?: return@onRequest
        request.headers.append(HttpHeaders.Authorization, "$BEARER_SCHEME $token")
    }
}

object YukiHttpClient {
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun create(
        baseUrl: String,
        engine: HttpClientEngine = OkHttp.create(),
        tokens: AuthTokenSource = AuthTokenSource { null },
    ): HttpClient = HttpClient(engine) {
        expectSuccess = false

        install(ContentNegotiation) {
            json(json, contentType = ContentType.Application.Json)
        }

        install(bearerPlugin(tokens))

        defaultRequest {
            url(baseUrl)
        }
    }
}
