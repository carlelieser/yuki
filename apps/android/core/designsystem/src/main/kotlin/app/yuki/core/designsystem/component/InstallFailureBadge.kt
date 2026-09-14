package app.yuki.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import app.yuki.core.model.InstallFailure

const val INSTALL_FAILURE_BADGE_TAG = "installFailureBadge"

fun installFailureLabel(reason: InstallFailure): String = when (reason) {
    InstallFailure.DownloadFailed -> "Download failed"
    InstallFailure.DownloadUnreadable -> "Download incomplete"
    InstallFailure.Aborted -> "Install cancelled"
    InstallFailure.InsufficientStorage -> "Not enough space"
    InstallFailure.Incompatible -> "Not compatible"
    InstallFailure.PackageMismatch -> "Different app"
    InstallFailure.TimedOut -> "Install timed out"
    is InstallFailure.Rejected -> "Install failed"
}

@Composable
fun installFailureBadge(reason: InstallFailure): BadgeContent = BadgeContent(
    label = installFailureLabel(reason),
    icon = YukiIcons.Error,
    description = installFailureLabel(reason),
    tone = BadgeTone.Error,
)

@Composable
fun InstallFailureBadge(reason: InstallFailure, modifier: Modifier = Modifier) {
    YukiBadge(
        content = installFailureBadge(reason),
        modifier = modifier.testTag(INSTALL_FAILURE_BADGE_TAG),
    )
}
