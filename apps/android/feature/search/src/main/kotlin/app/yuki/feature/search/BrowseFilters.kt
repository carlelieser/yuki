package app.yuki.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingCategory

const val CATEGORY_FILTER_TAG = "categoryFilter"
const val SORT_SELECTOR_TAG = "sortSelector"

internal const val ALL_CATEGORIES_LABEL = "All"

private val CategoryFilterEntries: List<ListingCategory?> =
    listOf(null) + ListingCategory.entries

@Composable
internal fun CategoryFilter(
    selected: ListingCategory?,
    onCategorySelected: (ListingCategory?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.testTag(CATEGORY_FILTER_TAG),
        contentPadding = PaddingValues(horizontal = YukiSpacing.Large),
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Small),
    ) {
        items(items = CategoryFilterEntries, key = { category -> category?.wireValue ?: "all" }) {
            CategoryChip(
                category = it,
                isSelected = it == selected,
                onSelect = { onCategorySelected(it) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    category: ListingCategory?,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    FilterChip(
        selected = isSelected,
        onClick = onSelect,
        label = { Text(text = category?.label ?: ALL_CATEGORIES_LABEL) },
    )
}

@Composable
internal fun SortSelector(
    selected: BrowseSortOption,
    onSortSelected: (BrowseSortOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }

    TextButton(
        onClick = { isExpanded = true },
        modifier = modifier.testTag(SORT_SELECTOR_TAG),
    ) {
        Text(text = selected.label)
    }

    DropdownMenu(expanded = isExpanded, onDismissRequest = { isExpanded = false }) {
        BrowseSortOption.entries.forEach { option ->
            DropdownMenuItem(
                text = { Text(text = option.label) },
                onClick = {
                    isExpanded = false
                    onSortSelected(option)
                },
            )
        }
    }
}
