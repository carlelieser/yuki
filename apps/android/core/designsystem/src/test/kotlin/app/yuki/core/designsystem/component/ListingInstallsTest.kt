package app.yuki.core.designsystem.component

import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstallState
import app.yuki.core.model.ListingSummary
import app.yuki.core.model.downloadSizeOf
import org.junit.Assert.assertEquals
import org.junit.Test

class ListingInstallsTest {
    @Test
    fun `an install in progress shows on the card`() {
        val downloading = InstallState.Downloading(downloadSizeOf(1L, 2L))

        assertEquals(downloading, stateShownFor(downloading))
    }

    @Test
    fun `a failed install shows the card as installable again`() {
        val failed = InstallState.Failed(InstallFailure.DownloadFailed(httpStatus = null))

        assertEquals(InstallState.NotInstalled, stateShownFor(failed))
    }

    @Test
    fun `a listing with no install shows as installable`() {
        assertEquals(InstallState.NotInstalled, stateShownFor(state = null))
    }
}

private fun stateShownFor(state: InstallState?): InstallState {
    val states = if (state == null) emptyMap() else mapOf(LISTING.githubRepoId to state)

    return ListingInstalls(installStates = states)
        .apply(LISTING, ProductListItemContent(title = LISTING.title, supporting = "", iconUrl = null))
        .installState
}

private val LISTING = ListingSummary(
    id = "id-1",
    githubRepoId = 1L,
    slug = "termux",
    title = "Termux",
    author = "author",
    description = null,
    iconUrl = null,
    bannerUrl = null,
    category = null,
    stars = 0,
)
