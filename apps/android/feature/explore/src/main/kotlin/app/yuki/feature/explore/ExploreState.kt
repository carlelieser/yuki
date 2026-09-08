package app.yuki.feature.explore

import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState

data class ExploreContent(
    val featured: UiState<List<ListingSummary>>,
    val search: SearchState,
)

data class SearchState(
    val query: String,
    val recent: List<String>,
    val results: UiState<List<ListingSummary>>?,
) {
    val isSearching: Boolean get() = results != null
}
