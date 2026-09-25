package app.yuki.install

import app.yuki.core.installer.InstallProgress
import app.yuki.core.installer.InstallTarget
import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstallState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShownOnButtonTest {
    @Test
    fun `a failed install is not shown on the listing button`() {
        assertNull(progressIn(InstallState.Failed(InstallFailure.Aborted)).shownOnButton())
    }

    @Test
    fun `an install waiting for the user is shown on the listing button`() {
        val pending = progressIn(InstallState.PendingUserAction)

        assertEquals(pending, pending.shownOnButton())
    }
}

private fun progressIn(state: InstallState) = InstallProgress(
    target = InstallTarget(githubRepoId = 1L, slug = "termux", title = "Termux", iconUrl = null),
    versionTag = "v1",
    state = state,
)
