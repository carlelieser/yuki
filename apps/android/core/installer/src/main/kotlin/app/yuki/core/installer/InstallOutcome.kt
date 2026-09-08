package app.yuki.core.installer

import app.yuki.core.model.InstallFailure

sealed interface InstallOutcome {
    data object Succeeded : InstallOutcome

    data object AwaitingUserAction : InstallOutcome

    data class Failed(val reason: InstallFailure) : InstallOutcome
}
