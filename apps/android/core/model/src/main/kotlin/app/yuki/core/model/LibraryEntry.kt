package app.yuki.core.model

data class LibraryEntry(
    val githubRepoId: Long,
    val packageName: String?,
    val slug: String,
    val title: String,
    val iconUrl: String?,
    val versionTag: String,
)
