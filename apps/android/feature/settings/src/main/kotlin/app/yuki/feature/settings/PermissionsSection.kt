package app.yuki.feature.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import app.yuki.core.designsystem.component.BadgeContent
import app.yuki.core.designsystem.component.SectionHeader
import app.yuki.core.designsystem.component.SettingsItem
import app.yuki.core.designsystem.component.SettingsItemContent
import app.yuki.core.designsystem.component.StatusChip
import app.yuki.core.designsystem.component.YukiBadge
import app.yuki.core.designsystem.component.YukiIcons

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
                trailing = { PermissionChip(row) },
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
private fun PermissionChip(row: PermissionRow) {
    StatusChip(label = row.chipLabel, tone = row.chipTone)
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
