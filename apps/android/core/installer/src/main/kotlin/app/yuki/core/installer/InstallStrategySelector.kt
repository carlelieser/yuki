package app.yuki.core.installer

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstallStrategySelector @Inject constructor(
    internal val identityReader: ApkIdentityReader,
    private val fallback: InstallStrategy,
    private val privileged: PrivilegedInstaller?,
) {
    suspend fun select(): InstallStrategy {
        val available = privileged ?: return fallback
        return if (available.isReady()) available.strategy() else fallback
    }
}
