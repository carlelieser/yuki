package app.yuki.feature.explore

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.yuki.core.designsystem.component.CollectionEmpty
import app.yuki.core.designsystem.component.EmptyContent
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.component.ProductCard
import app.yuki.core.designsystem.component.SectionHeader
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.CategorySection
import app.yuki.core.model.ListingCategory
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.UiState

const val CATEGORY_SECTIONS_TAG = "categorySections"
const val CATEGORY_SECTION_ARROW_DESCRIPTION = "See all"

private val NothingToExplore = EmptyContent(
    title = "No apps yet",
    description = "There is nothing to explore right now. Pull to refresh shortly.",
)

internal data class CategorySectionActions(
    val onListingSelected: (ListingSummary) -> Unit,
    val onCategorySelected: (ListingCategory) -> Unit,
)

internal fun LazyListScope.categorySections(
    sections: UiState<List<CategorySection>>,
    actions: CategorySectionActions,
) {
    when (sections) {
        is UiState.Loading -> item { SectionsLoading() }

        is UiState.Failure -> item { SectionsFailure(sections) }

        is UiState.Success -> sectionList(entries = sections.data, actions = actions)
    }
}

private fun LazyListScope.sectionList(
    entries: List<CategorySection>,
    actions: CategorySectionActions,
) {
    if (entries.isEmpty()) {
        item { SectionsEmpty() }
        return
    }

    entries.forEach { entry ->
        item(key = entry.category.wireValue) {
            CategorySectionBlock(section = entry, actions = actions)
        }
    }
}

@Composable
private fun CategorySectionBlock(
    section: CategorySection,
    actions: CategorySectionActions,
) {
    val category = section.category

    SectionHeader(
        title = category.label,
        action = {
            IconButton(onClick = { actions.onCategorySelected(category) }) {
                Icon(
                    imageVector = YukiIcons.Forward,
                    contentDescription = "$CATEGORY_SECTION_ARROW_DESCRIPTION ${category.label}",
                )
            }
        },
    )

    SectionCardRow(listings = section.results, onSelect = actions.onListingSelected)
}

@Composable
private fun SectionCardRow(
    listings: List<ListingSummary>,
    onSelect: (ListingSummary) -> Unit,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = YukiSpacing.Large),
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Medium),
    ) {
        items(items = listings, key = { listing -> listing.id }) { listing ->
            ProductCard(listing = listing, onClick = { onSelect(listing) })
        }
    }
}

@Composable
private fun SectionsLoading() {
    YukiLoadingIndicator(
        modifier = Modifier
            .fillMaxWidth()
            .padding(YukiSpacing.ExtraLarge),
    )
}

@Composable
private fun SectionsFailure(sections: UiState.Failure) {
    FailureState(
        reason = sections.reason,
        modifier = Modifier.padding(YukiSpacing.Large),
    )
}

@Composable
private fun SectionsEmpty() {
    CollectionEmpty(
        content = NothingToExplore,
        modifier = Modifier.padding(YukiSpacing.Large),
    )
}
