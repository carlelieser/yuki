package app.yuki.feature.listing

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val ArrowOutward: ImageVector = ImageVector.Builder(
    name = "ArrowOutward",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(6.4f, 18.65f)
        lineTo(5.35f, 17.6f)
        lineTo(16.2f, 6.75f)
        horizontalLineTo(7f)
        verticalLineTo(5.25f)
        horizontalLineTo(18.75f)
        verticalLineTo(17f)
        horizontalLineTo(17.25f)
        verticalLineTo(7.8f)
        close()
    }
}.build()

@Composable
internal fun ExternalLinkIcon(modifier: Modifier = Modifier) {
    Icon(
        imageVector = ArrowOutward,
        contentDescription = EXTERNAL_LINK_DESCRIPTION,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier,
    )
}

internal const val EXTERNAL_LINK_DESCRIPTION = "Opens in your browser"
