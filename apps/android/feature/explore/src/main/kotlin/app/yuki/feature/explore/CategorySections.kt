package app.yuki.feature.explore

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.yuki.core.designsystem.component.ClickableProductListItem
import app.yuki.core.designsystem.component.CollectionEmpty
import app.yuki.core.designsystem.component.EmptyContent
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.component.ListingInstalls
import app.yuki.core.designsystem.component.SectionHeader
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.component.toProductListItemContent
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

    item(key = category.wireValue) {
        CategorySectionHeader(
            category = category,
            onSeeAll = { actions.onCategorySelected(category) },
        )
    }

    items(
        items = section.results,
        key = { listing -> "${category.wireValue}/${listing.id}" },
    ) { listing ->
        ClickableProductListItem(
            content = installs.apply(listing, listing.toProductListItemContent()),
            onClick = { actions.onListingSelected(listing) },
        )
    }
}

@Composable
private fun CategorySectionHeader(category: ListingCategory, onSeeAll: () -> Unit) {
    SectionHeader(
        title = category.label,
        action = {
            IconButton(onClick = onSeeAll) {
                Icon(
                    imageVector = YukiIcons.Forward,
                    contentDescription = "$CATEGORY_SECTION_ARROW_DESCRIPTION ${category.label}",
                )
            }
        },
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
