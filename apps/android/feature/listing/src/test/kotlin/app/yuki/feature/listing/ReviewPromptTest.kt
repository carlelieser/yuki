package app.yuki.feature.listing

import app.yuki.core.model.InstallState
import app.yuki.core.model.OwnReview
import org.junit.Assert.assertEquals
import org.junit.Test

class ReviewPromptTest {
    private val eligible = ReviewViewer(isSignedIn = true, own = OwnReview(review = null, canReview = true))

    @Test
    fun `a signed-out viewer is asked to sign in`() {
        val prompt = reviewPrompt(ReviewViewer(isSignedIn = false), InstallState.Installed("v1"))

        assertEquals(ReviewPrompt.SignIn, prompt)
    }

    @Test
    fun `an installed app can be reviewed`() {
        assertEquals(ReviewPrompt.Write, reviewPrompt(eligible, InstallState.Installed("v1")))
        assertEquals(ReviewPrompt.Write, reviewPrompt(eligible, InstallState.UpdateAvailable("v1", "v2")))
    }

    @Test
    fun `an app that is not on the device must be installed first`() {
        assertEquals(ReviewPrompt.InstallRequired, reviewPrompt(eligible, InstallState.NotInstalled))
        assertEquals(ReviewPrompt.InstallRequired, reviewPrompt(eligible, InstallState.Installing))
        assertEquals(ReviewPrompt.InstallRequired, reviewPrompt(eligible, null))
    }

    @Test
    fun `an existing review is offered for editing even after uninstalling`() {
        val own = review("mine")
        val viewer = ReviewViewer(isSignedIn = true, own = OwnReview(review = own, canReview = true))

        assertEquals(ReviewPrompt.Edit(own), reviewPrompt(viewer, InstallState.NotInstalled))
    }

    @Test
    fun `nothing is offered while the signed-in review is unknown`() {
        val viewer = ReviewViewer(isSignedIn = true, own = null)

        assertEquals(ReviewPrompt.Hidden, reviewPrompt(viewer, InstallState.Installed("v1")))
    }
}
