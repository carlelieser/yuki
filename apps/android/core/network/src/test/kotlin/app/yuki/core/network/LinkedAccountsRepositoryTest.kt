package app.yuki.core.network

import app.yuki.core.model.FailureReason
import app.yuki.core.model.LinkedAccount
import app.yuki.core.model.failureReason
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

private const val ACCOUNTS_BODY = """
[
  {"id":"acc-1","providerId":"credential","accountId":"user-1"},
  {"id":"acc-2","providerId":"github","accountId":"42",
   "providerUsername":"ada","providerProfileUrl":"https://github.com/ada"}
]
"""

class LinkedAccountsRepositoryTest {
    private val requests = mutableListOf<HttpRequestData>()

    @Test
    fun `linked accounts include the GitHub username`() = runTest {
        val accounts = repository(ACCOUNTS_BODY).list().getOrThrow()

        assertEquals(
            listOf(
                LinkedAccount("acc-1", "credential", username = null, profileUrl = null),
                LinkedAccount("acc-2", "github", "ada", "https://github.com/ada"),
            ),
            accounts,
        )
    }

    @Test
    fun `unlinking names the account row`() = runTest {
        repository("""{"status":true}""").unlink("acc-2").getOrThrow()

        assertEquals("/api/auth/unlink-account", requests.single().url.encodedPath)
        assertEquals("""{"accountId":"acc-2"}""", (requests.single().body as TextContent).text)
    }

    @Test
    fun `the last sign-in method cannot be unlinked`() = runTest {
        val failure = repository(
            body = """{"code":"FAILED_TO_UNLINK_LAST_ACCOUNT","message":"no"}""",
            status = HttpStatusCode.BadRequest,
        ).unlink("acc-2").exceptionOrNull()!!

        assertEquals(FailureReason.LastSignInMethod, failure.failureReason())
    }

    @Test
    fun `an old session must sign in again before unlinking`() = runTest {
        val failure = repository(
            body = """{"code":"SESSION_NOT_FRESH","message":"no"}""",
            status = HttpStatusCode.Forbidden,
        ).unlink("acc-2").exceptionOrNull()!!

        assertEquals(FailureReason.ReauthenticationRequired, failure.failureReason())
    }

    private fun repository(
        body: String,
        status: HttpStatusCode = HttpStatusCode.OK,
    ): LinkedAccountsRepository {
        val engine = MockEngine { request ->
            requests.add(request)
            respond(
                content = body,
                status = status,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
            )
        }

        return NetworkLinkedAccountsRepository(YukiHttpClient.create(BASE_URL, engine))
    }
}
