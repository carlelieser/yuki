package app.yuki.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

const val INSTALLED_BADGE_TAG = "installedBadge"
const val INSTALLED_BADGE_LABEL = "Installed"

@Composable
fun installedBadge(): BadgeContent = BadgeContent(
    label = INSTALLED_BADGE_LABEL,
    icon = YukiIcons.Check,
    description = INSTALLED_BADGE_LABEL,
)

@Composable
fun InstalledBadge(isInstalled: Boolean, modifier: Modifier = Modifier) {
    if (!isInstalled) return

    YukiBadge(content = installedBadge(), modifier = modifier.testTag(INSTALLED_BADGE_TAG))
}
