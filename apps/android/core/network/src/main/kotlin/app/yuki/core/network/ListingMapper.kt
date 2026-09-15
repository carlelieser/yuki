package app.yuki.core.network

import app.yuki.core.model.CatalogPackage
import app.yuki.core.model.CategorySection
import app.yuki.core.model.ListingDetail
import app.yuki.core.model.ListingLinks
import app.yuki.core.model.ListingPage
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.ListingVersion
import app.yuki.core.model.Screenshot
import app.yuki.core.model.readListingCategory
import java.time.Instant
import java.time.format.DateTimeParseException

internal fun ListingSummaryDto.toDomain(): ListingSummary = ListingSummary(
    id = id,
    githubRepoId = githubRepoId,
    slug = slug,
    title = title,
    author = author,
    description = description,
    iconUrl = iconUrl,
    bannerUrl = bannerUrl,
    category = readListingCategory(category),
    stars = stars,
    ratingAverage = ratingAverage,
    ratingCount = ratingCount,
)

internal fun ListingPageDto.toDomain(): ListingPage = ListingPage(
    results = results.map(ListingSummaryDto::toDomain),
    hasMore = hasMore,
)

internal fun CategorySectionsDto.toDomain(): List<CategorySection> =
    sections.mapNotNull(CategorySectionDto::toDomain)

private fun CategorySectionDto.toDomain(): CategorySection? {
    val known = readListingCategory(category) ?: return null

    return CategorySection(
        category = known,
        results = results.map(ListingSummaryDto::toDomain),
    )
}

internal fun ListingDetailDto.toDomain(): ListingDetail = ListingDetail(
    summary = toSummary(),
    links = ListingLinks(authorUrl, repositoryUrl, homepageUrl),
    license = license,
    isArchived = isArchived,
    screenshots = screenshots.map(ScreenshotDto::toDomain),
    versions = versions.map(ListingVersionDto::toDomain),
)

private fun ListingDetailDto.toSummary(): ListingSummary = ListingSummary(
    id = id,
    githubRepoId = githubRepoId,
    slug = slug,
    title = title,
    author = author,
    description = description,
    iconUrl = iconUrl,
    bannerUrl = bannerUrl,
    category = readListingCategory(category),
    stars = stars,
    ratingAverage = ratingAverage,
    ratingCount = ratingCount,
)

internal fun ScreenshotDto.toDomain(): Screenshot = Screenshot(url, alt)

internal fun CatalogPackageDto.toDomain(): CatalogPackage = CatalogPackage(
    packageName = packageName,
    githubRepoId = githubRepoId,
    slug = slug,
    title = title,
    iconUrl = iconUrl,
)

internal fun ListingVersionDto.toDomain(): ListingVersion = ListingVersion(
    tag = tag,
    name = name,
    downloadUrl = downloadUrl,
    assetName = assetName,
    isPrerelease = isPrerelease,
    publishedAt = publishedAt?.let(::parseTimestamp),
)

private fun parseTimestamp(raw: String): Instant? = try {
    Instant.parse(raw)
} catch (_: DateTimeParseException) {
    null
}
