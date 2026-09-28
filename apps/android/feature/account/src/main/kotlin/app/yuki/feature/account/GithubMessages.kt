package app.yuki.feature.account

import app.yuki.core.model.FailureReason

private val githubErrors: Map<String, AccountMessage> = mapOf(
    "access_denied" to AccountMessage.GithubCancelled,
    "account_not_linked" to AccountMessage.GithubAccountExists,
    "github_email_unverified" to AccountMessage.GithubEmailUnverified,
    "unable_to_link_account" to AccountMessage.GithubEmailUnverified,
    "email_not_verified" to AccountMessage.GithubEmailUnverified,
    "account_already_linked_to_different_user" to AccountMessage.GithubLinkedElsewhere,
    "link_expired" to AccountMessage.GithubLinkExpired,
)

internal fun githubErrorMessage(code: String): AccountMessage =
    githubErrors[code] ?: AccountMessage.GithubFailed

internal fun githubFailureMessage(reason: FailureReason): AccountMessage = when (reason) {
    FailureReason.Offline -> AccountMessage.Offline
    FailureReason.Unauthorized -> AccountMessage.SessionExpired
    FailureReason.LastSignInMethod -> AccountMessage.GithubLastSignInMethod
    FailureReason.ReauthenticationRequired -> AccountMessage.GithubReauthenticate
    else -> AccountMessage.GithubFailed
}
