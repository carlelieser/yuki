package app.yuki.feature.explore

import app.yuki.core.model.CategorySection
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState

data class ExploreContent(
    val featured: UiState<List<ListingSummary>>,
    val sections: UiState<List<CategorySection>>,
)
