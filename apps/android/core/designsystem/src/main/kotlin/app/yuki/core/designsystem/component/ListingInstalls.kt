package app.yuki.core.designsystem.component

import app.yuki.core.model.InstallState
import app.yuki.core.model.ListingSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class ListingInstalls(
    val installedIds: Set<Long> = emptySet(),
    val installStates: Map<Long, InstallState> = emptyMap(),
) {
    fun apply(
        listing: ListingSummary,
        content: ProductListItemContent,
    ): ProductListItemContent = content.copy(
        isInstalled = listing.githubRepoId in installedIds,
        installState = stateOf(listing.githubRepoId),
    )

    private fun stateOf(githubRepoId: Long): InstallState {
        val state = installStates[githubRepoId]

        return if (state == null || state is InstallState.Failed) InstallState.NotInstalled else state
    }
}

fun observeListingInstalls(
    installedIds: Flow<Set<Long>>,
    activeStates: Flow<Map<Long, InstallState>>,
): Flow<ListingInstalls> = combine(installedIds, activeStates) { ids, states ->
    ListingInstalls(installedIds = ids, installStates = states)
}
