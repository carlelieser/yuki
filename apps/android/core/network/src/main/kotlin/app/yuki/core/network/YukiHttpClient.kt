package app.yuki.core.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.ContentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

object YukiHttpClient {
    val json: Json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
    }

    fun create(baseUrl: String, engine: HttpClientEngine = OkHttp.create()): HttpClient =
        HttpClient(engine) {
            expectSuccess = false

            install(ContentNegotiation) {
                json(json, contentType = ContentType.Application.Json)
            }

            defaultRequest {
                url(baseUrl)
            }
        }
}
