package app.yuki.core.model

enum class InstallSource {
    YUKI,
    DETECTED,
}

data class InstalledApp(
    val githubRepoId: Long,
    val packageName: String,
    val slug: String,
    val title: String,
    val iconUrl: String?,
    val versionTag: String,
    val source: InstallSource = InstallSource.YUKI,
) {
    val isDetected: Boolean get() = source == InstallSource.DETECTED
}

data class AvailableUpdate(
    val installed: InstalledApp,
    val version: ListingVersion,
)

private fun isEligible(version: ListingVersion, includePrereleases: Boolean): Boolean {
    if (version.downloadUrl == null) return false
    return includePrereleases || !version.isPrerelease
}

fun matchingReleaseTag(versionName: String, versions: List<ListingVersion>): String? {
    val installed = parseVersion(versionName) ?: return null

    val matches = versions
        .map(ListingVersion::tag)
        .filter { tag -> parseVersion(tag)?.let { compareVersions(it, installed) == 0 } == true }
        .distinct()

    return matches.singleOrNull()
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
    val resolved = resolveInstalledVersion(installed, detail) ?: return@mapNotNull null

    newestUpdate(resolved, detail, includePrereleases)
}

private fun resolveInstalledVersion(
    installed: InstalledApp,
    detail: ListingDetail,
): InstalledApp? {
    if (!installed.isDetected) return installed

    val tag = matchingReleaseTag(installed.versionTag, detail.versions) ?: return null

    return installed.copy(versionTag = tag)
}
