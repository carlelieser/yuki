package app.yuki.feature.updates

import app.yuki.core.model.InstalledApp
import app.yuki.core.model.ListingVersion

internal fun installedApp(
    githubRepoId: Long,
    slug: String,
    versionTag: String,
): InstalledApp = InstalledApp(
    githubRepoId = githubRepoId,
    packageName = "app.$slug",
    slug = slug,
    title = slug.replaceFirstChar(Char::uppercase),
    iconUrl = null,
    versionTag = versionTag,
)

internal fun version(tag: String): ListingVersion = ListingVersion(
    tag = tag,
    name = tag,
    downloadUrl = "https://example.test/$tag.apk",
    assetName = "$tag.apk",
    isPrerelease = false,
    publishedAt = null,
)
