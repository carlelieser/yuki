package app.yuki.core.network

import java.net.URLEncoder
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GithubBrowserUrls @Inject constructor(
    @param:YukiBaseUrl private val baseUrl: String,
) {
    fun signIn(state: String): String =
        "${origin()}/auth/mobile/github/start?state=${encode(state)}"

    fun link(ticket: String, state: String): String =
        "${origin()}/auth/mobile/github/link?ticket=${encode(ticket)}&state=${encode(state)}"

    private fun origin(): String = baseUrl.trimEnd('/')

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")
}
