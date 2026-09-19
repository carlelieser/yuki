package app.yuki.feature.library

enum class LibraryFilter(val label: String, val emptyMessage: String) {
    All("All", "Your library is empty."),
    Installed("Installed", "Nothing in your library is installed on this device."),
    NotInstalled("Not installed", "Everything in your library is already installed here."),
    ;

    fun accepts(item: LibraryItem): Boolean = when (this) {
        All -> true
        Installed -> item.isInstalled
        NotInstalled -> !item.isInstalled
    }
}
