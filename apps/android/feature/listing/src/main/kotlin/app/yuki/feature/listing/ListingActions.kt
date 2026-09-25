package app.yuki.feature.listing

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import app.yuki.core.designsystem.component.OverflowAction
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.model.UiState

typealias ListingAction = OverflowAction

@Composable
internal fun listingActions(
    listing: UiState<ListingUiModel>,
    viewModel: ListingViewModel,
): List<ListingAction> {
    val sharer = rememberListingSharer()
    val obtainium = rememberObtainiumOpener()
    val hasObtainium = rememberIsObtainiumInstalled()

    return buildList {
        if (listing is UiState.Success) {
            add(
                ListingAction(
                    label = stringResource(R.string.listing_share),
                    icon = YukiIcons.Share,
                    onClick = { sharer.share(viewModel.shareUrl) },
                ),
            )
        }
        if (listing is UiState.Success && hasObtainium) {
            val repositoryUrl = listing.data.detail.links.repositoryUrl
            add(
                ListingAction(
                    label = stringResource(R.string.listing_open_in_obtainium),
                    icon = YukiIcons.Obtainium,
                    onClick = { obtainium.open(repositoryUrl) },
                ),
            )
        }
    }
}
