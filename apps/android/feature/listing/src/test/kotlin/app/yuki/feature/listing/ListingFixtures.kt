package app.yuki.feature.listing

import app.yuki.core.model.CatalogPackage
import app.yuki.core.model.CategorySection
import app.yuki.core.model.FailureAware
import app.yuki.core.model.FailureReason
import app.yuki.core.model.ListingDetail
import app.yuki.core.model.ListingLinks
import app.yuki.core.model.ListingPage
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.ListingVersion
import app.yuki.core.model.Screenshot
import app.yuki.core.network.BrowseQuery
import app.yuki.core.network.ListingRepository
import app.yuki.core.network.SearchQuery
import java.time.Instant

internal const val SLUG = "aurora"

internal fun summary(
    id: String = "listing-1",
    githubRepoId: Long = 42L,
    slug: String = SLUG,
    title: String = "Aurora",
    author: String = "nightsky",
): ListingSummary = ListingSummary(
    id = id,
    githubRepoId = githubRepoId,
    slug = slug,
    title = title,
    author = author,
    description = "A calm launcher.",
    iconUrl = "https://cdn.test/icon.png",
    bannerUrl = "https://cdn.test/banner.png",
    category = null,
    stars = 128,
)

internal fun version(
    tag: String,
    isPrerelease: Boolean = false,
    downloadUrl: String? = "https://cdn.test/$tag.apk",
    publishedAt: Instant? = Instant.parse("2026-01-01T00:00:00Z"),
): ListingVersion = ListingVersion(
    tag = tag,
    name = "Aurora $tag",
    downloadUrl = downloadUrl,
    assetName = "aurora.apk",
    isPrerelease = isPrerelease,
    publishedAt = publishedAt,
)

internal fun detail(
    versions: List<ListingVersion> = listOf(version("v2.0.0")),
    isArchived: Boolean = false,
    license: String? = "MIT",
    screenshots: List<Screenshot> = listOf(Screenshot("https://cdn.test/one.png", "Home")),
    summary: ListingSummary = summary(),
): ListingDetail = ListingDetail(
    summary = summary,
    links = ListingLinks(
        authorUrl = "https://github.com/nightsky",
        repositoryUrl = "https://github.com/nightsky/aurora",
        homepageUrl = "https://aurora.test",
    ),
    license = license,
    isArchived = isArchived,
    screenshots = screenshots,
    versions = versions,
)

internal class TypedFailure(override val reason: FailureReason) :
    RuntimeException("listing failed"), FailureAware

internal class FakeListingRepository(
    private val result: Result<ListingDetail>,
    private val browseResult: Result<ListingPage> = Result.success(ListingPage(emptyList(), false)),
) : ListingRepository {
    var detailCallCount: Int = 0
        private set

    val browsedQueries: MutableList<BrowseQuery> = mutableListOf()

    override suspend fun browse(query: BrowseQuery): Result<ListingPage> {
        browsedQueries.add(query)
        return browseResult
    }

    override suspend fun featured(): Result<List<ListingSummary>> =
        error("featured is not used by the listing screen")

    override suspend fun sections(limit: Int): Result<List<CategorySection>> =
        error("sections is not used by the listing screen")

    override suspend fun packages(): Result<List<CatalogPackage>> =
        error("packages is not used by the listing screen")

    override suspend fun search(query: SearchQuery): Result<List<ListingSummary>> =
        error("search is not used by the listing screen")

    override suspend fun detail(slug: String): Result<ListingDetail> {
        detailCallCount += 1
        return result
    }
}
