package app.yuki.feature.library

enum class LibraryFilter(
    val label: String,
    val emptyTitle: String,
    val emptyDescription: String,
) {
    All(
        label = "All",
        emptyTitle = "Your library is empty",
        emptyDescription = "Apps you install show up here.",
    ),
    Installed(
        label = "Installed",
        emptyTitle = "No installed apps",
        emptyDescription = "Install an app to see it here.",
    ),
    NotInstalled(
        label = "Not installed",
        emptyTitle = "Nothing left to install",
        emptyDescription = "All apps are already installed.",
    ),
    ;

    fun accepts(item: LibraryItem): Boolean = when (this) {
        All -> true
        Installed -> item.isInstalled
        NotInstalled -> !item.isInstalled
    }
}
