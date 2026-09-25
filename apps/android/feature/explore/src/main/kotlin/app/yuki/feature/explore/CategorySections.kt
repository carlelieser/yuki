package app.yuki.feature.explore

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.yuki.core.designsystem.component.CollectionEmpty
import app.yuki.core.designsystem.component.EmptyContent
import app.yuki.core.designsystem.component.FailureState
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

private val NothingToExplore = EmptyContent(
    title = "No apps yet",
    description = "There is nothing to explore right now. Pull to refresh shortly.",
)

internal data class CategorySectionActions(
    val onListingSelected: (ListingSummary) -> Unit,
    val onCategorySelected: (ListingCategory) -> Unit,
    val onAuthorSelected: ((String) -> Unit)? = null,
)

internal data class CategorySectionsContent(
    val sections: UiState<List<CategorySection>>,
    val installs: ListingInstalls,
    val labels: Map<ListingCategory, String>,
)

internal fun LazyListScope.categorySections(
    content: CategorySectionsContent,
    actions: CategorySectionActions,
) {
    when (val sections = content.sections) {
        is UiState.Loading -> item(key = SECTIONS_STATUS_KEY) {
            SectionsLoading(modifier = Modifier.animateItem())
        }

        is UiState.Failure -> item(key = SECTIONS_STATUS_KEY) {
            SectionsFailure(sections = sections, modifier = Modifier.animateItem())
        }

        is UiState.Success -> sectionList(
            content = content,
            entries = sections.data,
            actions = actions,
        )
    }
}

private fun LazyListScope.sectionList(
    content: CategorySectionsContent,
    entries: List<CategorySection>,
    actions: CategorySectionActions,
) {
    if (entries.isEmpty()) {
        item(key = SECTIONS_STATUS_KEY) { SectionsEmpty(modifier = Modifier.animateItem()) }
        return
    }

    entries.forEach { entry ->
        categorySection(section = entry, content = content, actions = actions)
    }
}

private fun LazyListScope.categorySection(
    section: CategorySection,
    content: CategorySectionsContent,
    actions: CategorySectionActions,
) {
    val category = section.category

    listingSection(
        content = ListingSectionContent(
            title = content.labels.getValue(category),
            keyPrefix = category.wireValue,
            listings = section.results,
            installs = content.installs,
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
