package app.yuki.feature.account

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

sealed interface AccountMessage {
    data object InvalidCredentials : AccountMessage

    data object EmailUnverified : AccountMessage

    data object VerificationResent : AccountMessage

    data object VerificationResendFailed : AccountMessage

    data object Offline : AccountMessage

    data object Unavailable : AccountMessage

    data object EmailTaken : AccountMessage

    data object AvatarUploadFailed : AccountMessage

    data object SessionExpired : AccountMessage

    data object AvatarUnreadable : AccountMessage

    data class Explanation(val text: String) : AccountMessage
}

private val messageText: Map<AccountMessage, Int> = mapOf(
    AccountMessage.InvalidCredentials to R.string.account_message_invalid_credentials,
    AccountMessage.EmailUnverified to R.string.account_message_email_unverified,
    AccountMessage.VerificationResent to R.string.account_message_verification_resent,
    AccountMessage.VerificationResendFailed to R.string.account_message_verification_resend_failed,
    AccountMessage.Offline to R.string.account_message_offline,
    AccountMessage.Unavailable to R.string.account_message_unavailable,
    AccountMessage.EmailTaken to R.string.account_message_email_taken,
    AccountMessage.AvatarUploadFailed to R.string.account_message_avatar_upload_failed,
    AccountMessage.SessionExpired to R.string.account_message_session_expired,
    AccountMessage.AvatarUnreadable to R.string.account_message_avatar_unreadable,
)

@Composable
internal fun AccountMessage.text(): String = when (this) {
    is AccountMessage.Explanation -> text
    else -> stringResource(messageText.getValue(this))
}
