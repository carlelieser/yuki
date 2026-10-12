package app.yuki.core.network

import app.yuki.core.model.FailureAware
import app.yuki.core.model.FailureReason
import app.yuki.core.model.NetworkStatus
import app.yuki.core.model.connectionFailure
import io.ktor.client.plugins.api.Send
import io.ktor.client.plugins.api.createClientPlugin
import java.io.IOException

internal class ConnectionFailure(
    override val reason: FailureReason,
    url: String,
    cause: IOException,
) : IOException("Request to $url did not complete", cause), FailureAware

internal fun connectionPlugin(network: NetworkStatus) = createClientPlugin("YukiConnection") {
    on(Send) { request ->
        try {
            proceed(request)
        } catch (error: IOException) {
            throw ConnectionFailure(network.connectionFailure(), request.url.buildString(), error)
        }
    }
}
