package app.yuki.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import app.yuki.core.designsystem.theme.YukiShape
import app.yuki.core.designsystem.theme.YukiSize
import coil3.compose.AsyncImage

@Composable
fun AppIcon(
    iconUrl: String?,
    modifier: Modifier = Modifier,
    size: Dp = YukiSize.IconMedium,
) {
    val shape = if (size >= YukiSize.IconLarge) YukiShape.IconLarge else YukiShape.Icon
    val placeholder = Modifier
        .size(size)
        .clip(shape)
        .background(MaterialTheme.colorScheme.surfaceContainerHighest)

    Box(modifier = modifier.then(placeholder)) {
        if (iconUrl == null) return@Box

        AsyncImage(
            model = iconUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(size)
                .clearAndSetSemantics { },
        )
    }
}
