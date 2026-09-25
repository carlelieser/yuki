package app.yuki.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.yuki.core.designsystem.component.BadgeContent
import app.yuki.core.designsystem.component.SettingsGroup
import app.yuki.core.designsystem.component.SettingsRow
import app.yuki.core.designsystem.component.SettingsRowPosition
import app.yuki.core.designsystem.component.YukiBadge
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing

@Composable
internal fun PermissionsSection(
    rows: List<PermissionRow>,
    onPermissionClick: (PermissionRow) -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsGroup(
        modifier = modifier,
        label = stringResource(R.string.settings_permissions_title),
    ) {
        rows.forEachIndexed { index, row ->
            SettingsRow(
                position = SettingsRowPosition(index = index, count = rows.size),
                title = stringResource(row.permission.label),
                supporting = row.permission.reason?.let { reason -> stringResource(reason) },
                onClick = { onPermissionClick(row) },
                trailing = { PermissionTrailing(row) },
            )
        }
    }
}

@Composable
private fun PermissionTrailing(row: PermissionRow) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (row.permission.isRequired) {
            RequiredBadge()
        }

        PermissionStatusIcon(row)
    }
}

@Composable
private fun PermissionStatusIcon(row: PermissionRow) {
    Icon(
        imageVector = if (row.isGranted) YukiIcons.Check else YukiIcons.Warning,
        contentDescription = stringResource(row.statusLabel),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(YukiSize.IconSmall),
    )
}

@Composable
private fun RequiredBadge() {
    val label = stringResource(R.string.settings_permission_required)

    YukiBadge(
        content = BadgeContent(
            label = label,
            icon = YukiIcons.Asterisk,
            description = label,
        ),
    )
}

