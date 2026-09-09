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
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.InstallState
import app.yuki.core.model.UiState

const val LISTING_LOADING_TAG = "listingLoading"

@Composable
fun ListingRoute(
    slug: String,
    modifier: Modifier = Modifier,
) {
    val viewModel: ListingViewModel = hiltViewModel(key = slug)
    val listing by viewModel.listing.collectAsStateWithLifecycle()
    val installState by viewModel.installState.collectAsStateWithLifecycle()

    ListingScreen(
        state = ListingScreenState(listing = listing, installState = installState),
        callbacks = rememberListingCallbacks(viewModel),
        modifier = modifier,
    )
}

@Composable
private fun rememberListingCallbacks(viewModel: ListingViewModel): ListingScreenCallbacks {
    val opener = rememberLinkOpener()

    return ListingScreenCallbacks(
        callbacks = ListingCallbacks(
            onInstallAction = InstallActionHandler(viewModel::onInstallAction),
            onOpenLink = opener,
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
    val installState: InstallState,
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
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (val listing = state.listing) {
            UiState.Loading -> ListingLoading()
            is UiState.Success -> ListingDetailBody(
                model = listing.data,
                installState = state.installState,
                callbacks = callbacks.callbacks,
            )
            is UiState.Failure -> FailureState(
                reason = listing.reason,
                onRetry = callbacks.onRetry,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(YukiSpacing.Large),
            )
        }
    }
}
