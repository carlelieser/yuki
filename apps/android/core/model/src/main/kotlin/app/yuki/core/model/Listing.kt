package app.yuki.core.model

import java.time.Instant

data class ListingSummary(
    val id: String,
    val githubRepoId: Long,
    val slug: String,
    val title: String,
    val author: String,
    val description: String?,
    val iconUrl: String?,
    val bannerUrl: String?,
    val category: ListingCategory?,
    val stars: Int,
    val ratingAverage: Double? = null,
    val ratingCount: Int = 0,
)

data class ListingDetail(
    val summary: ListingSummary,
    val links: ListingLinks,
    val license: String?,
    val isArchived: Boolean,
    val screenshots: List<Screenshot>,
    val versions: List<ListingVersion>,
) {
    val slug: String get() = summary.slug

    val githubRepoId: Long get() = summary.githubRepoId

    val title: String get() = summary.title
}

data class ListingLinks(
    val authorUrl: String,
    val repositoryUrl: String,
    val homepageUrl: String?,
)

data class Screenshot(
    val url: String,
    val alt: String?,
)

data class ScreenshotSelection(
    val index: Int,
    val urls: List<String>,
)

data class ListingVersion(
    val tag: String,
    val name: String?,
    val downloadUrl: String?,
    val assetName: String?,
    val isPrerelease: Boolean,
    val publishedAt: Instant?,
)

data class ListingPage(
    val results: List<ListingSummary>,
    val hasMore: Boolean,
)

data class CategorySection(
    val category: ListingCategory,
    val results: List<ListingSummary>,
)
