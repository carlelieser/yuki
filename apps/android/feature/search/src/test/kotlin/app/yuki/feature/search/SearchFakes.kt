package app.yuki.feature.search

import app.yuki.core.datastore.RecentSearchStore
import app.yuki.core.model.CategorySection
import app.yuki.core.model.FailureAware
import app.yuki.core.model.FailureReason
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.ListingDetail
import app.yuki.core.model.ListingPage
import app.yuki.core.model.ListingSummary
import app.yuki.core.network.BrowseQuery
import app.yuki.core.network.ListingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

internal class TypedFailure(override val reason: FailureReason) :
    Exception("Search test failure"), FailureAware

internal fun listing(slug: String): ListingSummary = ListingSummary(
    id = slug,
    githubRepoId = slug.hashCode().toLong(),
    slug = slug,
    title = slug.replaceFirstChar(Char::uppercase),
    author = "yuki",
    description = null,
    iconUrl = null,
    bannerUrl = null,
    category = null,
    stars = 1,
)

internal class FakeListingRepository : ListingRepository {
    var searchResult: Result<List<ListingSummary>> = Result.success(emptyList())
    var browseResult: Result<ListingPage> = Result.success(ListingPage(emptyList(), false))

    val browsedOffsets = mutableListOf<Int>()
    val browsedCategories = mutableListOf<ListingCategory?>()
    val browsedSorts = mutableListOf<String>()
    val searchedQueries = mutableListOf<String>()

    override suspend fun browse(query: BrowseQuery): Result<ListingPage> {
        browsedOffsets += query.offset
        browsedCategories += query.category
        browsedSorts += "${query.sort}-${query.order}"
        return browseResult
    }

    override suspend fun featured(): Result<List<ListingSummary>> = Result.success(emptyList())

    override suspend fun sections(limit: Int): Result<List<CategorySection>> =
        Result.success(emptyList())

    override suspend fun search(query: String): Result<List<ListingSummary>> {
        searchedQueries += query
        return searchResult
    }

    override suspend fun detail(slug: String): Result<ListingDetail> =
        Result.failure(TypedFailure(FailureReason.NotFound))
}

internal class FakeRecentSearchStore : RecentSearchStore {
    private val entries = MutableStateFlow<List<String>>(emptyList())

    override val recentSearches: Flow<List<String>> = entries

    fun snapshot(): List<String> = entries.value

    override suspend fun remember(query: String) {
        entries.value = (listOf(query) + entries.value.filterNot { it == query }).take(8)
    }

    override suspend fun forget(query: String) {
        entries.value = entries.value.filterNot { it == query }
    }

    override suspend fun clear() {
        entries.value = emptyList()
    }
}
