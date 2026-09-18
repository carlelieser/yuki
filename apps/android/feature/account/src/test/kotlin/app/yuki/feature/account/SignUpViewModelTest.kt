package app.yuki.feature.account

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import app.yuki.core.model.FailureReason
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SignUpViewModelTest {
    private val repository = FakeAuthRepository()

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = SignUpViewModel(repository)

    private fun SignUpViewModel.fillIn(
        name: String = "Ada Lovelace",
        email: String = "ada@yuki.test",
        password: String = "hunter2000",
    ) {
        onNameChange(name)
        onEmailChange(email)
        onPasswordChange(password)
    }

    @Test
    fun `an empty form reports every missing field`() = runTest {
        val viewModel = viewModel()

        viewModel.onSubmit()

        val state = viewModel.state.value
        assertEquals(NAME_REQUIRED, state.nameError)
        assertEquals(EMAIL_REQUIRED, state.emailError)
        assertEquals(PASSWORD_TOO_SHORT, state.passwordError)
    }

    @Test
    fun `a short password is rejected before reaching the server`() = runTest {
        val viewModel = viewModel()

        viewModel.fillIn(password = "short")
        viewModel.onSubmit()

        assertEquals(PASSWORD_TOO_SHORT, viewModel.state.value.passwordError)
    }

    @Test
    fun `a successful sign-up asks the user to check their email`() = runTest {
        val viewModel = viewModel()

        viewModel.state.test {
            awaitItem()
            viewModel.fillIn()
            viewModel.onSubmit()

            assertEquals("ada@yuki.test", awaitVerificationSent())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a taken email is named rather than reported as a generic failure`() = runTest {
        repository.signUpResult = Result.failure(TypedFailure(FailureReason.AccountExists))
        val viewModel = viewModel()

        viewModel.state.test {
            awaitItem()
            viewModel.fillIn()
            viewModel.onSubmit()

            assertEquals(SIGN_UP_TAKEN, awaitMessage())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `being offline is reported as a connection problem`() = runTest {
        repository.signUpResult = Result.failure(TypedFailure(FailureReason.Offline))
        val viewModel = viewModel()

        viewModel.state.test {
            awaitItem()
            viewModel.fillIn()
            viewModel.onSubmit()

            assertEquals(SIGN_UP_OFFLINE, awaitMessage())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `a failed sign-up does not claim verification was sent`() = runTest {
        repository.signUpResult = Result.failure(TypedFailure(FailureReason.Offline))
        val viewModel = viewModel()

        viewModel.state.test {
            awaitItem()
            viewModel.fillIn()
            viewModel.onSubmit()

            awaitMessage()
            cancelAndIgnoreRemainingEvents()
        }

        assertNull(viewModel.state.value.verificationSentTo)
    }
}

private suspend fun ReceiveTurbine<SignUpState>.awaitVerificationSent(): String? {
    while (true) {
        val sent = awaitItem().verificationSentTo
        if (sent != null) return sent
    }
}

private suspend fun ReceiveTurbine<SignUpState>.awaitMessage(): String? {
    while (true) {
        val state = awaitItem()
        if (!state.isSubmitting && state.message != null) return state.message
    }
}
