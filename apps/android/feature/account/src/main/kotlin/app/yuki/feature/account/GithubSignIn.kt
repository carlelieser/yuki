package app.yuki.feature.account

import app.yuki.core.auth.AuthSession
import app.yuki.core.auth.BrowserAuth
import app.yuki.core.auth.BrowserAuthPurpose
import app.yuki.core.auth.BrowserAuthResult
import app.yuki.core.auth.SessionStore
import app.yuki.core.model.failureReason
import app.yuki.core.network.AuthRepository
import app.yuki.core.network.GithubBrowserUrls
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

internal sealed interface GithubSignInOutcome {
    data object SignedIn : GithubSignInOutcome

    data class Failed(val message: AccountMessage) : GithubSignInOutcome
}

internal class GithubSignIn @Inject constructor(
    private val browserAuth: BrowserAuth,
    private val repository: AuthRepository,
    private val store: SessionStore,
    private val urls: GithubBrowserUrls,
) {
    suspend fun start(): String = urls.signIn(browserAuth.begin(BrowserAuthPurpose.SignIn))

    fun outcomes(): Flow<GithubSignInOutcome> =
        browserAuth.outcomes(BrowserAuthPurpose.SignIn).mapNotNull(::finish)

    private suspend fun finish(result: BrowserAuthResult): GithubSignInOutcome? = when (result) {
        is BrowserAuthResult.SignedIn -> exchange(result.ticket)
        is BrowserAuthResult.Failed -> GithubSignInOutcome.Failed(githubErrorMessage(result.code))
        is BrowserAuthResult.Linked -> null
    }

    private suspend fun exchange(ticket: String): GithubSignInOutcome {
        val signedIn = repository.exchangeTicket(ticket).getOrElse { error ->
            return GithubSignInOutcome.Failed(githubFailureMessage(error.failureReason()))
        }
        val token = signedIn.token ?: return GithubSignInOutcome.Failed(AccountMessage.GithubFailed)

        store.store(AuthSession(token = token, account = signedIn.account))
        return GithubSignInOutcome.SignedIn
    }
}
