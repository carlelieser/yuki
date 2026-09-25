package app.yuki.feature.listing

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.component.OverflowMenu
import app.yuki.core.designsystem.component.YukiAnimatedState
import app.yuki.core.designsystem.component.YukiDetailScreen
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.component.rememberLinkOpener
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ScreenshotSelection
import app.yuki.core.model.UiState

const val LISTING_LOADING_TAG = "listingLoading"

data class ListingNavigation(
    val onBackClick: () -> Unit,
    val onScreenshotSelected: (ScreenshotSelection) -> Unit,
    val onAuthorSelected: (String) -> Unit = {},
    val onListingSelected: (String) -> Unit = {},
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
    val isConfirmingUninstall by viewModel.isConfirmingUninstall.collectAsStateWithLifecycle()
    val hasUninstallFailed by viewModel.hasUninstallFailed.collectAsStateWithLifecycle()
    val authorListings by viewModel.authorListings.collectAsStateWithLifecycle()
    val installs by viewModel.installs.collectAsStateWithLifecycle()
    val actions = listingActions(listing = listing, viewModel = viewModel)


    ListingScreen(
        state = ListingScreenState(
            listing = listing,
            installStatus = installStatus,
            isConfirmingUninstall = isConfirmingUninstall,
            hasUninstallFailed = hasUninstallFailed,
            actions = actions,
            authored = AuthoredListings(listings = authorListings, installs = installs),
        ),
        callbacks = rememberListingCallbacks(viewModel, navigation),
        onBackClick = navigation.onBackClick,
        modifier = modifier,
    )
}

@Composable
private fun rememberListingCallbacks(
    viewModel: ListingViewModel,
    navigation: ListingNavigation,
): ListingScreenCallbacks {
    val opener = rememberLinkOpener()

    return ListingScreenCallbacks(
        callbacks = ListingCallbacks(
            onInstallAction = VersionInstallHandler(viewModel::onInstallAction),
            onOpenLink = opener,
            onScreenshotSelected = navigation.onScreenshotSelected,
            onVersionInstallAction = VersionInstallHandler(viewModel::onVersionInstallAction),
            onAuthorSelected = navigation.onAuthorSelected,
            onListingSelected = { listing -> navigation.onListingSelected(listing.slug) },
        ),
        onRetry = viewModel::refresh,
        onUninstallConfirmed = viewModel::onUninstallConfirmed,
        onUninstallDismissed = viewModel::onUninstallDismissed,
    )
}

data class ListingScreenCallbacks(
    val callbacks: ListingCallbacks,
    val onRetry: () -> Unit,
    val onUninstallConfirmed: () -> Unit = {},
    val onUninstallDismissed: () -> Unit = {},
)

data class ListingScreenState(
    val listing: UiState<ListingUiModel>,
    val installStatus: ListingInstallStatus?,
    val isConfirmingUninstall: Boolean = false,
    val hasUninstallFailed: Boolean = false,
    val actions: List<ListingAction> = emptyList(),
    val authored: AuthoredListings = AuthoredListings(),
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
        trailing = { OverflowMenu(actions = state.actions) },
    ) {
        YukiAnimatedState(state = state.listing, modifier = Modifier.fillMaxSize()) { listing ->
            when (listing) {
                UiState.Loading -> ListingLoading()
                is UiState.Success -> {
                    ListingDetailBody(
                        model = listing.data,
                        status = ListingUninstallStatus(
                            install = state.installStatus,
                            hasUninstallFailed = state.hasUninstallFailed,
                        ),
                        callbacks = callbacks.callbacks,
                        authored = state.authored,
                    )

                    if (state.isConfirmingUninstall) {
                        ListingUninstallDialog(
                            prompt = ListingUninstallPrompt(
                                title = listing.data.detail.summary.title,
                                onConfirm = callbacks.onUninstallConfirmed,
                                onDismiss = callbacks.onUninstallDismissed,
                            ),
                        )
                    }
                }
                is UiState.Failure -> Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    FailureState(
                        reason = listing.reason,
                        missingMessage = stringResource(R.string.listing_missing),
                        onRetry = callbacks.onRetry,
                        modifier = Modifier.padding(YukiSpacing.Large),
                    )
                }
            }
        }
    }
}

