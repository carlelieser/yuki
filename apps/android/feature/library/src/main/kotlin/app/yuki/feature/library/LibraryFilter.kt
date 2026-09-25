package app.yuki.feature.library

enum class LibraryFilter {
    All,
    Installed,
    NotInstalled,
    ;

    fun accepts(item: LibraryItem): Boolean = when (this) {
        All -> true
        Installed -> item.isInstalled
        NotInstalled -> !item.isInstalled
    }
}
