package app.yuki.feature.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.yuki.core.auth.SessionStore
import app.yuki.core.model.AuthAccount
import app.yuki.core.model.FailureReason
import app.yuki.core.model.failureReason
import app.yuki.core.network.AuthRepository
import app.yuki.core.network.AvatarUpload
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val STOP_TIMEOUT_MILLIS = 5_000L

internal const val AVATAR_UPLOAD_FAILED = "Could not save that picture. Try again."
internal const val AVATAR_UPLOAD_OFFLINE = "You're offline. Check your connection and try again."
internal const val AVATAR_UPLOAD_SIGNED_OUT = "Your session has expired. Sign in again."
internal const val AVATAR_UNREADABLE = "That image could not be read. Try a different one."

data class AccountState(
    val account: AuthAccount? = null,
    val isUploadingAvatar: Boolean = false,
    val message: String? = null,
) {
    val isSignedIn: Boolean get() = account != null
}

@HiltViewModel
class AccountViewModel @Inject internal constructor(
    private val repository: AuthRepository,
    private val store: SessionStore,
) : ViewModel() {
    private val transient = MutableStateFlow(TransientState())

    val state: StateFlow<AccountState> =
        combine(store.session.map { session -> session?.account }, transient.asStateFlow()) {
            account, pending ->
            AccountState(
                account = account,
                isUploadingAvatar = pending.isUploadingAvatar,
                message = pending.message,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = AccountState(),
        )

    internal fun onAvatarPicked(pick: AvatarPick) {
        when (pick) {
            is AvatarPick.Unreadable -> transient.value = TransientState(message = AVATAR_UNREADABLE)
            is AvatarPick.Ready -> uploadAvatar(pick.upload)
        }
    }

    private fun uploadAvatar(avatar: AvatarUpload) {
        if (transient.value.isUploadingAvatar) return

        transient.value = TransientState(isUploadingAvatar = true)

        viewModelScope.launch {
            repository.uploadAvatar(avatar)
                .onSuccess { account -> store.updateAccount(account) }
                .onFailure { error ->
                    transient.value = TransientState(message = avatarFailureMessage(error))
                }

            transient.update { pending -> pending.copy(isUploadingAvatar = false) }
        }
    }

    fun onMessageShown() {
        transient.update { pending -> pending.copy(message = null) }
    }

    fun onSignOut() {
        if (transient.value.isSigningOut) return

        transient.update { pending -> pending.copy(isSigningOut = true) }

        viewModelScope.launch {
            store.clear()
            repository.signOut()
        }
    }
}

private data class TransientState(
    val isUploadingAvatar: Boolean = false,
    val message: String? = null,
    val isSigningOut: Boolean = false,
)

internal fun avatarFailureMessage(error: Throwable): String = when (val reason = error.failureReason()) {
    is FailureReason.Rejected -> reason.explanation
    FailureReason.Offline -> AVATAR_UPLOAD_OFFLINE
    FailureReason.Unauthorized -> AVATAR_UPLOAD_SIGNED_OUT
    else -> AVATAR_UPLOAD_FAILED
}
