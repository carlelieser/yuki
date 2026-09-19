package app.yuki.core.network

import app.yuki.core.model.FailureReason
import app.yuki.core.model.LibraryEntry
import app.yuki.core.model.failureReason
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val LIBRARY_BODY = """
{"results":[{"githubRepoId":42,"slug":"aurora-store","title":"Aurora Store",
"packageName":"com.aurora.store","iconUrl":"/icon.png","versionTag":"v1.2.0"}]}
"""

class LibraryRepositoryTest {
    private val requests = mutableListOf<HttpRequestData>()

    @Test
    fun `the library carries the package name so installs can be matched`() = runTest {
        val entries = repositoryReturning(LIBRARY_BODY).library().getOrThrow()

        assertEquals(
            listOf(
                LibraryEntry(
                    githubRepoId = 42,
                    packageName = "com.aurora.store",
                    slug = "aurora-store",
                    title = "Aurora Store",
                    iconUrl = "/icon.png",
                    versionTag = "v1.2.0",
                ),
            ),
            entries,
        )
    }

    @Test
    fun `an empty library reads as no entries`() = runTest {
        assertEquals(emptyList<LibraryEntry>(), repositoryReturning("""{}""").library().getOrThrow())
    }

    @Test
    fun `recording posts the slug to the library`() = runTest {
        repositoryReturning("""{"slug":"aurora-store"}""").record("aurora-store", "v1.2.0")

        val request = requests.single()
        assertEquals(HttpMethod.Post, request.method)
        assertTrue(request.url.encodedPath.endsWith("api/library"))
    }

    @Test
    fun `a signed-out library reads as unauthorized`() = runTest {
        val failure = repositoryFailing(HttpStatusCode.Unauthorized).library().exceptionOrNull()!!

        assertEquals(FailureReason.Unauthorized, failure.failureReason())
    }

    @Test
    fun `a failed recording names the slug`() = runTest {
        val failure = repositoryFailing(HttpStatusCode.NotFound)
            .record("missing-app", "v1")
            .exceptionOrNull()!!

        assertTrue(failure.message!!.contains("slug=missing-app"))
    }

    private fun repositoryReturning(body: String): LibraryRepository = repositoryWith(
        MockEngine { request ->
            requests.add(request)
            respond(
                content = body,
                status = HttpStatusCode.OK,
                headers = headersOf("Content-Type", ContentType.Application.Json.toString()),
            )
        },
    )

    private fun repositoryFailing(status: HttpStatusCode): LibraryRepository =
        repositoryWith(MockEngine { respondError(status) })

    private fun repositoryWith(engine: MockEngine): LibraryRepository =
        NetworkLibraryRepository(
            LibraryRemoteDataSource(YukiHttpClient.create(BASE_URL, engine)),
        )
}
