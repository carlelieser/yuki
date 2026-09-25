package app.yuki.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.component.icon
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingCategory
import app.yuki.core.designsystem.component.label

const val CATEGORY_FILTER_TAG = "categoryFilter"
const val SORT_SELECTOR_TAG = "sortSelector"
const val SORT_DESCRIPTION = "Sort by"

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
        items(
            items = ListingCategory.entries,
            key = { category -> category.wireValue },
        ) { category ->
            CategoryChip(
                category = category,
                isSelected = category == selected,
                onSelect = { onCategorySelected(toggled(category, selected)) },
            )
        }
    }
}

internal fun toggled(
    category: ListingCategory,
    selected: ListingCategory?,
): ListingCategory? = if (category == selected) null else category

@Composable
private fun CategoryChip(
    category: ListingCategory,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    FilterChip(
        selected = isSelected,
        onClick = onSelect,
        label = { Text(text = category.label()) },
        leadingIcon = { CategoryChipIcon(category = category) },
    )
}

@Composable
private fun CategoryChipIcon(category: ListingCategory) {
    Icon(
        imageVector = category.icon,
        contentDescription = null,
        modifier = Modifier.size(FilterChipDefaults.IconSize),
    )
}

@Composable
internal fun SortSelector(
    selected: BrowseSortOption,
    onSortSelected: (BrowseSortOption) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { isExpanded = true },
            modifier = Modifier.testTag(SORT_SELECTOR_TAG),
        ) {
            Icon(
                imageVector = YukiIcons.Sort,
                contentDescription = "$SORT_DESCRIPTION ${selected.label}",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SortMenu(
            isExpanded = isExpanded,
            onDismiss = { isExpanded = false },
            onSortSelected = { option ->
                isExpanded = false
                onSortSelected(option)
            },
        )
    }
}

@Composable
private fun SortMenu(
    isExpanded: Boolean,
    onDismiss: () -> Unit,
    onSortSelected: (BrowseSortOption) -> Unit,
) {
    DropdownMenu(expanded = isExpanded, onDismissRequest = onDismiss) {
        BrowseSortOption.entries.forEach { option ->
            DropdownMenuItem(
                text = { Text(text = option.label) },
                onClick = { onSortSelected(option) },
            )
        }
    }
}
