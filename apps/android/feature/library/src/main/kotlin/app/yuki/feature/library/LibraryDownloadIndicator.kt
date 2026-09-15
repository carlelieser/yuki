package app.yuki.feature.library

import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.model.DownloadSize

const val LIBRARY_DOWNLOAD_PROGRESS_TAG = "libraryDownloadProgress"

@Composable
internal fun LibraryDownloadIndicator(
    size: DownloadSize,
    modifier: Modifier = Modifier,
    description: String = describeDownload(size),
) {
    val fraction = size.fraction
    val indicatorModifier = modifier
        .size(YukiSize.IconSmall)
        .testTag(LIBRARY_DOWNLOAD_PROGRESS_TAG)
        .semantics { contentDescription = description }

    if (fraction == null) {
        CircularProgressIndicator(modifier = indicatorModifier)
        return
    }

    CircularProgressIndicator(progress = { fraction }, modifier = indicatorModifier)
}

internal fun describeDownload(size: DownloadSize): String {
    val fraction = size.fraction ?: return "Downloading"

    return "Downloading, ${(fraction * 100).toInt()} percent, ${size.label}"
}
