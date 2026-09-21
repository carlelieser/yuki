package app.yuki.feature.explore

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.yuki.core.designsystem.component.CollectionEmpty
import app.yuki.core.designsystem.component.EmptyContent
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.component.LISTING_SECTION_ARROW_DESCRIPTION
import app.yuki.core.designsystem.component.ListingInstalls
import app.yuki.core.designsystem.component.ListingSectionActions
import app.yuki.core.designsystem.component.ListingSectionContent
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.component.listingSection
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.CategorySection
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState

const val CATEGORY_SECTIONS_TAG = "categorySections"
const val CATEGORY_SECTION_ARROW_DESCRIPTION = LISTING_SECTION_ARROW_DESCRIPTION

private val NothingToExplore = EmptyContent(
    title = "No apps yet",
    description = "There is nothing to explore right now. Pull to refresh shortly.",
)

internal data class CategorySectionActions(
    val onListingSelected: (ListingSummary) -> Unit,
    val onCategorySelected: (ListingCategory) -> Unit,
    val onAuthorSelected: ((String) -> Unit)? = null,
)

internal fun LazyListScope.categorySections(
    sections: UiState<List<CategorySection>>,
    installs: ListingInstalls,
    actions: CategorySectionActions,
) {
    when (sections) {
        is UiState.Loading -> item(key = SECTIONS_STATUS_KEY) {
            SectionsLoading(modifier = Modifier.animateItem())
        }

        is UiState.Failure -> item(key = SECTIONS_STATUS_KEY) {
            SectionsFailure(sections = sections, modifier = Modifier.animateItem())
        }

        is UiState.Success -> sectionList(
            entries = sections.data,
            installs = installs,
            actions = actions,
        )
    }
}

private fun LazyListScope.sectionList(
    entries: List<CategorySection>,
    installs: ListingInstalls,
    actions: CategorySectionActions,
) {
    if (entries.isEmpty()) {
        item(key = SECTIONS_STATUS_KEY) { SectionsEmpty(modifier = Modifier.animateItem()) }
        return
    }

    entries.forEach { entry ->
        categorySection(section = entry, installs = installs, actions = actions)
    }
}

private fun LazyListScope.categorySection(
    section: CategorySection,
    installs: ListingInstalls,
    actions: CategorySectionActions,
) {
    val category = section.category

    listingSection(
        content = ListingSectionContent(
            title = category.label,
            keyPrefix = category.wireValue,
            listings = section.results,
            installs = installs,
        ),
        actions = ListingSectionActions(
            onListingSelected = actions.onListingSelected,
            onSeeAll = { actions.onCategorySelected(category) },
            onAuthorSelected = actions.onAuthorSelected,
        ),
    )
}

private const val SECTIONS_STATUS_KEY = "categorySectionsStatus"

@Composable
private fun SectionsLoading(modifier: Modifier = Modifier) {
    YukiLoadingIndicator(
        modifier = modifier
            .fillMaxWidth()
            .padding(YukiSpacing.ExtraLarge),
    )
}

@Composable
private fun SectionsFailure(sections: UiState.Failure, modifier: Modifier = Modifier) {
    FailureState(
        reason = sections.reason,
        modifier = modifier.padding(YukiSpacing.Large),
    )
}

@Composable
private fun SectionsEmpty(modifier: Modifier = Modifier) {
    CollectionEmpty(
        content = NothingToExplore,
        modifier = modifier.padding(YukiSpacing.Large),
    )
}
