package app.yuki.core.network

import app.yuki.core.model.FailureReason
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.UiState
import app.yuki.core.model.failureReason
import app.yuki.core.model.toUiState
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListingRepositoryBrowseTest {
    @Test
    fun `maps a browse page to domain summaries`() = runTest {
        val page = repositoryReturning(BROWSE_PAGE_JSON).browse(BrowseQuery()).getOrThrow()

        assertTrue(page.hasMore)
        val summary = page.results.single()
        assertEquals("example-app", summary.slug)
        assertEquals(1_234_567_890L, summary.githubRepoId)
        assertEquals(42, summary.stars)
        assertNull(summary.description)
        assertEquals(ListingCategory.DeveloperTools, summary.category)
    }

    @Test
    fun `maps the rating a listing was sent with`() = runTest {
        val page = repositoryReturning(BROWSE_PAGE_JSON).browse(BrowseQuery()).getOrThrow()

        val summary = page.results.single()
        assertEquals(4.6, summary.ratingAverage!!, 0.001)
        assertEquals(12, summary.ratingCount)
    }

    @Test
    fun `a listing sent without a rating has none`() = runTest {
        val page = repositoryReturning(UNCATEGORISED_PAGE_JSON).browse(BrowseQuery()).getOrThrow()

        val summary = page.results.single()
        assertNull(summary.ratingAverage)
        assertEquals(0, summary.ratingCount)
    }

    @Test
    fun `a listing sent without a category has none`() = runTest {
        val page = repositoryReturning(UNCATEGORISED_PAGE_JSON).browse(BrowseQuery()).getOrThrow()

        assertNull(page.results.single().category)
    }

    @Test
    fun `a category the client does not know yet is dropped rather than fatal`() = runTest {
        val page =
            repositoryReturning(UNKNOWN_CATEGORY_PAGE_JSON).browse(BrowseQuery()).getOrThrow()

        assertNull(page.results.single().category)
    }

    @Test
    fun `sends the category the caller asked to filter by`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val repository = repositoryRecording(requests, BROWSE_PAGE_JSON)

        repository.browse(BrowseQuery(category = ListingCategory.PrivacySecurity)).getOrThrow()

        assertEquals("privacy_security", requests.single().url.parameters["category"])
    }

    @Test
    fun `omits the category parameter when browsing every category`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val repository = repositoryRecording(requests, BROWSE_PAGE_JSON)

        repository.browse(BrowseQuery()).getOrThrow()

        val parameters = requests.single().url.parameters
        assertTrue(parameters.contains("category").not())
        assertNull(parameters["category"])
    }

    @Test
    fun `sends the sort order and offset the server expects`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val repository = repositoryRecording(requests, BROWSE_PAGE_JSON)

        repository.browse(BrowseQuery(sort = "newest", order = "asc", offset = 48)).getOrThrow()

        val parameters = requests.single().url.parameters
        assertEquals("newest", parameters["sort"])
        assertEquals("asc", parameters["order"])
        assertEquals("48", parameters["offset"])
    }

    @Test
    fun `an empty page is a success carrying no results`() = runTest {
        val page = repositoryReturning(EMPTY_PAGE_JSON).browse(BrowseQuery()).getOrThrow()

        assertTrue(page.results.isEmpty())
        assertTrue(page.hasMore.not())
    }
}

class ListingRepositoryFeaturedTest {
    @Test
    fun `requests the featured flag as the string true`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val repository = repositoryRecording(requests, FEATURED_PAGE_JSON)

        val featured = repository.featured().getOrThrow()

        assertEquals("true", requests.single().url.parameters["featured"])
        assertEquals("example-app", featured.single().slug)
    }
}

class ListingRepositorySearchTest {
    @Test
    fun `sends the raw query and maps the bare results envelope`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val repository = repositoryRecording(requests, SEARCH_RESULTS_JSON)

        val results = repository.search(SearchQuery(term = "example")).getOrThrow()

        assertEquals("example", requests.single().url.parameters["q"])
        assertEquals("Example App", results.single().title)
    }

    @Test
    fun `sends the requested sort order and limit`() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val repository = repositoryRecording(requests, SEARCH_RESULTS_JSON)

        repository.search(
            SearchQuery(term = "example", sort = "name", order = "asc", limit = 24),
        ).getOrThrow()

        val parameters = requests.single().url.parameters
        assertEquals("name", parameters["sort"])
        assertEquals("asc", parameters["order"])
        assertEquals("24", parameters["limit"])
    }
}

class ListingRepositoryDetailTest {
    @Test
    fun `maps the unenveloped detail payload`() = runTest {
        val detail = repositoryReturning(DETAIL_JSON).detail("example-app").getOrThrow()

        assertEquals("example-app", detail.slug)
        assertEquals("MIT", detail.license)
        assertEquals(ListingCategory.DeveloperTools, detail.summary.category)
        assertEquals("https://github.com/octocat", detail.links.authorUrl)
        assertEquals(listOf("https://cdn.test/one.png"), detail.screenshots.map { it.url })
        assertTrue(detail.isArchived.not())
    }

    @Test
    fun `parses an iso timestamp into an instant and keeps a null one null`() = runTest {
        val detail = repositoryReturning(DETAIL_JSON).detail("example-app").getOrThrow()

        assertEquals(Instant.parse("2026-01-02T03:04:05.000Z"), detail.versions.first().publishedAt)
        assertNull(detail.versions.last().publishedAt)
    }

    @Test
    fun `preserves the published-at descending order the server sends`() = runTest {
        val detail = repositoryReturning(DETAIL_JSON).detail("example-app").getOrThrow()

        assertEquals(listOf("v1.2.0", "v1.1.0"), detail.versions.map { version -> version.tag })
    }

    @Test
    fun `requests the slug path without an envelope parameter`() = runTest {
        val requests = mutableListOf<HttpRequestData>()

        repositoryRecording(requests, DETAIL_JSON).detail("example-app").getOrThrow()

        assertEquals("/api/listings/example-app", requests.single().url.encodedPath)
    }
}

class ListingRepositorySectionsTest {
    @Test
    fun `maps each section to its category and summaries`() = runTest {
        val sections = repositoryReturning(SECTIONS_JSON).sections(3).getOrThrow()

        assertEquals(
            listOf(ListingCategory.Gaming, ListingCategory.Media),
            sections.map { section -> section.category },
        )
        assertEquals("example-app", sections.first().results.single().slug)
    }

    @Test
    fun `a section category the client does not know yet is dropped not fatal`() = runTest {
        val sections = repositoryReturning(UNKNOWN_SECTION_JSON).sections(3).getOrThrow()

        assertEquals(listOf(ListingCategory.Gaming), sections.map { section -> section.category })
    }

    @Test
    fun `an empty sections array is a success not a failure`() = runTest {
        val sections = repositoryReturning(EMPTY_SECTIONS_JSON).sections(3).getOrThrow()

        assertTrue(sections.isEmpty())
    }

    @Test
    fun `requests the sections path with the limit the caller asked for`() = runTest {
        val requests = mutableListOf<HttpRequestData>()

        repositoryRecording(requests, SECTIONS_JSON).sections(6).getOrThrow()

        val request = requests.single()
        assertEquals("/api/feed", request.url.encodedPath)
        assertEquals("6", request.url.parameters["limit"])
    }

    @Test
    fun `a sections failure names the operation and the limit`() = runTest {
        val result = repositoryFailingWith(HttpStatusCode.InternalServerError).sections(3)

        val error = result.exceptionOrNull()!!
        assertEquals(FailureReason.Server(500), error.failureReason())
        assertTrue(error.message!!.contains("limit=3"))
    }
}

class ListingRepositoryFailureTest {
    @Test
    fun `a 404 becomes a not-found failure naming the slug`() = runTest {
        val result = repositoryFailingWith(HttpStatusCode.NotFound).detail("missing-app")

        val error = result.exceptionOrNull()!!
        assertEquals(FailureReason.NotFound, error.failureReason())
        assertTrue(error.message!!.contains("slug=missing-app"))
    }

    @Test
    fun `a 500 becomes a server failure carrying the status`() = runTest {
        val result = repositoryFailingWith(HttpStatusCode.InternalServerError).browse(BrowseQuery())

        assertEquals(FailureReason.Server(500), result.exceptionOrNull()!!.failureReason())
    }

    @Test
    fun `a transport error becomes an offline failure`() = runTest {
        val result = repositoryOffline().featured()

        assertEquals(FailureReason.Offline, result.exceptionOrNull()!!.failureReason())
    }

    @Test
    fun `malformed json becomes an unexpected failure naming the operation`() = runTest {
        val result = repositoryReturning("""{"results":"not-a-list"}""")
            .search(SearchQuery(term = "example"))

        val error = result.exceptionOrNull()!!
        assertTrue(error.failureReason() is FailureReason.Unexpected)
        assertTrue(error.message!!.contains("q=example"))
    }

    @Test
    fun `a failure is distinguishable from an empty result`() = runTest {
        val empty = repositoryReturning(EMPTY_PAGE_JSON).browse(BrowseQuery()).toUiState()
        val failed = repositoryOffline().browse(BrowseQuery()).toUiState()

        assertTrue(empty is UiState.Success && empty.data.results.isEmpty())
        assertEquals(UiState.Failure(FailureReason.Offline), failed)
    }
}
