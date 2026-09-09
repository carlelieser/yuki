package app.yuki.feature.settings

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import app.yuki.core.designsystem.component.SectionHeader
import app.yuki.core.designsystem.component.SettingsItem
import app.yuki.core.designsystem.component.SettingsItemContent
import app.yuki.core.designsystem.component.StatusChip

internal fun LazyListScope.permissionsSection(
    rows: List<PermissionRow>,
    onPermissionClick: (PermissionRow) -> Unit,
) {
    item { SectionHeader(title = PERMISSIONS_SECTION_TITLE) }

    items(rows, key = { row -> row.permission.permission }) { row ->
        SettingsItem(
            content = SettingsItemContent(
                title = row.permission.label,
                supporting = supportingFor(row),
                trailing = { PermissionChip(row) },
            ),
            onClick = { onPermissionClick(row) },
        )
    }
}

@Composable
private fun PermissionChip(row: PermissionRow) {
    StatusChip(label = row.chipLabel, tone = row.chipTone)
}

internal fun supportingFor(row: PermissionRow): String =
    if (row.permission.isRequired) {
        "${row.permission.reason} $REASON_SEPARATOR $REQUIRED_LABEL"
    } else {
        "${row.permission.reason} $REASON_SEPARATOR $OPTIONAL_LABEL"
    }

internal const val PERMISSIONS_SECTION_TITLE = "Permissions"
internal const val REASON_SEPARATOR = "·"
internal const val REQUIRED_LABEL = "Required"
internal const val OPTIONAL_LABEL = "Optional"
