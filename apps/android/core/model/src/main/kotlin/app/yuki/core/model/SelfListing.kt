package app.yuki.core.model

data class SelfListing(
    val githubRepoId: Long,
    val slug: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
)

fun resolveInstalledTag(versionName: String, versions: List<ListingVersion>): String? {
    val installed = parseVersion(versionName) ?: return null

    return versions
        .map(ListingVersion::tag)
        .firstOrNull { tag -> parseVersion(tag) == installed }
}
