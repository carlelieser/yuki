package app.yuki.feature.explore

import app.yuki.core.model.CategorySection
import app.yuki.core.model.FailureAware
import app.yuki.core.model.FailureReason
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.ListingDetail
import app.yuki.core.model.ListingPage
import app.yuki.core.model.ListingSummary
import app.yuki.core.network.BrowseQuery
import app.yuki.core.network.ListingRepository
import app.yuki.core.network.SearchQuery
import kotlinx.coroutines.CompletableDeferred

internal class TypedFailure(override val reason: FailureReason) :
    Exception("Explore test failure"), FailureAware

internal fun section(
    category: ListingCategory,
    slugs: List<String>,
): CategorySection = CategorySection(
    category = category,
    results = slugs.map(::listing),
)

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
    var featuredResult: Result<List<ListingSummary>> = Result.success(emptyList())
    var searchResult: Result<List<ListingSummary>> = Result.success(emptyList())
    var browseResult: Result<ListingPage> = Result.success(ListingPage(emptyList(), false))
    var sectionsResult: Result<List<CategorySection>> = Result.success(emptyList())

    val browsedOffsets = mutableListOf<Int>()
    val sectionLimits = mutableListOf<Int>()
    val searchedQueries = mutableListOf<String>()
    val startedCalls = mutableListOf<String>()

    var featuredGate: CompletableDeferred<Unit>? = null

    override suspend fun browse(query: BrowseQuery): Result<ListingPage> {
        browsedOffsets += query.offset
        return browseResult
    }

    override suspend fun featured(): Result<List<ListingSummary>> {
        startedCalls += FEATURED_CALL
        featuredGate?.await()
        return featuredResult
    }

    override suspend fun sections(limit: Int): Result<List<CategorySection>> {
        startedCalls += SECTIONS_CALL
        sectionLimits += limit
        return sectionsResult
    }

    override suspend fun search(query: SearchQuery): Result<List<ListingSummary>> {
        searchedQueries += query.term
        return searchResult
    }

    override suspend fun detail(slug: String): Result<ListingDetail> =
        Result.failure(TypedFailure(FailureReason.NotFound))
}

internal const val FEATURED_CALL = "featured"
internal const val SECTIONS_CALL = "sections"
