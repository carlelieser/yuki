package app.yuki.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.yuki.core.auth.BrowserAuth
import app.yuki.core.auth.BrowserAuthPurpose
import app.yuki.core.auth.BrowserAuthResult
import app.yuki.core.auth.SessionStore
import app.yuki.core.model.LinkedAccount
import app.yuki.core.model.failureReason
import app.yuki.core.network.AuthRepository
import app.yuki.core.network.GithubBrowserUrls
import app.yuki.core.network.LinkedAccountsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val GITHUB = "github"

data class GithubConnectionState(
    val isLoaded: Boolean = false,
    val github: LinkedAccount? = null,
    val canDisconnect: Boolean = false,
    val isBusy: Boolean = false,
    val browserUrl: String? = null,
    val message: AccountMessage? = null,
)

internal class GithubLinking @Inject constructor(
    val accounts: LinkedAccountsRepository,
    val auth: AuthRepository,
    val browserAuth: BrowserAuth,
    val urls: GithubBrowserUrls,
)

@HiltViewModel
class GithubConnectionViewModel @Inject internal constructor(
    private val linking: GithubLinking,
    store: SessionStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(GithubConnectionState())

    val state: StateFlow<GithubConnectionState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            store.session.map { session -> session?.account?.id }.distinctUntilChanged()
                .collect { userId -> if (userId != null) refresh() }
        }
        viewModelScope.launch {
            linking.browserAuth.outcomes(BrowserAuthPurpose.Link).collect(::onLinkResult)
        }
    }

    fun onConnect() {
        if (mutableState.value.isBusy) return
        mutableState.update { state -> state.copy(isBusy = true, message = null) }

        viewModelScope.launch {
            val ticket = linking.auth.createTicket().getOrElse { error ->
                fail(githubFailureMessage(error.failureReason()))
                return@launch
            }
            val url = linking.urls.link(ticket, linking.browserAuth.begin(BrowserAuthPurpose.Link))
            mutableState.update { state -> state.copy(isBusy = false, browserUrl = url) }
        }
    }

    fun onBrowserLaunched() {
        mutableState.update { state -> state.copy(browserUrl = null) }
    }

    fun onDisconnect() {
        val current = mutableState.value
        val github = current.github ?: return
        if (current.isBusy) return
        if (!current.canDisconnect) return fail(AccountMessage.GithubLastSignInMethod)

        mutableState.update { state -> state.copy(isBusy = true, message = null) }

        viewModelScope.launch {
            linking.accounts.unlink(github.id)
                .onSuccess { refresh() }
                .onFailure { error -> fail(githubFailureMessage(error.failureReason())) }
        }
    }

    fun onMessageShown() {
        mutableState.update { state -> state.copy(message = null) }
    }

    private fun onLinkResult(result: BrowserAuthResult) {
        when (result) {
            is BrowserAuthResult.Failed -> fail(githubErrorMessage(result.code))
            else -> viewModelScope.launch { refresh() }
        }
    }

    private suspend fun refresh() {
        linking.accounts.list()
            .onSuccess { accounts -> mutableState.update { state -> state.showing(accounts) } }
            .onFailure { mutableState.update { state -> state.copy(isBusy = false) } }
    }

    private fun fail(message: AccountMessage) {
        mutableState.update { state -> state.copy(isBusy = false, message = message) }
    }
}

private fun GithubConnectionState.showing(accounts: List<LinkedAccount>) = copy(
    isLoaded = true,
    github = accounts.firstOrNull { account -> account.providerId == GITHUB },
    canDisconnect = accounts.size > 1,
    isBusy = false,
)
