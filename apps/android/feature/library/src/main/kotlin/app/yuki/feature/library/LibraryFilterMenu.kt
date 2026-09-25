package app.yuki.feature.library

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.ui.res.stringResource
import app.yuki.core.designsystem.component.YukiIcons

const val LIBRARY_FILTER_TAG = "libraryFilter"

@Composable
internal fun LibraryFilterSelector(
    selected: LibraryFilter,
    onFilterSelected: (LibraryFilter) -> Unit,
    modifier: Modifier = Modifier,
) {
    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { isExpanded = true },
            modifier = Modifier.testTag(LIBRARY_FILTER_TAG),
        ) {
            Icon(
                imageVector = YukiIcons.Sort,
                contentDescription = stringResource(
                    R.string.library_filter_description,
                    stringResource(selected.copy.label),
                ),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        LibraryFilterMenu(
            isExpanded = isExpanded,
            onDismiss = { isExpanded = false },
            onFilterSelected = { filter ->
                isExpanded = false
                onFilterSelected(filter)
            },
        )
    }
}

@Composable
private fun LibraryFilterMenu(
    isExpanded: Boolean,
    onDismiss: () -> Unit,
    onFilterSelected: (LibraryFilter) -> Unit,
) {
    DropdownMenu(expanded = isExpanded, onDismissRequest = onDismiss) {
        LibraryFilter.entries.forEach { filter ->
            DropdownMenuItem(
                text = { Text(text = stringResource(filter.copy.label)) },
                onClick = { onFilterSelected(filter) },
            )
        }
    }
}
