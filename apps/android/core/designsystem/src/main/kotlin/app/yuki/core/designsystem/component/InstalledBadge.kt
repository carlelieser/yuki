package app.yuki.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import app.yuki.core.designsystem.R
import app.yuki.core.designsystem.theme.YukiSize

const val INSTALLED_BADGE_TAG = "installedBadge"

private const val GLYPH_FRACTION = 0.7f

@Composable
fun InstalledBadge(isInstalled: Boolean, modifier: Modifier = Modifier) {
    if (!isInstalled) return

    val description = stringResource(R.string.designsystem_installed)

    Box(
        modifier = modifier
            .testTag(INSTALLED_BADGE_TAG)
            .size(YukiSize.InstalledBadge)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                width = YukiSize.InstalledBadgeBorder,
                color = MaterialTheme.colorScheme.surface,
                shape = CircleShape,
            )
            .semantics(mergeDescendants = true) { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = YukiIcons.Check,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(YukiSize.InstalledBadge * GLYPH_FRACTION),
        )
    }
}
