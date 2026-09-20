package app.yuki.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemColors
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import app.yuki.core.designsystem.theme.YukiSpacing

data class SettingsRowPosition(val index: Int, val count: Int)

@Composable
fun SettingsGroup(
    modifier: Modifier = Modifier,
    label: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.ExtraSmall),
    ) {
        if (label != null) {
            SectionHeader(title = label, variant = SectionHeaderVariant.Overline)
        }

        content()
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsRow(
    position: SettingsRowPosition,
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val shapes = ListItemDefaults.segmentedShapes(index = position.index, count = position.count)
    val colors = settingsRowColors()
    val leading = icon?.let { vector -> @Composable { SettingsRowIcon(vector) } }
    val supportingContent = supporting?.let { text -> @Composable { Text(text = text) } }

    if (onClick == null) {
        SegmentedListItem(
            shapes = shapes,
            modifier = modifier,
            colors = colors,
            leadingContent = leading,
            trailingContent = trailing,
            supportingContent = supportingContent,
        ) {
            Text(text = title)
        }
        return
    }

    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier,
        colors = colors,
        leadingContent = leading,
        trailingContent = trailing,
        supportingContent = supportingContent,
    ) {
        Text(text = title)
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SettingsSlotRow(
    position: SettingsRowPosition,
    modifier: Modifier = Modifier,
    colors: ListItemColors? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val shapes = ListItemDefaults.segmentedShapes(index = position.index, count = position.count)
    val rowColors = colors ?: settingsRowColors()

    if (onClick == null) {
        SegmentedListItem(
            shapes = shapes,
            modifier = modifier,
            colors = rowColors,
            content = content,
        )
        return
    }

    SegmentedListItem(
        onClick = onClick,
        shapes = shapes,
        modifier = modifier,
        colors = rowColors,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun settingsRowColors(): ListItemColors = ListItemDefaults.segmentedColors(
    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
)

@Composable
private fun SettingsRowIcon(icon: ImageVector) {
    Icon(imageVector = icon, contentDescription = null)
}
