package app.yuki.feature.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.yuki.core.designsystem.component.BadgeContent
import app.yuki.core.designsystem.component.SectionHeader
import app.yuki.core.designsystem.component.SettingsItem
import app.yuki.core.designsystem.component.SettingsItemContent
import app.yuki.core.designsystem.component.YukiBadge
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.theme.YukiSize

internal fun LazyListScope.permissionsSection(
    rows: List<PermissionRow>,
    onPermissionClick: (PermissionRow) -> Unit,
) {
    item { SectionHeader(title = PERMISSIONS_SECTION_TITLE) }

    items(rows, key = { row -> row.permission.permission }) { row ->
        SettingsItem(
            content = SettingsItemContent(
                title = row.permission.label,
                supporting = row.permission.reason,
                trailing = { PermissionStatusIcon(row) },
                belowText = requiredBadgeFor(row),
            ),
            onClick = { onPermissionClick(row) },
        )
    }
}

private fun requiredBadgeFor(row: PermissionRow): (@Composable () -> Unit)? {
    if (!row.permission.isRequired) return null

    return { RequiredBadge() }
}

@Composable
private fun PermissionStatusIcon(row: PermissionRow) {
    Icon(
        imageVector = if (row.isGranted) YukiIcons.Check else YukiIcons.PriorityHigh,
        contentDescription = row.statusLabel,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(YukiSize.IconSmall),
    )
}

@Composable
private fun RequiredBadge() {
    YukiBadge(
        content = BadgeContent(
            label = REQUIRED_LABEL,
            icon = YukiIcons.Asterisk,
            description = REQUIRED_LABEL,
        ),
    )
}

internal const val PERMISSIONS_SECTION_TITLE = "Permissions"
internal const val REQUIRED_LABEL = "Required"
