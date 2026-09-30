package app.yuki.feature.listing

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.yuki.core.designsystem.component.FailureState
import app.yuki.core.designsystem.component.YukiAnimatedState
import app.yuki.core.designsystem.component.YukiDetailScreen
import app.yuki.core.designsystem.component.YukiLoadingIndicator
import app.yuki.core.designsystem.theme.YukiSpacing
import app.yuki.core.model.ListingDetail
import app.yuki.core.model.UiState

const val VERSIONS_LIST_TAG = "versionsList"

@Composable
fun VersionsRoute(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VersionsViewModel = hiltViewModel(),
) {
    val listing by viewModel.listing.collectAsStateWithLifecycle()
    val installStatus by viewModel.installStatus.collectAsStateWithLifecycle()

    VersionsScreen(
        state = VersionsScreenState(listing = listing, installStatus = installStatus),
        callbacks = VersionsCallbacks(
            onVersionInstallAction = VersionInstallHandler(viewModel::onVersionInstallAction),
            onRetry = viewModel::refresh,
            onBackClick = onBackClick,
        ),
        modifier = modifier,
    )
}

data class VersionsScreenState(
    val listing: UiState<ListingDetail>,
    val installStatus: ListingInstallStatus?,
)

data class VersionsCallbacks(
    val onVersionInstallAction: VersionInstallHandler,
    val onRetry: () -> Unit,
    val onBackClick: () -> Unit,
)

@Composable
internal fun VersionsScreen(
    state: VersionsScreenState,
    callbacks: VersionsCallbacks,
    modifier: Modifier = Modifier,
) {
    YukiDetailScreen(
        title = stringResource(R.string.listing_section_versions),
        onBackClick = callbacks.onBackClick,
        modifier = modifier,
    ) {
        YukiAnimatedState(state = state.listing, modifier = Modifier.fillMaxSize()) { listing ->
            when (listing) {
                UiState.Loading -> CenteredBox { YukiLoadingIndicator() }
                is UiState.Success -> VersionList(
                    versions = listing.data,
                    status = state.installStatus,
                    onVersionInstallAction = callbacks.onVersionInstallAction,
                )
                is UiState.Failure -> CenteredBox {
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

@Composable
private fun VersionList(
    versions: ListingDetail,
    status: ListingInstallStatus?,
    onVersionInstallAction: VersionInstallHandler,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .testTag(VERSIONS_LIST_TAG),
        contentPadding = PaddingValues(vertical = YukiSpacing.Large),
        verticalArrangement = Arrangement.spacedBy(YukiSpacing.Large),
    ) {
        if (status == null) return@LazyColumn

        versionItems(
            versions = versions.versions,
            status = status,
            onVersionInstallAction = onVersionInstallAction,
        )
    }
}

@Composable
private fun CenteredBox(content: @Composable () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        content()
    }
}
