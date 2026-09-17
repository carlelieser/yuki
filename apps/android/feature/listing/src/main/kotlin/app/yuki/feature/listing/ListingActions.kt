package app.yuki.feature.listing

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import app.yuki.core.designsystem.component.OverflowAction

typealias ListingAction = OverflowAction

const val SHARE_LABEL = "Share"

@Composable
internal fun rememberListingActions(
    isShareable: Boolean,
    shareUrl: () -> String,
): List<ListingAction> {
    val sharer = rememberListingSharer()

    return remember(isShareable, sharer) {
        if (!isShareable) return@remember emptyList()

        listOf(ListingAction(label = SHARE_LABEL, onClick = { sharer.share(shareUrl()) }))
    }
}
