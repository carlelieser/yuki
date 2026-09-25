package app.yuki.feature.library

import androidx.annotation.StringRes

internal data class LibraryFilterCopy(
    @StringRes val label: Int,
    @StringRes val emptyTitle: Int,
    @StringRes val emptyDescription: Int,
)

private val filterCopy: Map<LibraryFilter, LibraryFilterCopy> = mapOf(
    LibraryFilter.All to LibraryFilterCopy(
        label = R.string.library_filter_all,
        emptyTitle = R.string.library_filter_all_empty_title,
        emptyDescription = R.string.library_filter_all_empty_description,
    ),
    LibraryFilter.Installed to LibraryFilterCopy(
        label = R.string.library_filter_installed,
        emptyTitle = R.string.library_filter_installed_empty_title,
        emptyDescription = R.string.library_filter_installed_empty_description,
    ),
    LibraryFilter.NotInstalled to LibraryFilterCopy(
        label = R.string.library_filter_not_installed,
        emptyTitle = R.string.library_filter_not_installed_empty_title,
        emptyDescription = R.string.library_filter_not_installed_empty_description,
    ),
)

internal val LibraryFilter.copy: LibraryFilterCopy get() = filterCopy.getValue(this)
