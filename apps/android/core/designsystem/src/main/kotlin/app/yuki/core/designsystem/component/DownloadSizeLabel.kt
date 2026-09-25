package app.yuki.core.designsystem.component

import android.text.format.Formatter
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import app.yuki.core.designsystem.R
import app.yuki.core.model.DownloadSize

@Composable
fun DownloadSize.label(): String {
    val context = LocalContext.current
    val downloaded = Formatter.formatShortFileSize(context, bytesDownloaded)
    val total = bytesTotal?.let { bytes -> Formatter.formatShortFileSize(context, bytes) }

    return stringResource(
        R.string.designsystem_download_size,
        downloaded,
        total ?: stringResource(R.string.designsystem_download_size_unknown),
    )
}
