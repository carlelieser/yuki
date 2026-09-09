package app.yuki.feature.explore

import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.yuki.core.model.ListingPage
import app.yuki.core.model.ListingSummary
import app.yuki.core.network.BrowseQuery
import app.yuki.core.network.ListingRepository

const val BROWSE_PAGE_SIZE = 24

internal class ListingPagingSource(
    private val repository: ListingRepository,
) : PagingSource<Int, ListingSummary>() {
    override fun getRefreshKey(state: PagingState<Int, ListingSummary>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ListingSummary> {
        val offset = params.key ?: 0

        return repository.browse(BrowseQuery(offset = offset)).fold(
            onSuccess = { page -> page.toLoadResult(offset) },
            onFailure = { error -> LoadResult.Error(error) },
        )
    }
}

private fun ListingPage.toLoadResult(offset: Int): PagingSource.LoadResult<Int, ListingSummary> =
    PagingSource.LoadResult.Page(
        data = results,
        prevKey = null,
        nextKey = if (hasMore) offset + BROWSE_PAGE_SIZE else null,
    )
