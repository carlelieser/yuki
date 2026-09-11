package app.yuki.core.network

import kotlinx.serialization.Serializable

@Serializable
internal data class ListingSummaryDto(
    val id: String,
    val githubRepoId: Long,
    val slug: String,
    val title: String,
    val author: String,
    val description: String? = null,
    val iconUrl: String? = null,
    val bannerUrl: String? = null,
    val category: String? = null,
    val stars: Int,
    val ratingAverage: Double? = null,
    val ratingCount: Int = 0,
)

@Serializable
internal data class ListingDetailDto(
    val id: String,
    val githubRepoId: Long,
    val slug: String,
    val title: String,
    val author: String,
    val description: String? = null,
    val iconUrl: String? = null,
    val bannerUrl: String? = null,
    val category: String? = null,
    val stars: Int,
    val ratingAverage: Double? = null,
    val ratingCount: Int = 0,
    val authorUrl: String,
    val repositoryUrl: String,
    val homepageUrl: String? = null,
    val license: String? = null,
    val isArchived: Boolean,
    val screenshots: List<ScreenshotDto> = emptyList(),
    val versions: List<ListingVersionDto> = emptyList(),
)

@Serializable
internal data class ScreenshotDto(
    val url: String,
    val alt: String? = null,
)

@Serializable
internal data class ListingVersionDto(
    val tag: String,
    val name: String? = null,
    val downloadUrl: String? = null,
    val assetName: String? = null,
    val isPrerelease: Boolean,
    val publishedAt: String? = null,
)

@Serializable
internal data class ListingPageDto(
    val results: List<ListingSummaryDto> = emptyList(),
    val hasMore: Boolean = false,
)

@Serializable
internal data class SearchResultsDto(
    val results: List<ListingSummaryDto> = emptyList(),
)

@Serializable
internal data class CategorySectionsDto(
    val sections: List<CategorySectionDto> = emptyList(),
)

@Serializable
internal data class CategorySectionDto(
    val category: String? = null,
    val results: List<ListingSummaryDto> = emptyList(),
)
