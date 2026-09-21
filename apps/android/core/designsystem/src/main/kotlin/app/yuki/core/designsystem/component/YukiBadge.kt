package app.yuki.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import app.yuki.core.designsystem.theme.YukiShape
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing

enum class BadgeTone {
    Neutral,
    Error,
}

data class BadgeContent(
    val label: String,
    val icon: ImageVector,
    val description: String,
    val tone: BadgeTone = BadgeTone.Neutral,
    val onClick: (() -> Unit)? = null,
)

@Composable
fun YukiBadge(content: BadgeContent, modifier: Modifier = Modifier) {
    val container = when (content.tone) {
        BadgeTone.Neutral -> MaterialTheme.colorScheme.surfaceContainerHigh
        BadgeTone.Error -> MaterialTheme.colorScheme.errorContainer
    }
    val foreground = when (content.tone) {
        BadgeTone.Neutral -> MaterialTheme.colorScheme.onSurfaceVariant
        BadgeTone.Error -> MaterialTheme.colorScheme.onErrorContainer
    }

    val onClick = content.onClick

    Row(
        modifier = modifier
            .clip(YukiShape.Pill)
            .background(container)
            .then(
                if (onClick == null) {
                    Modifier
                } else {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                },
            )
            .defaultMinSize(minHeight = YukiSize.BadgeHeight)
            .padding(horizontal = YukiSpacing.Small)
            .semantics(mergeDescendants = true) { contentDescription = content.description },
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = content.icon,
            contentDescription = null,
            tint = foreground,
            modifier = Modifier
                .size(YukiSize.IconTiny)
                .clearAndSetSemantics { },
        )
        Text(
            text = content.label,
            style = MaterialTheme.typography.labelSmall,
            color = foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
