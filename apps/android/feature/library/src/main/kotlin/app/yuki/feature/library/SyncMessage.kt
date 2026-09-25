package app.yuki.feature.library

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource

sealed interface SyncMessage {
    data object Failed : SyncMessage

    data object UpToDate : SyncMessage

    data class Added(val uploaded: Int) : SyncMessage

    data class Partial(val uploaded: Int, val failed: Int) : SyncMessage
}

@Composable
internal fun SyncMessage.text(): String = when (this) {
    SyncMessage.Failed -> stringResource(R.string.library_sync_failed)
    SyncMessage.UpToDate -> stringResource(R.string.library_sync_up_to_date)
    is SyncMessage.Added -> pluralStringResource(R.plurals.library_sync_added, uploaded, uploaded)
    is SyncMessage.Partial -> pluralStringResource(R.plurals.library_sync_partial, failed, uploaded, failed)
}
