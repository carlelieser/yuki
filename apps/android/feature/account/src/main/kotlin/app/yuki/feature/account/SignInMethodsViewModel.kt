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
private const val PASSWORD = "credential"

data class SignInMethodsState(
    val isLoaded: Boolean = false,
    val github: LinkedAccount? = null,
    val hasPassword: Boolean = false,
    val canDisconnect: Boolean = false,
    val isBusy: Boolean = false,
    val browserUrl: String? = null,
    val message: AccountMessage? = null,
)

internal class SignInMethodsServices @Inject constructor(
    val accounts: LinkedAccountsRepository,
    val auth: AuthRepository,
    val browserAuth: BrowserAuth,
    val urls: GithubBrowserUrls,
)

@HiltViewModel
class SignInMethodsViewModel @Inject internal constructor(
    private val services: SignInMethodsServices,
    private val store: SessionStore,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SignInMethodsState())

    val state: StateFlow<SignInMethodsState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            store.session.map { session -> session?.account?.id }.distinctUntilChanged()
                .collect { userId -> if (userId != null) reload() }
        }
        viewModelScope.launch {
            services.browserAuth.outcomes(BrowserAuthPurpose.Link).collect(::onLinkResult)
        }
    }

    fun onConnect() {
        if (mutableState.value.isBusy) return
        mutableState.update { state -> state.copy(isBusy = true, message = null) }

        viewModelScope.launch {
            val ticket = services.auth.createTicket().getOrElse { error ->
                settle(githubFailureMessage(error.failureReason()))
                return@launch
            }
            val url = services.urls.link(ticket, services.browserAuth.begin(BrowserAuthPurpose.Link))
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
        if (!current.canDisconnect) return settle(AccountMessage.GithubLastSignInMethod)

        mutableState.update { state -> state.copy(isBusy = true, message = null) }

        viewModelScope.launch {
            services.accounts.unlink(github.id)
                .onSuccess { reload() }
                .onFailure { error -> settle(githubFailureMessage(error.failureReason())) }
        }
    }

    fun onSetPassword() {
        if (mutableState.value.isBusy) return
        mutableState.update { state -> state.copy(isBusy = true, message = null) }

        viewModelScope.launch {
            val email = store.read()?.account?.email ?: return@launch settle(AccountMessage.SessionExpired)
            services.auth.requestPasswordSetup(email)
                .onSuccess { settle(AccountMessage.PasswordEmailSent) }
                .onFailure { error -> settle(githubFailureMessage(error.failureReason())) }
        }
    }

    fun refresh() {
        viewModelScope.launch { if (store.read() != null) reload() }
    }

    fun onMessageShown() {
        mutableState.update { state -> state.copy(message = null) }
    }

    private fun onLinkResult(result: BrowserAuthResult) {
        when (result) {
            is BrowserAuthResult.Failed -> settle(githubErrorMessage(result.code))
            else -> refresh()
        }
    }

    private suspend fun reload() {
        services.accounts.list()
            .onSuccess { accounts -> mutableState.update { state -> state.showing(accounts) } }
            .onFailure { mutableState.update { state -> state.copy(isBusy = false) } }
    }

    private fun settle(message: AccountMessage) {
        mutableState.update { state -> state.copy(isBusy = false, message = message) }
    }
}

private fun SignInMethodsState.showing(accounts: List<LinkedAccount>) = copy(
    isLoaded = true,
    github = accounts.firstOrNull { account -> account.providerId == GITHUB },
    hasPassword = accounts.any { account -> account.providerId == PASSWORD },
    canDisconnect = accounts.size > 1,
    isBusy = false,
)
