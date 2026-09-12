package app.yuki.feature.listing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.component.InstallActionHandler
import app.yuki.core.designsystem.component.YukiDetailScreen
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.UiState

const val LISTING_LOADING_TAG = "listingLoading"

data class ListingNavigation(
    val onBackClick: () -> Unit,
    val onScreenshotSelected: (Int) -> Unit,
)

@Composable
fun ListingRoute(
    slug: String,
    navigation: ListingNavigation,
    modifier: Modifier = Modifier,
) {
    val viewModel: ListingViewModel = hiltViewModel(key = slug)
    val listing by viewModel.listing.collectAsStateWithLifecycle()
    val installStatus by viewModel.installStatus.collectAsStateWithLifecycle()

    ListingScreen(
        state = ListingScreenState(listing = listing, installStatus = installStatus),
        callbacks = rememberListingCallbacks(viewModel, navigation.onScreenshotSelected),
        onBackClick = navigation.onBackClick,
        modifier = modifier,
    )
}

@Composable
private fun rememberListingCallbacks(
    viewModel: ListingViewModel,
    onScreenshotSelected: (Int) -> Unit,
): ListingScreenCallbacks {
    val opener = rememberLinkOpener()

    return ListingScreenCallbacks(
        callbacks = ListingCallbacks(
            onInstallAction = InstallActionHandler(viewModel::onInstallAction),
            onOpenLink = opener,
            onScreenshotSelected = onScreenshotSelected,
            onVersionInstallAction = VersionInstallHandler(viewModel::onVersionInstallAction),
        ),
        onRetry = viewModel::refresh,
    )
}

data class ListingScreenCallbacks(
    val callbacks: ListingCallbacks,
    val onRetry: () -> Unit,
)

data class ListingScreenState(
    val listing: UiState<ListingUiModel>,
    val installStatus: ListingInstallStatus,
)

@Composable
private fun ListingLoading() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag(LISTING_LOADING_TAG),
        contentAlignment = Alignment.Center,
    ) {
        YukiLoadingIndicator()
    }
}

@Composable
internal fun ListingScreen(
    state: ListingScreenState,
    callbacks: ListingScreenCallbacks,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    YukiDetailScreen(
        title = "",
        onBackClick = onBackClick,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (val listing = state.listing) {
                UiState.Loading -> ListingLoading()
                is UiState.Success -> ListingDetailBody(
                    model = listing.data,
                    status = state.installStatus,
                    callbacks = callbacks.callbacks,
                )
                is UiState.Failure -> FailureState(
                    reason = listing.reason,
                    missingMessage = LISTING_MISSING_MESSAGE,
                    onRetry = callbacks.onRetry,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(YukiSpacing.Large),
                )
            }
        }
    }
}

internal const val LISTING_MISSING_MESSAGE = "This app is no longer available."
