package app.yuki.feature.listing

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.yuki.core.designsystem.component.YukiTextButton

const val LISTING_UNINSTALL_DIALOG_TAG = "listingUninstallDialog"

data class ListingUninstallPrompt(
    val title: String,
    val onConfirm: () -> Unit,
    val onDismiss: () -> Unit,
)

@Composable
internal fun ListingUninstallDialog(prompt: ListingUninstallPrompt) {
    AlertDialog(
        modifier = Modifier.testTag(LISTING_UNINSTALL_DIALOG_TAG),
        onDismissRequest = prompt.onDismiss,
        title = { Text(text = UNINSTALL_TITLE) },
        text = { Text(text = uninstallMessage(prompt.title)) },
        confirmButton = {
            YukiTextButton(label = UNINSTALL_CONFIRM, onClick = prompt.onConfirm)
        },
        dismissButton = {
            YukiTextButton(label = UNINSTALL_CANCEL, onClick = prompt.onDismiss)
        },
    )
}

private fun uninstallMessage(title: String): String =
    "$title will be removed from this device."

internal const val UNINSTALL_TITLE = "Uninstall this app?"
internal const val UNINSTALL_CONFIRM = "Uninstall"
internal const val UNINSTALL_CANCEL = "Cancel"
