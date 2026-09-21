package app.yuki.feature.listing

import app.yuki.core.model.ListingDetail
import app.yuki.core.model.ListingLinks
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.ListingVersion
import app.yuki.core.model.Screenshot
import java.time.Instant

internal const val SLUG = "aurora"

internal fun summary(
    bannerUrl: String? = "https://cdn.test/banner.png",
    stars: Int = 128,
    id: String = "listing-1",
    githubRepoId: Long = 42L,
    slug: String = SLUG,
    title: String = "Aurora",
): ListingSummary = ListingSummary(
    id = id,
    githubRepoId = githubRepoId,
    slug = slug,
    title = title,
    author = "nightsky",
    description = "A calm launcher.",
    iconUrl = "https://cdn.test/icon.png",
    bannerUrl = bannerUrl,
    category = null,
    stars = stars,
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
