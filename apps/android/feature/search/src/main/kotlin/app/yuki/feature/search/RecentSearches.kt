package app.yuki.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import app.yuki.core.designsystem.component.SectionHeader
import app.yuki.core.designsystem.component.SectionHeaderVariant
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.theme.YukiSpacing

const val RECENT_SEARCHES_TAG = "recentSearches"

data class RecentSearchActions(
    val onSelect: (String) -> Unit,
    val onRemove: (String) -> Unit,
)

@Composable
internal fun RecentSearches(
    entries: List<String>,
    actions: RecentSearchActions,
    modifier: Modifier = Modifier,
) {
    if (entries.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = YukiSpacing.Medium)
            .testTag(RECENT_SEARCHES_TAG),
    ) {
        SectionHeader(
            title = stringResource(R.string.search_recent_title),
            variant = SectionHeaderVariant.Overline,
        )

        entries.forEach { entry ->
            RecentSearchRow(entry = entry, actions = actions)
        }
    }
}

@Composable
private fun RecentSearchRow(entry: String, actions: RecentSearchActions) {
    ListItem(
        leadingContent = {
            Icon(
                imageVector = YukiIcons.History,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            IconButton(onClick = { actions.onRemove(entry) }) {
                Icon(
                    imageVector = YukiIcons.Close,
                    contentDescription = stringResource(R.string.search_recent_remove),
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { actions.onSelect(entry) },
        content = { Text(text = entry) },
    )
}
