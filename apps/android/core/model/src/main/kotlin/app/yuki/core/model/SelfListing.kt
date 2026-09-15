package app.yuki.core.model

data class SelfListing(
    val githubRepoId: Long,
    val slug: String,
    val packageName: String,
    val title: String,
    val iconUrl: String,
    val releaseTag: String,
    val versionCode: Long,
) {
    val isRelease: Boolean get() = releaseTag.isNotBlank()
}
