package app.yuki.feature.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.yuki.core.designsystem.component.SectionHeader
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.theme.YukiSpacing

const val RECENT_SEARCHES_TAG = "recentSearches"
const val RECENT_REMOVE_DESCRIPTION = "Remove recent search"

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
            .testTag(RECENT_SEARCHES_TAG),
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall),
    ) {
        SectionHeader(title = "Recent searches")

        entries.forEach { entry ->
            RecentSearchRow(entry = entry, actions = actions)
        }
    }
}

@Composable
private fun RecentSearchRow(entry: String, actions: RecentSearchActions) {
    ListItem(
        trailingContent = {
            IconButton(onClick = { actions.onRemove(entry) }) {
                Icon(
                    imageVector = YukiIcons.Close,
                    contentDescription = RECENT_REMOVE_DESCRIPTION,
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clickable { actions.onSelect(entry) }
            .padding(horizontal = YukiSpacing.Small),
        content = { Text(text = entry) },
    )
}
