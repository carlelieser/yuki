package app.yuki.feature.listing

import androidx.paging.PagingSource
import androidx.paging.testing.TestPager
import androidx.paging.PagingConfig
import app.yuki.core.model.FailureReason
import app.yuki.core.model.ReviewPage
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPagingSourceTest {
    private val config = PagingConfig(pageSize = REVIEWS_PAGE_SIZE)

    @Test
    fun `pages continue from the number of reviews already loaded`() = runTest {
        val first = (1..10).map { index -> review("r$index") }
        val second = listOf(review("r11"))
        val repository = FakeReviewRepository(
            pages = mapOf(
                0 to Result.success(ReviewPage(first, hasMore = true)),
                10 to Result.success(ReviewPage(second, hasMore = false)),
            ),
        )
        val pager = TestPager(config, ReviewPagingSource(repository, SLUG))

        val firstPage = pager.refresh() as PagingSource.LoadResult.Page
        val secondPage = pager.append() as PagingSource.LoadResult.Page

        assertEquals(first, firstPage.data)
        assertEquals(10, firstPage.nextKey)
        assertEquals(second, secondPage.data)
        assertNull(secondPage.nextKey)
    }

    @Test
    fun `a failed page reports the error`() = runTest {
        val repository = FakeReviewRepository(
            pages = mapOf(0 to Result.failure(TypedFailure(FailureReason.Offline))),
        )
        val pager = TestPager(config, ReviewPagingSource(repository, SLUG))

        assertTrue(pager.refresh() is PagingSource.LoadResult.Error)
    }
}
