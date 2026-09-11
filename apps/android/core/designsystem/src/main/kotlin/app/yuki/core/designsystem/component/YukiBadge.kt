package app.yuki.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import app.yuki.core.designsystem.theme.YukiShape
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing

data class BadgeContent(
    val label: String,
    val icon: ImageVector,
    val description: String,
)

@Composable
fun YukiBadge(content: BadgeContent, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(YukiShape.Pill)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .defaultMinSize(minHeight = YukiSize.BadgeHeight)
            .padding(horizontal = YukiSpacing.Small)
            .semantics(mergeDescendants = true) { contentDescription = content.description },
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = content.icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(YukiSize.IconTiny)
                .clearAndSetSemantics { },
        )
        Text(
            text = content.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
