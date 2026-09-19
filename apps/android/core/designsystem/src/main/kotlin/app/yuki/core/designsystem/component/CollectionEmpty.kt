package app.yuki.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing

const val COLLECTION_EMPTY_TAG = "collectionEmpty"

data class EmptyContent(
    val title: String,
    val description: String? = null,
    val icon: ImageVector? = null,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

@Composable
private fun EmptyIcon(icon: ImageVector?) {
    if (icon == null) return

    YukiTonalCircle(diameter = YukiSize.IconLarge) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(YukiSize.IconSmall),
        )
    }
}

@Composable
private fun EmptyDescription(description: String?) {
    if (description == null) return

    Text(
        text = description,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun EmptyActionButton(content: EmptyContent) {
    val label = content.actionLabel ?: return
    val onAction = content.onAction ?: return

    YukiButton(
        label = label,
        onClick = onAction,
        modifier = Modifier.padding(top = YukiSpacing.Small),
    )
}

@Composable
fun CollectionEmpty(
    content: EmptyContent,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = YukiSpacing.ExtraLarge, vertical = YukiSpacing.Section)
            .testTag(COLLECTION_EMPTY_TAG),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.Medium),
    ) {
        EmptyIcon(icon = content.icon)
        Text(
            text = content.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        EmptyDescription(description = content.description)
        EmptyActionButton(content = content)
    }
}
