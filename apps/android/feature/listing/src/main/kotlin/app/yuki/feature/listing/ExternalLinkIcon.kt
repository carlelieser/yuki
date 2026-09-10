package app.yuki.feature.listing

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.yuki.core.designsystem.component.YukiIcons

@Composable
internal fun ExternalLinkIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = YukiIcons.ArrowOutward,
        contentDescription = EXTERNAL_LINK_DESCRIPTION,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

internal const val EXTERNAL_LINK_DESCRIPTION = "Opens in your browser"
