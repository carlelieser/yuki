package app.yuki.core.model

data class InstalledApp(
    val githubRepoId: Long,
    val packageName: String,
    val slug: String,
    val title: String,
    val iconUrl: String?,
    val versionTag: String,
)

data class AvailableUpdate(
    val installed: InstalledApp,
    val version: ListingVersion,
)

private fun isEligible(version: ListingVersion, includePrereleases: Boolean): Boolean {
    if (version.downloadUrl == null) return false
    return includePrereleases || !version.isPrerelease
}

private fun newestUpdate(
    installed: InstalledApp,
    detail: ListingDetail,
    includePrereleases: Boolean,
): AvailableUpdate? {
    val best = detail.versions
        .filter { version -> isEligible(version, includePrereleases) }
        .filter { version -> isNewerTag(version.tag, installed.versionTag) }
        .maxWithOrNull(::compareByTag)

    return best?.let { version -> AvailableUpdate(installed, version) }
}

private fun compareByTag(left: ListingVersion, right: ListingVersion): Int {
    val parsedLeft = parseVersion(left.tag)
    val parsedRight = parseVersion(right.tag)
    if (parsedLeft == null || parsedRight == null) return 0

    return compareVersions(parsedLeft, parsedRight)
}

fun findUpdates(
    installs: List<InstalledApp>,
    listings: Map<Long, ListingDetail>,
    includePrereleases: Boolean,
): List<AvailableUpdate> = installs.mapNotNull { installed ->
    val detail = listings[installed.githubRepoId] ?: return@mapNotNull null
    newestUpdate(installed, detail, includePrereleases)
}
