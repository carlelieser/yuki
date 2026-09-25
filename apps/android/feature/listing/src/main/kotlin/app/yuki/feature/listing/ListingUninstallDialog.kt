package app.yuki.feature.listing

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
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
        title = { Text(text = stringResource(R.string.listing_uninstall_title)) },
        text = { Text(text = stringResource(R.string.listing_uninstall_message, prompt.title)) },
        confirmButton = {
            YukiTextButton(
                label = stringResource(R.string.listing_uninstall_confirm),
                onClick = prompt.onConfirm,
            )
        },
        dismissButton = {
            YukiTextButton(
                label = stringResource(R.string.listing_uninstall_cancel),
                onClick = prompt.onDismiss,
            )
        },
    )
}
