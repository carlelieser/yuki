package app.yuki.feature.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingCategory

const val CATEGORY_FILTER_TAG = "categoryFilter"
const val SORT_SELECTOR_TAG = "sortSelector"
const val SORT_DESCRIPTION = "Sort by"

internal const val ALL_CATEGORIES_LABEL = "All"

private val BrowseSortIcon: ImageVector = ImageVector.Builder(
    name = "Sort",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(3f, 18f)
        horizontalLineToRelative(6f)
        verticalLineToRelative(-2f)
        horizontalLineTo(3f)
        close()
        moveTo(3f, 6f)
        verticalLineToRelative(2f)
        horizontalLineToRelative(18f)
        verticalLineTo(6f)
        close()
        moveTo(3f, 13f)
        horizontalLineToRelative(12f)
        verticalLineToRelative(-2f)
        horizontalLineTo(3f)
        close()
    }
}.build()

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

    Box(modifier = modifier) {
        IconButton(
            onClick = { isExpanded = true },
            modifier = Modifier.testTag(SORT_SELECTOR_TAG),
        ) {
            Icon(
                imageVector = BrowseSortIcon,
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
