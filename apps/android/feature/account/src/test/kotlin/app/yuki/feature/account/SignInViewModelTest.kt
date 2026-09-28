package app.yuki.feature.account

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import app.yuki.core.model.FailureReason
import app.yuki.core.network.SignedIn
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
class SignInViewModelTest {
    private val repository = FakeAuthRepository()
    private val store = FakeSessionStore()
    private val browser = BrowserAuthFixture()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SignInViewModel(repository, store, browser.signIn(repository, store))

    private fun SignInViewModel.fillIn(
        email: String = "ada@yuki.test",
        password: String = "hunter2000",
    ) {
        onEmailChange(email)
        onPasswordChange(password)
    }

    @Test
    fun `an empty form reports which fields are missing`() = runTest {
        val viewModel = viewModel()

        viewModel.onSubmit()

        val state = viewModel.state.value
        assertEquals(CredentialError.EmailRequired, state.emailError)
        assertEquals(CredentialError.PasswordRequired, state.passwordError)
    }

    @Test
    fun `a malformed email is rejected before reaching the server`() = runTest {
        val viewModel = viewModel()

        viewModel.fillIn(email = "ada-at-yuki")
        viewModel.onSubmit()

        assertEquals(CredentialError.EmailInvalid, viewModel.state.value.emailError)
    }

    @Test
    fun `a successful sign-in stores the session`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            assertFalse(awaitItem().isSignedIn)

            viewModel.fillIn()
            viewModel.onSubmit()

            assertTrue(awaitSignedIn())
            cancelAndIgnoreRemainingEvents()
        }

        assertEquals("signed.token", store.current?.token)
        assertEquals(ADA, store.current?.account)
    }

    @Test
    fun `a wrong password is reported as invalid credentials`() = runTest {
        repository.signInResult = Result.failure(TypedFailure(FailureReason.Unauthorized))

        assertEquals(AccountMessage.InvalidCredentials, messageAfterSubmit())
    }

    @Test
    fun `an unverified account is told to check its email instead`() = runTest {
        repository.signInResult = Result.failure(TypedFailure(FailureReason.EmailNotVerified))

        assertEquals(AccountMessage.EmailUnverified, messageAfterSubmit())
    }

    @Test
    fun `only an unverified account is offered a resend`() = runTest {
        repository.signInResult = Result.failure(TypedFailure(FailureReason.EmailNotVerified))
        val unverified = viewModel()

        unverified.fillIn()
        unverified.onSubmit()
        advanceUntilIdle()

        assertTrue(unverified.state.value.canResendVerification)

        repository.signInResult = Result.failure(TypedFailure(FailureReason.Unauthorized))
        val rejected = viewModel()

        rejected.fillIn()
        rejected.onSubmit()
        advanceUntilIdle()

        assertFalse(rejected.state.value.canResendVerification)
    }

    @Test
    fun `resending asks for a new link and confirms it was sent`() = runTest {
        repository.signInResult = Result.failure(TypedFailure(FailureReason.EmailNotVerified))
        val viewModel = viewModel()

        viewModel.fillIn(email = " ada@yuki.test ")
        viewModel.onSubmit()
        advanceUntilIdle()

        viewModel.onResendVerification()
        advanceUntilIdle()

        assertEquals(listOf("ada@yuki.test"), repository.verificationsSentTo)
        assertEquals(AccountMessage.VerificationResent, viewModel.state.value.message)
        assertFalse(viewModel.state.value.canResendVerification)
    }

    @Test
    fun `a resend that fails says so instead of claiming success`() = runTest {
        repository.signInResult = Result.failure(TypedFailure(FailureReason.EmailNotVerified))
        repository.verificationResult = Result.failure(TypedFailure(FailureReason.Offline))
        val viewModel = viewModel()

        viewModel.fillIn()
        viewModel.onSubmit()
        advanceUntilIdle()

        viewModel.onResendVerification()
        advanceUntilIdle()

        assertEquals(AccountMessage.VerificationResendFailed, viewModel.state.value.message)
    }

    @Test
    fun `being offline is reported as a connection problem`() = runTest {
        repository.signInResult = Result.failure(TypedFailure(FailureReason.Offline))

        assertEquals(AccountMessage.Offline, messageAfterSubmit())
    }

    @Test
    fun `a sign-in without a token does not claim to be signed in`() = runTest {
        repository.signInResult = Result.success(SignedIn(ADA, token = null))

        assertEquals(AccountMessage.EmailUnverified, messageAfterSubmit())
        assertNull(store.current)
    }

    @Test
    fun `typing again clears the error for that field`() = runTest {
        val viewModel = viewModel()

        viewModel.onSubmit()
        viewModel.onEmailChange("ada@yuki.test")

        assertNull(viewModel.state.value.emailError)
    }

    private suspend fun messageAfterSubmit(): AccountMessage? {
        val viewModel = viewModel()
        var settled: AccountMessage? = null

        viewModel.state.test {
            awaitItem()
            viewModel.fillIn()
            viewModel.onSubmit()

            settled = awaitSettled()
            cancelAndIgnoreRemainingEvents()
        }

        return settled
    }

    @Test
    fun `continuing with GitHub opens the browser with a fresh state`() = runTest {
        val viewModel = viewModel()

        viewModel.onGithubClick()
        advanceUntilIdle()

        assertEquals(
            "https://yuki.test/auth/mobile/github/start?state=state-1",
            viewModel.state.value.browserUrl,
        )
    }

    @Test
    fun `a double tap starts only one GitHub sign-in`() = runTest {
        val viewModel = viewModel()

        viewModel.onGithubClick()
        viewModel.onGithubClick()
        advanceUntilIdle()

        assertEquals(
            "https://yuki.test/auth/mobile/github/start?state=state-1",
            viewModel.state.value.browserUrl,
        )
    }

    @Test
    fun `returning from GitHub signs in with the ticket`() = runTest {
        val viewModel = viewModel()
        viewModel.onGithubClick()
        advanceUntilIdle()

        browser.returnToApp("flow=signin&token=ticket-9&state=state-1")
        advanceUntilIdle()

        assertEquals(listOf("ticket-9"), repository.exchangedTickets)
        assertEquals("github.token", store.current?.token)
        assertTrue(viewModel.state.value.isSignedIn)
    }

    @Test
    fun `a ticket for a sign-in this app did not start is never used`() = runTest {
        val viewModel = viewModel()
        advanceUntilIdle()

        browser.returnToApp("flow=signin&token=ticket-9&state=forged")
        advanceUntilIdle()

        assertTrue(repository.exchangedTickets.isEmpty())
        assertNull(store.current)
        assertFalse(viewModel.state.value.isSignedIn)
    }

    @Test
    fun `a sign-in that finishes after a newer attempt says it expired`() = runTest {
        val viewModel = viewModel()
        viewModel.onGithubClick()
        advanceUntilIdle()
        viewModel.onBrowserLaunched()
        viewModel.onGithubClick()
        advanceUntilIdle()

        browser.returnToApp("flow=signin&error=state_security_mismatch&state=state-1")
        advanceUntilIdle()

        assertEquals(AccountMessage.GithubExpired, viewModel.state.value.message)
    }

    @Test
    fun `a stale ticket says the sign-in expired instead of doing nothing`() = runTest {
        val viewModel = viewModel()
        viewModel.onGithubClick()
        advanceUntilIdle()
        viewModel.onBrowserLaunched()
        viewModel.onGithubClick()
        advanceUntilIdle()

        browser.returnToApp("flow=signin&token=ticket-9&state=state-1")
        advanceUntilIdle()

        assertTrue(repository.exchangedTickets.isEmpty())
        assertEquals(AccountMessage.GithubExpired, viewModel.state.value.message)
    }

    @Test
    fun `a link result is left for the settings screen`() = runTest {
        val viewModel = viewModel()
        viewModel.onGithubClick()
        advanceUntilIdle()

        browser.returnToApp("flow=link&result=linked&state=state-1")
        advanceUntilIdle()

        assertNull(viewModel.state.value.message)
        assertFalse(viewModel.state.value.isSignedIn)
    }

    @Test
    fun `an existing email account is told to connect GitHub after signing in`() = runTest {
        val viewModel = viewModel()
        viewModel.onGithubClick()
        advanceUntilIdle()

        browser.returnToApp("flow=signin&error=account_not_linked&state=state-1")
        advanceUntilIdle()

        assertEquals(AccountMessage.GithubAccountExists, viewModel.state.value.message)
        assertFalse(viewModel.state.value.isSignedIn)
    }

}

private suspend fun ReceiveTurbine<SignInState>.awaitSignedIn(): Boolean {
    while (true) {
        if (awaitItem().isSignedIn) return true
    }
}

private suspend fun ReceiveTurbine<SignInState>.awaitSettled(): AccountMessage? {
    while (true) {
        val state = awaitItem()
        if (!state.isSubmitting && state.message != null) return state.message
    }
}
