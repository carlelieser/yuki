package app.yuki.install

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import app.yuki.core.database.InstallStore
import app.yuki.core.installer.InstallProgress
import app.yuki.core.installer.InstallScheduler
import app.yuki.core.installer.Uninstaller
import app.yuki.core.installer.UninstallOutcome
import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstallState
import app.yuki.feature.listing.ListingInstallGateway
import app.yuki.feature.listing.ListingInstallRequest
import app.yuki.feature.listing.ListingInstallStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

@Singleton
internal class CoordinatorListingInstallGateway @Inject constructor(
    private val scheduler: InstallScheduler,
    private val installs: InstalledAppLookup,
    private val dependencies: UninstallDependencies,
    @ApplicationContext private val context: Context,
) : ListingInstallGateway {
    private val refreshes = MutableStateFlow(0)

    override suspend fun install(request: ListingInstallRequest) =
        scheduler.start(request.toInstallRequest(installs.baseUrl))

    override fun observe(githubRepoId: Long): Flow<ListingInstallStatus> =
        combine(scheduler.observe(githubRepoId), refreshes) { progress, _ -> progress }
            .map { progress -> statusFor(githubRepoId, progress) }

    private suspend fun statusFor(
        githubRepoId: Long,
        progress: InstallProgress?,
    ): ListingInstallStatus {
        if (progress != null) return ListingInstallStatus(progress.state, progress.versionTag)

        return ListingInstallStatus(installedStateOf(githubRepoId), versionTag = null)
    }

    override suspend fun cancel(githubRepoId: Long) = scheduler.cancel(githubRepoId)

    override suspend fun open(githubRepoId: Long) {
        val packageName = installs.packageNameOf(githubRepoId) ?: return
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return

        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override suspend fun isSilentUninstall(): Boolean = dependencies.uninstaller.isSilent()

    override fun refresh() {
        refreshes.value += 1
    }

    override suspend fun uninstall(githubRepoId: Long) {
        val packageName = requireNotNull(installs.packageNameOf(githubRepoId)) {
            "Cannot uninstall githubRepoId=$githubRepoId: no recorded package name"
        }

        when (val outcome = dependencies.uninstaller.uninstall(packageName)) {
            is UninstallOutcome.Removed -> forget(githubRepoId)
            is UninstallOutcome.NeedsSystemPrompt -> promptSystemUninstall(packageName)
            is UninstallOutcome.Failed -> throw UninstallException(packageName, outcome.reason)
        }
    }

    private suspend fun forget(githubRepoId: Long) {
        dependencies.store.forget(githubRepoId)
        refresh()
    }

    private fun promptSystemUninstall(packageName: String) {
        val intent = Intent(Intent.ACTION_DELETE, "package:$packageName".toUri())
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(intent)
    }

    private suspend fun installedStateOf(githubRepoId: Long): InstallState {
        val installed = installs.find(githubRepoId) ?: return InstallState.NotInstalled

        return if (installs.isPresent(installed.packageName)) {
            InstallState.Installed(installed.versionTag)
        } else {
            InstallState.NotInstalled
        }
    }
}

internal class UninstallException(packageName: String, reason: InstallFailure) :
    Exception("Failed to uninstall $packageName: $reason")

@Singleton
internal class UninstallDependencies @Inject constructor(
    val uninstaller: Uninstaller,
    val store: InstallStore,
)
