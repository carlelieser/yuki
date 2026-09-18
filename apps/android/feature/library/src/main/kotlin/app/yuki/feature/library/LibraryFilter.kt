package app.yuki.feature.library

enum class LibraryFilter(val label: String) {
    All("All"),
    Installed("Installed"),
    NotInstalled("Not installed"),
    ;

    fun accepts(item: LibraryItem): Boolean = when (this) {
        All -> true
        Installed -> item.isInstalled
        NotInstalled -> !item.isInstalled
    }
}
