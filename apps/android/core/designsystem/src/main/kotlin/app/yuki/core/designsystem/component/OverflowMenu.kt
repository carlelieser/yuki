package app.yuki.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag

const val OVERFLOW_MENU_TAG = "overflowMenu"
const val OVERFLOW_DESCRIPTION = "More options"

data class OverflowAction(
    val label: String,
    val onClick: () -> Unit,
    val icon: ImageVector? = null,
    val isEnabled: Boolean = true,
)

@Composable
fun OverflowMenu(
    actions: List<OverflowAction>,
    modifier: Modifier = Modifier,
) {
    if (actions.isEmpty()) return

    var isExpanded by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        IconButton(
            onClick = { isExpanded = true },
            modifier = Modifier.testTag(OVERFLOW_MENU_TAG),
        ) {
            Icon(
                imageVector = YukiIcons.MoreVert,
                contentDescription = OVERFLOW_DESCRIPTION,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OverflowActionMenu(
            actions = actions,
            isExpanded = isExpanded,
            onDismiss = { isExpanded = false },
        )
    }
}

@Composable
private fun OverflowActionMenu(
    actions: List<OverflowAction>,
    isExpanded: Boolean,
    onDismiss: () -> Unit,
) {
    DropdownMenu(expanded = isExpanded, onDismissRequest = onDismiss) {
        actions.forEach { action ->
            DropdownMenuItem(
                text = { Text(text = action.label) },
                onClick = {
                    onDismiss()
                    action.onClick()
                },
                enabled = action.isEnabled,
                leadingIcon = action.icon?.let { icon -> { OverflowActionIcon(icon) } },
            )
        }
    }
}

@Composable
private fun OverflowActionIcon(icon: ImageVector) {
    Icon(imageVector = icon, contentDescription = null)
}
