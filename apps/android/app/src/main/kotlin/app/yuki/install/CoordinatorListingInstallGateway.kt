package app.yuki.install

import android.content.Context
import android.content.Intent
import app.yuki.core.installer.InstallScheduler
import app.yuki.core.model.InstallState
import app.yuki.feature.listing.ListingInstallGateway
import app.yuki.feature.listing.ListingInstallRequest
import app.yuki.feature.listing.ListingInstallStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
internal class CoordinatorListingInstallGateway @Inject constructor(
    private val scheduler: InstallScheduler,
    private val installs: InstalledAppLookup,
    @ApplicationContext private val context: Context,
) : ListingInstallGateway {
    override suspend fun install(request: ListingInstallRequest) =
        scheduler.start(request.toInstallRequest(installs.baseUrl))

    override fun observe(githubRepoId: Long): Flow<ListingInstallStatus> =
        scheduler.observe(githubRepoId).map { progress ->
            progress?.let { ListingInstallStatus(it.state, it.versionTag) }
                ?: ListingInstallStatus(installedStateOf(githubRepoId), versionTag = null)
        }

    override suspend fun cancel(githubRepoId: Long) = scheduler.cancel(githubRepoId)

    override suspend fun open(githubRepoId: Long) {
        val packageName = installs.packageNameOf(githubRepoId) ?: return
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return

        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
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
