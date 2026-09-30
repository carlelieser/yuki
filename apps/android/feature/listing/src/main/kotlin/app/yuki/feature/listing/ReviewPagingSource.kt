package app.yuki.feature.listing

import androidx.paging.PagingSource
import androidx.paging.PagingState
import app.yuki.core.model.Review
import app.yuki.core.network.ReviewRepository

const val REVIEWS_PAGE_SIZE = 10

internal class ReviewPagingSource(
    private val repository: ReviewRepository,
    private val slug: String,
) : PagingSource<Int, Review>() {
    override fun getRefreshKey(state: PagingState<Int, Review>): Int? = null

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Review> {
        val offset = params.key ?: 0

        return repository.reviews(slug, offset).fold(
            onSuccess = { page ->
                LoadResult.Page(
                    data = page.results,
                    prevKey = null,
                    nextKey = if (page.hasMore) offset + page.results.size else null,
                )
            },
            onFailure = { error -> LoadResult.Error(error) },
        )
    }
}
