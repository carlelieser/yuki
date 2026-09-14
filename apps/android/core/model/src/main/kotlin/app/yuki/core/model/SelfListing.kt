package app.yuki.core.model

data class SelfListing(
    val githubRepoId: Long,
    val slug: String,
    val versionName: String,
)

fun resolveInstalledTag(versionName: String, versions: List<ListingVersion>): String? {
    val installed = parseVersion(versionName) ?: return null

    return versions
        .map(ListingVersion::tag)
        .firstOrNull { tag -> parseVersion(tag) == installed }
}
