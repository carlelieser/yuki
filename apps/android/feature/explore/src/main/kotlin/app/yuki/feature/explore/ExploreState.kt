package app.yuki.feature.explore

import app.yuki.core.designsystem.component.ListingInstalls
import app.yuki.core.model.CategorySection
import app.yuki.core.model.InstallState
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState

data class ExploreRows(
    val featured: UiState<List<ListingSummary>>,
    val newest: UiState<List<ListingSummary>>,
    val updated: UiState<List<ListingSummary>>,
)

data class ExploreContent(
    val featured: UiState<List<ListingSummary>>,
    val sections: UiState<List<CategorySection>>,
    val newest: UiState<List<ListingSummary>> = UiState.Success(emptyList()),
    val updated: UiState<List<ListingSummary>> = UiState.Success(emptyList()),
    val installedIds: Set<Long> = emptySet(),
    val installStates: Map<Long, InstallState> = emptyMap(),
) {
    val installs: ListingInstalls get() = ListingInstalls(installedIds, installStates)
}
