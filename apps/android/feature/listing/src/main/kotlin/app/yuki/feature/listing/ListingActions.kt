package app.yuki.feature.listing

import androidx.compose.runtime.Composable
import app.yuki.core.designsystem.component.OverflowAction
import app.yuki.core.designsystem.component.YukiIcons
import app.yuki.core.model.UiState

typealias ListingAction = OverflowAction

const val SHARE_LABEL = "Share"

@Composable
internal fun listingActions(
    listing: UiState<ListingUiModel>,
    viewModel: ListingViewModel,
): List<ListingAction> {
    val sharer = rememberListingSharer()

    return buildList {
        if (listing is UiState.Success) {
            add(
                ListingAction(
                    label = SHARE_LABEL,
                    icon = YukiIcons.Share,
                    onClick = { sharer.share(viewModel.shareUrl) },
                ),
            )
        }
    }
}
