package app.yuki.core.installer

import app.yuki.core.model.InstallFailure

sealed interface UninstallOutcome {
    data object Removed : UninstallOutcome

    data object NeedsSystemPrompt : UninstallOutcome

    data class Failed(val reason: InstallFailure) : UninstallOutcome
}

class Uninstaller internal constructor(
    private val privileged: PrivilegedInstaller?,
) {
    suspend fun isSilent(): Boolean = privileged?.isReady() == true

    suspend fun uninstall(packageName: String): UninstallOutcome {
        val installer = privileged?.takeIf { it.isReady() } ?: return NEEDS_PROMPT

        return when (val outcome = installer.uninstall(packageName)) {
            is InstallOutcome.Succeeded -> UninstallOutcome.Removed
            is InstallOutcome.Failed -> UninstallOutcome.Failed(outcome.reason)
            is InstallOutcome.AwaitingUserAction -> NEEDS_PROMPT
        }
    }
}

private val NEEDS_PROMPT = UninstallOutcome.NeedsSystemPrompt
