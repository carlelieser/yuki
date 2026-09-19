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

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SignInViewModel(repository, store)

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
        assertEquals(EMAIL_REQUIRED, state.emailError)
        assertEquals(PASSWORD_REQUIRED, state.passwordError)
    }

    @Test
    fun `a malformed email is rejected before reaching the server`() = runTest {
        val viewModel = viewModel()

        viewModel.fillIn(email = "ada-at-yuki")
        viewModel.onSubmit()

        assertEquals(EMAIL_INVALID, viewModel.state.value.emailError)
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

        assertEquals(SIGN_IN_INVALID, messageAfterSubmit())
    }

    @Test
    fun `an unverified account is told to check its email instead`() = runTest {
        repository.signInResult = Result.failure(TypedFailure(FailureReason.EmailNotVerified))

        assertEquals(SIGN_IN_UNVERIFIED, messageAfterSubmit())
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
        assertEquals(SIGN_IN_RESENT, viewModel.state.value.message)
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

        assertEquals(SIGN_IN_RESEND_FAILED, viewModel.state.value.message)
    }

    @Test
    fun `being offline is reported as a connection problem`() = runTest {
        repository.signInResult = Result.failure(TypedFailure(FailureReason.Offline))

        assertEquals(SIGN_IN_OFFLINE, messageAfterSubmit())
    }

    @Test
    fun `a sign-in without a token does not claim to be signed in`() = runTest {
        repository.signInResult = Result.success(SignedIn(ADA, token = null))

        assertEquals(SIGN_IN_UNVERIFIED, messageAfterSubmit())
        assertNull(store.current)
    }

    @Test
    fun `typing again clears the error for that field`() = runTest {
        val viewModel = viewModel()

        viewModel.onSubmit()
        viewModel.onEmailChange("ada@yuki.test")

        assertNull(viewModel.state.value.emailError)
    }

    private suspend fun messageAfterSubmit(): String? {
        val viewModel = viewModel()
        var settled: String? = null

        viewModel.state.test {
            awaitItem()
            viewModel.fillIn()
            viewModel.onSubmit()

            settled = awaitSettled()
            cancelAndIgnoreRemainingEvents()
        }

        return settled
    }
}

private suspend fun ReceiveTurbine<SignInState>.awaitSignedIn(): Boolean {
    while (true) {
        if (awaitItem().isSignedIn) return true
    }
}

private suspend fun ReceiveTurbine<SignInState>.awaitSettled(): String? {
    while (true) {
        val state = awaitItem()
        if (!state.isSubmitting && state.message != null) return state.message
    }
}
