package app.yuki.feature.account

import app.yuki.core.auth.AuthSession
import app.yuki.core.model.FailureReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GithubConnectionViewModelTest {
    private val auth = FakeAuthRepository()
    private val accounts = FakeLinkedAccountsRepository()
    private val store = FakeSessionStore(AuthSession(token = "signed.token", account = ADA))
    private val browser = BrowserAuthFixture()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = GithubConnectionViewModel(
        GithubLinking(accounts, auth, browser.browserAuth, browser.urls),
        store,
    )

    @Test
    fun `an email-only account shows GitHub as not connected`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.isLoaded)
        assertNull(viewModel.state.value.github)
    }

    @Test
    fun `connecting opens the confirmation page with a ticket and state`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onConnect()
        advanceUntilIdle()

        assertEquals(
            "https://yuki.test/auth/mobile/github/link?ticket=ticket-1&state=state-1",
            viewModel.state.value.browserUrl,
        )
    }

    @Test
    fun `returning from GitHub shows the connected username`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onConnect()
        advanceUntilIdle()

        accounts.accounts = listOf(PASSWORD_ACCOUNT, GITHUB_ACCOUNT)
        browser.returnToApp("result=linked&state=state-1")
        advanceUntilIdle()

        assertEquals("ada", viewModel.state.value.github?.username)
        assertTrue(viewModel.state.value.canDisconnect)
    }

    @Test
    fun `a GitHub account linked elsewhere is explained`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()
        viewModel.onConnect()
        advanceUntilIdle()

        browser.returnToApp("error=account_already_linked_to_different_user&state=state-1")
        advanceUntilIdle()

        assertEquals(AccountMessage.GithubLinkedElsewhere, viewModel.state.value.message)
    }

    @Test
    fun `disconnecting removes GitHub`() = runTest {
        accounts.accounts = listOf(PASSWORD_ACCOUNT, GITHUB_ACCOUNT)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onDisconnect()
        advanceUntilIdle()

        assertEquals(listOf("acc-2"), accounts.unlinked)
        assertNull(viewModel.state.value.github)
    }

    @Test
    fun `the only sign-in method cannot be disconnected`() = runTest {
        accounts.accounts = listOf(GITHUB_ACCOUNT)
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onDisconnect()
        advanceUntilIdle()

        assertTrue(accounts.unlinked.isEmpty())
        assertEquals(AccountMessage.GithubLastSignInMethod, viewModel.state.value.message)
    }

    @Test
    fun `an old session is asked to sign in again before disconnecting`() = runTest {
        accounts.accounts = listOf(PASSWORD_ACCOUNT, GITHUB_ACCOUNT)
        accounts.unlinkResult = Result.failure(TypedFailure(FailureReason.ReauthenticationRequired))
        val viewModel = viewModel()
        advanceUntilIdle()

        viewModel.onDisconnect()
        advanceUntilIdle()

        assertEquals(AccountMessage.GithubReauthenticate, viewModel.state.value.message)
        assertFalse(viewModel.state.value.isBusy)
    }
}
