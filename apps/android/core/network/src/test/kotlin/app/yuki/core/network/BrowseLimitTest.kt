package app.yuki.core.network

import io.ktor.client.request.HttpRequestData
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BrowseLimitTest {
    @Test
    fun `sends the number of listings the caller asked for`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val repository = repositoryRecording(requests, BROWSE_PAGE_JSON)

        repository.browse(BrowseQuery(sort = "updated", limit = 10)).getOrThrow()

        val parameters = requests.single().url.parameters
        assertEquals("updated", parameters["sort"])
        assertEquals("10", parameters["limit"])
    }

    @Test
    fun `leaves the page size to the server when no limit is given`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val repository = repositoryRecording(requests, BROWSE_PAGE_JSON)

        repository.browse(BrowseQuery()).getOrThrow()

        assertNull(requests.single().url.parameters["limit"])
    }
}
