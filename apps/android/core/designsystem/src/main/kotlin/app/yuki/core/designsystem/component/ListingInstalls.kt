package app.yuki.core.designsystem.component

import app.yuki.core.model.InstallState
import app.yuki.core.model.ListingSummary

data class ListingInstalls(
    val installedIds: Set<Long> = emptySet(),
    val installStates: Map<Long, InstallState> = emptyMap(),
) {
    fun apply(
        listing: ListingSummary,
        content: ProductListItemContent,
    ): ProductListItemContent = content.copy(
        isInstalled = listing.githubRepoId in installedIds,
        installState = installStates[listing.githubRepoId] ?: InstallState.NotInstalled,
    )
}
