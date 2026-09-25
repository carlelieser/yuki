package app.yuki.feature.listing

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import app.yuki.core.designsystem.component.YukiIcons

@Composable
internal fun ExternalLinkIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = YukiIcons.ArrowOutward,
        contentDescription = stringResource(R.string.listing_external_link),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

