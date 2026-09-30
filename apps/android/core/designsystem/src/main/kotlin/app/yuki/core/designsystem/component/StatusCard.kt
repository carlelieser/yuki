package app.yuki.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import app.yuki.core.designsystem.theme.YukiShape
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing

const val STATUS_ICON_TAG = "statusIcon"

enum class StatusTone {
    Positive,
    Neutral,
    Informative,
    Attention,
}

@Composable
fun statusIconFor(tone: StatusTone): ImageVector = when (tone) {
    StatusTone.Positive -> YukiIcons.Check
    StatusTone.Attention -> YukiIcons.Error
    StatusTone.Informative, StatusTone.Neutral -> YukiIcons.Info
}

data class StatusAction(
    val label: String,
    val onClick: () -> Unit,
)

data class StatusContent(
    val title: String,
    val description: String,
    val tone: StatusTone = StatusTone.Neutral,
    val action: StatusAction? = null,
    val icon: ImageVector? = null,
)

@Composable
private fun containerFor(tone: StatusTone): Color = when (tone) {
    StatusTone.Positive -> MaterialTheme.colorScheme.tertiaryContainer
    StatusTone.Neutral -> MaterialTheme.colorScheme.surfaceContainerHigh
    StatusTone.Informative -> MaterialTheme.colorScheme.secondaryContainer
    StatusTone.Attention -> MaterialTheme.colorScheme.errorContainer
}

@Composable
private fun contentFor(tone: StatusTone): Color = when (tone) {
    StatusTone.Positive -> MaterialTheme.colorScheme.onTertiaryContainer
    StatusTone.Neutral -> MaterialTheme.colorScheme.onSurface
    StatusTone.Informative -> MaterialTheme.colorScheme.onSecondaryContainer
    StatusTone.Attention -> MaterialTheme.colorScheme.onErrorContainer
}

@Composable
fun StatusCard(
    content: StatusContent,
    modifier: Modifier = Modifier,
) {
    Card(
        shape = YukiShape.Card,
        colors = CardDefaults.cardColors(
            containerColor = containerFor(content.tone),
            contentColor = contentFor(content.tone),
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(YukiSpacing.Large),
            horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Medium),
        ) {
            content.icon?.let { icon -> StatusIcon(icon = icon) }
            StatusText(content = content, modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatusIcon(icon: ImageVector) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier
            .size(YukiSize.IconSmall)
            .testTag(STATUS_ICON_TAG),
    )
}

@Composable
private fun StatusText(content: StatusContent, modifier: Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.Small),
    ) {
        Text(text = content.title, style = MaterialTheme.typography.titleMedium)
        Text(text = content.description, style = MaterialTheme.typography.bodyMedium)

        val action = content.action ?: return@Column
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            YukiTextButton(label = action.label, onClick = action.onClick)
        }
    }
}
