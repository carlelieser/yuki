package app.yuki.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import app.yuki.core.designsystem.theme.YukiRatio
import app.yuki.core.designsystem.theme.YukiShape
import app.yuki.core.designsystem.theme.YukiSize
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.Screenshot
import app.yuki.core.model.ScreenshotSelection
import coil3.compose.AsyncImage

fun describeScreenshot(alt: String?, index: Int, total: Int): String =
    alt ?: "Screenshot ${index + 1} of $total"

@Composable
fun ScreenshotCarousel(
    screenshots: List<Screenshot>,
    modifier: Modifier = Modifier,
    onSelect: ((ScreenshotSelection) -> Unit)? = null,
) {
    val urls = screenshots.map(Screenshot::url)
    val focus = LocalScreenshotFocus.current

    LaunchedEffect(urls) { focus.confine(urls) }

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = YukiSpacing.Large),
        horizontalArrangement = Arrangement.spacedBy(YukiSpacing.Medium),
    ) {
        itemsIndexed(items = screenshots, key = { _, item -> item.url }) { index, screenshot ->
            val itemModifier = Modifier
                .width(YukiSize.ScreenshotWidth)
                .aspectRatio(YukiRatio.Screenshot)
                .clip(YukiShape.Media)
                .sharedScreenshot(url = screenshot.url)

            AsyncImage(
                model = screenshot.url,
                contentDescription = describeScreenshot(
                    alt = screenshot.alt,
                    index = index,
                    total = screenshots.size,
                ),
                contentScale = ContentScale.Crop,
                modifier = if (onSelect == null) {
                    itemModifier
                } else {
                    itemModifier.clickable {
                        onSelect(ScreenshotSelection(index = index, urls = urls))
                    }
                },
            )
        }
    }
}
