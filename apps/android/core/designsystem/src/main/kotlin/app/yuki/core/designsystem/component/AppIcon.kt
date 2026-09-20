package app.yuki.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import app.yuki.core.designsystem.theme.YukiShape
import app.yuki.core.designsystem.theme.YukiSize
import coil3.compose.AsyncImagePainter
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest

private const val FALLBACK_GLYPH_FRACTION = 0.5f

@Composable
private fun IconPlaceholder(
    size: Dp,
    hasGlyph: Boolean,
    modifier: Modifier = Modifier,
) {
    val shape = if (size >= YukiSize.IconLarge) YukiShape.IconLarge else YukiShape.Icon

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        contentAlignment = Alignment.Center,
    ) {
        if (!hasGlyph) return@Box

        Icon(
            imageVector = YukiIcons.DeployedCode,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(size * FALLBACK_GLYPH_FRACTION)
                .clearAndSetSemantics { },
        )
    }
}

@Composable
fun AppIcon(
    iconUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = YukiSize.IconMedium,
) {
    if (iconUrl == null) {
        IconPlaceholder(size = size, hasGlyph = true, modifier = modifier)
        return
    }

    val pixels = with(LocalDensity.current) { size.roundToPx() }
    val request = ImageRequest.Builder(LocalContext.current)
        .data(iconUrl)
        .size(pixels)
        .build()
    val painter = rememberAsyncImagePainter(model = request, contentScale = ContentScale.Crop)
    val state = painter.state.collectAsState().value

    if (state !is AsyncImagePainter.State.Success) {
        val hasGlyph = state is AsyncImagePainter.State.Error
        IconPlaceholder(size = size, hasGlyph = hasGlyph, modifier = modifier)
        return
    }

    val shape = if (size >= YukiSize.IconLarge) YukiShape.IconLarge else YukiShape.Icon

    Image(
        painter = painter,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .size(size)
            .clip(shape)
            .clearAndSetSemantics { },
    )
}
