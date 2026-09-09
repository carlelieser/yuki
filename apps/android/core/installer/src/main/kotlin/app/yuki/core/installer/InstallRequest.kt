package app.yuki.core.installer

data class InstallRequest(
    val target: InstallTarget,
    val source: InstallSource,
)

data class InstallTarget(
    val githubRepoId: Long,
    val slug: String,
    val title: String,
    val iconUrl: String?,
)

data class InstallSource(
    val downloadUrl: String,
    val versionTag: String,
    val assetName: String?,
)
