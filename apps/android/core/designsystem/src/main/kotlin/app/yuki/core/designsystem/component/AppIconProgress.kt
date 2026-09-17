package app.yuki.core.designsystem.component

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.model.InstallState

const val APP_ICON_PROGRESS_TAG = "appIconProgress"

fun installProgressFraction(state: InstallState): Float? = when (state) {
    is InstallState.Downloading -> state.size.fraction
    else -> null
}

fun isInstallInProgress(state: InstallState): Boolean = when (state) {
    is InstallState.Downloading -> true
    InstallState.Installing -> true
    else -> false
}

@Composable
fun AppIconProgress(
    iconUrl: String?,
    state: InstallState,
    modifier: Modifier = Modifier,
    description: String? = null,
) {
    if (!isInstallInProgress(state)) {
        AppIcon(iconUrl = iconUrl, size = YukiSize.IconMedium, modifier = modifier)
        return
    }

    val fraction = installProgressFraction(state)
    val ring = Modifier.size(YukiSize.IconMedium)

    Box(
        modifier = modifier
            .size(YukiSize.IconMedium)
            .testTag(APP_ICON_PROGRESS_TAG)
            .semantics(mergeDescendants = true) {
                if (description != null) contentDescription = description
            },
        contentAlignment = Alignment.Center,
    ) {
        AppIcon(iconUrl = iconUrl, size = YukiSize.IconMediumInProgress)

        ProgressIndicatorCrossfade(fraction = fraction, modifier = ring) { settled ->
            InstallRingIndicator(fraction = settled)
        }
    }
}

@Composable
private fun InstallRingIndicator(fraction: Float?) {
    if (fraction == null) {
        CircularWavyProgressIndicator()
        return
    }

    val animated = animatedProgress(fraction)

    CircularWavyProgressIndicator(progress = { animated })
}
