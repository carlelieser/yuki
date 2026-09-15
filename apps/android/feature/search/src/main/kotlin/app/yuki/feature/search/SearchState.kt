package app.yuki.feature.search

import app.yuki.core.designsystem.component.ListingInstalls
import app.yuki.core.model.InstallState
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState

data class SearchContent(
    val query: String,
    val recent: List<String>,
    val results: UiState<List<ListingSummary>>?,
    val filter: BrowseFilter,
    val installedIds: Set<Long> = emptySet(),
    val installStates: Map<Long, InstallState> = emptyMap(),
) {
    val installs: ListingInstalls get() = ListingInstalls(installedIds, installStates)

    val isSearching: Boolean get() = results != null

    val category: ListingCategory? get() = filter.category

    val sort: BrowseSortOption get() = filter.sort
}
