package app.yuki.core.auth

import java.net.URI
import java.net.URLDecoder

sealed interface BrowserAuthResult {
    val state: String

    data class SignedIn(val ticket: String, override val state: String) : BrowserAuthResult

    data class Linked(override val state: String) : BrowserAuthResult

    data class Failed(val code: String, override val state: String) : BrowserAuthResult
}

private const val LINKED = "linked"

fun parseBrowserAuthResult(url: String?): BrowserAuthResult? {
    val query = url?.let { runCatching { URI(it).rawQuery }.getOrNull() } ?: return null
    val params = queryParams(query)
    val state = params["state"] ?: return null
    val ticket = params["token"]
    val error = params["error"]

    return when {
        error != null -> BrowserAuthResult.Failed(error, state)
        ticket != null -> BrowserAuthResult.SignedIn(ticket, state)
        params["result"] == LINKED -> BrowserAuthResult.Linked(state)
        else -> null
    }
}

private fun queryParams(query: String): Map<String, String> = query.split('&')
    .mapNotNull { pair ->
        val separator = pair.indexOf('=')
        if (separator <= 0) return@mapNotNull null
        decode(pair.substring(0, separator)) to decode(pair.substring(separator + 1))
    }
    .toMap()

private fun decode(value: String): String = URLDecoder.decode(value, "UTF-8")
