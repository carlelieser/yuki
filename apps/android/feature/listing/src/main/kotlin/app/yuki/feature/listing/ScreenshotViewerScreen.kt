package app.yuki.feature.listing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.component.LocalScreenshotFocus
import app.yuki.core.designsystem.component.YukiAnimatedState
import app.yuki.core.designsystem.component.YukiDetailScreen
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.component.describeScreenshot
import app.yuki.core.designsystem.component.sharedScreenshot
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.Screenshot
import app.yuki.core.model.UiState
import coil3.compose.AsyncImage

const val SCREENSHOT_VIEWER_TAG = "screenshotViewer"
const val SCREENSHOT_VIEWER_LOADING_TAG = "screenshotViewerLoading"

@Composable
fun ScreenshotViewerRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: ScreenshotViewerViewModel = hiltViewModel()
    val screenshots by viewModel.screenshots.collectAsStateWithLifecycle()

    ScreenshotViewerScreen(
        state = ScreenshotViewerState(
            screenshots = screenshots,
            startIndex = viewModel.startIndex,
        ),
        onBackClick = onBackClick,
        onRetry = viewModel::refresh,
        modifier = modifier,
    )
}

data class ScreenshotViewerState(
    val screenshots: UiState<List<Screenshot>>,
    val startIndex: Int,
)

@Composable
internal fun ScreenshotViewerScreen(
    state: ScreenshotViewerState,
    onBackClick: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YukiDetailScreen(title = "", onBackClick = onBackClick, modifier = modifier) {
        YukiAnimatedState(
            state = state.screenshots,
            modifier = Modifier.fillMaxSize(),
        ) { screenshots ->
            when (screenshots) {
                UiState.Loading -> ScreenshotViewerLoading()
                is UiState.Success -> ScreenshotPager(
                    screenshots = screenshots.data,
                    startIndex = state.startIndex,
                )
                is UiState.Failure -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    FailureState(
                        reason = screenshots.reason,
                        missingMessage = LISTING_MISSING_MESSAGE,
                        onRetry = onRetry,
                        modifier = Modifier.padding(YukiSpacing.Large),
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenshotViewerLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(SCREENSHOT_VIEWER_LOADING_TAG),
        contentAlignment = Alignment.Center,
    ) {
        YukiLoadingIndicator()
    }
}

@Composable
private fun ScreenshotPager(screenshots: List<Screenshot>, startIndex: Int) {
    if (screenshots.isEmpty()) return

    val pagerState = rememberPagerState(
        initialPage = startIndex.coerceIn(0, screenshots.lastIndex),
        pageCount = { screenshots.size },
    )
    val focus = LocalScreenshotFocus.current

    LaunchedEffect(pagerState.currentPage, screenshots) {
        val current = screenshots.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        focus.focus(current.url)
    }

    DisposableEffect(focus) {
        onDispose(focus::release)
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier
            .fillMaxSize()
            .testTag(SCREENSHOT_VIEWER_TAG),
    ) { page ->
        ScreenshotPage(
            screenshot = screenshots[page],
            position = ScreenshotPosition(
                index = page,
                total = screenshots.size,
                isCurrent = page == pagerState.currentPage,
            ),
        )
    }
}

private data class ScreenshotPosition(
    val index: Int,
    val total: Int,
    val isCurrent: Boolean,
)

@Composable
private fun ScreenshotPage(screenshot: Screenshot, position: ScreenshotPosition) {
    val shared = if (position.isCurrent) {
        Modifier.sharedScreenshot(url = screenshot.url)
    } else {
        Modifier
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        AsyncImage(
            model = screenshot.url,
            contentDescription = describeScreenshot(
                alt = screenshot.alt,
                index = position.index,
                total = position.total,
            ),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .then(shared),
        )
    }
}
