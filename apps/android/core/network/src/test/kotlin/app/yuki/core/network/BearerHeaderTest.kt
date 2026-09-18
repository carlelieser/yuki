package app.yuki.core.network

import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

private const val EMPTY_PACKAGES = """{"packages":[]}"""

class BearerHeaderTest {
    private val requests = mutableListOf<HttpRequestData>()

    private fun repositoryWithToken(tokens: AuthTokenSource): ListingRepository =
        repositoryRecording(requests, EMPTY_PACKAGES, tokens)

    @Test
    fun `a request carries the stored token so the server sees a session`() = runTest {
        repositoryWithToken { "signed.token" }.packages()

        assertEquals("Bearer signed.token", requests.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun `a request omits the header while signed out`() = runTest {
        repositoryWithToken { null }.packages()

        assertNull(requests.single().headers[HttpHeaders.Authorization])
    }

    @Test
    fun `each request reads the token again so signing out takes effect`() = runTest {
        var current: String? = "first.token"
        val repository = repositoryWithToken { current }

        repository.packages()
        current = null
        repository.packages()

        assertEquals("Bearer first.token", requests.first().headers[HttpHeaders.Authorization])
        assertNull(requests.last().headers[HttpHeaders.Authorization])
    }
}
