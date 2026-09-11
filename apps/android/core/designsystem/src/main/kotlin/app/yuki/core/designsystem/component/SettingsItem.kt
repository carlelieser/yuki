package app.yuki.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing

data class SettingsItemContent(
    val title: String,
    val supporting: String? = null,
    val leading: (@Composable () -> Unit)? = null,
    val trailing: (@Composable () -> Unit)? = null,
    val belowText: (@Composable () -> Unit)? = null,
)

@Composable
private fun SettingsItemText(content: SettingsItemContent, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall),
    ) {
        Text(
            text = content.title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )

        if (content.supporting != null) {
            Text(
                text = content.supporting,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
        }

        content.belowText?.invoke()
    }
}

@Composable
fun SettingsItem(
    content: SettingsItemContent,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val clickable = if (onClick == null) Modifier else Modifier.clickable(onClick = onClick)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickable)
            .defaultMinSize(minHeight = YukiSize.MinimumTouchTarget)
            .padding(horizontal = YukiSpacing.Large, vertical = YukiSpacing.Medium),
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Large),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        content.leading?.invoke()
        SettingsItemText(content = content, modifier = Modifier.weight(1f))
        content.trailing?.invoke()
    }
}
