package app.yuki.feature.search

import androidx.paging.PagingSource
import app.yuki.core.model.FailureReason
import app.yuki.core.model.ListingPage
import app.yuki.core.model.ListingSummary
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ListingPagingSourceTest {
    private val repository = FakeListingRepository()

    private fun refresh(key: Int?) = PagingSource.LoadParams.Refresh(
        key = key,
        loadSize = BROWSE_PAGE_SIZE,
        placeholdersEnabled = false,
    )

    @Test
    fun `the first load starts at offset zero`() = runTest {
        repository.browseResult = Result.success(ListingPage(listOf(listing("alpha")), false))

        ListingPagingSource(repository).load(refresh(null))

        assertEquals(listOf(0), repository.browsedOffsets)
    }

    @Test
    fun `the next key advances by a full page when more remain`() = runTest {
        repository.browseResult = Result.success(ListingPage(listOf(listing("alpha")), true))

        val page = ListingPagingSource(repository).load(refresh(0))
            as PagingSource.LoadResult.Page<Int, ListingSummary>

        assertEquals(BROWSE_PAGE_SIZE, page.nextKey)
        assertNull(page.prevKey)
    }

    @Test
    fun `the next key is null on the last page`() = runTest {
        repository.browseResult = Result.success(ListingPage(listOf(listing("alpha")), false))

        val page = ListingPagingSource(repository).load(refresh(0))
            as PagingSource.LoadResult.Page<Int, ListingSummary>

        assertNull(page.nextKey)
    }

    @Test
    fun `an empty page is a page not an error`() = runTest {
        repository.browseResult = Result.success(ListingPage(emptyList(), false))

        val result = ListingPagingSource(repository).load(refresh(0))

        assertTrue(result is PagingSource.LoadResult.Page)
        assertTrue((result as PagingSource.LoadResult.Page).data.isEmpty())
    }

    @Test
    fun `a failed browse surfaces the typed reason`() = runTest {
        repository.browseResult = Result.failure(TypedFailure(FailureReason.Offline))

        val result = ListingPagingSource(repository).load(refresh(0))

        val error = result as PagingSource.LoadResult.Error
        assertEquals(FailureReason.Offline, (error.throwable as TypedFailure).reason)
    }

    @Test
    fun `an author filter reaches the browse query`() = runTest {
        repository.browseResult = Result.success(ListingPage(emptyList(), false))

        ListingPagingSource(repository, BrowseFilter(author = "acme")).load(refresh(0))

        assertEquals(listOf("acme"), repository.browsedAuthors)
    }

    @Test
    fun `listings by another author are dropped when the server ignores the filter`() = runTest {
        repository.browseResult = Result.success(
            ListingPage(listOf(listing("alpha"), listing("beta")), false),
        )

        val result = ListingPagingSource(repository, BrowseFilter(author = "nobody"))
            .load(refresh(0))

        assertTrue((result as PagingSource.LoadResult.Page).data.isEmpty())
    }

    @Test
    fun `paging stops instead of looping when the server ignores the filter`() = runTest {
        repository.browseResult = Result.success(
            ListingPage(listOf(listing("alpha"), listing("beta")), true),
        )

        val result = ListingPagingSource(repository, BrowseFilter(author = "nobody"))
            .load(refresh(0))

        assertNull((result as PagingSource.LoadResult.Page).nextKey)
    }

    @Test
    fun `an honestly empty page still stops paging`() = runTest {
        repository.browseResult = Result.success(ListingPage(emptyList(), false))

        val result = ListingPagingSource(repository, BrowseFilter(author = "acme"))
            .load(refresh(0))

        assertNull((result as PagingSource.LoadResult.Page).nextKey)
    }

    @Test
    fun `the requested author is kept regardless of casing`() = runTest {
        repository.browseResult = Result.success(ListingPage(listOf(listing("alpha")), false))

        val result = ListingPagingSource(repository, BrowseFilter(author = "YUKI"))
            .load(refresh(0))

        assertEquals(1, (result as PagingSource.LoadResult.Page).data.size)
    }

    @Test
    fun `an unfiltered browse sends no author`() = runTest {
        repository.browseResult = Result.success(ListingPage(emptyList(), false))

        ListingPagingSource(repository).load(refresh(0))

        assertEquals(listOf<String?>(null), repository.browsedAuthors)
    }
}
