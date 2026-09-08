package app.yuki.install

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import app.yuki.core.database.InstallStore
import app.yuki.core.installer.InstallCoordinator
import app.yuki.core.installer.InstallRequest
import app.yuki.core.installer.InstallSource
import app.yuki.core.installer.InstallTarget
import app.yuki.core.model.InstallState
import app.yuki.feature.listing.ListingInstallGateway
import app.yuki.feature.listing.ListingInstallRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

@Singleton
internal class CoordinatorListingInstallGateway @Inject constructor(
    private val coordinator: InstallCoordinator,
    private val store: InstallStore,
    @ApplicationContext private val context: Context,
) : ListingInstallGateway {
    override fun install(request: ListingInstallRequest): Flow<InstallState> =
        coordinator.install(request.toInstallRequest())

    override fun observe(githubRepoId: Long): Flow<InstallState> = flow {
        emit(installedStateOf(githubRepoId))
    }

    override suspend fun cancel(githubRepoId: Long) = Unit

    override suspend fun open(githubRepoId: Long) {
        val packageName = store.packageNameOf(githubRepoId) ?: return
        val intent = context.packageManager.getLaunchIntentForPackage(packageName) ?: return

        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private suspend fun installedStateOf(githubRepoId: Long): InstallState {
        val installed = store.installs().firstOrNull { app -> app.githubRepoId == githubRepoId }
            ?: return InstallState.NotInstalled

        val isPresent = isPackagePresent(installed.packageName)
        return if (isPresent) InstallState.Installed(installed.versionTag) else InstallState.NotInstalled
    }

    private fun isPackagePresent(packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (absent: PackageManager.NameNotFoundException) {
        false
    }
}

private fun ListingInstallRequest.toInstallRequest(): InstallRequest = InstallRequest(
    target = InstallTarget(
        githubRepoId = detail.githubRepoId,
        slug = detail.slug,
        title = detail.title,
        iconUrl = detail.summary.iconUrl,
    ),
    source = InstallSource(
        downloadUrl = downloadUrl,
        versionTag = version.tag,
        assetName = version.assetName,
    ),
)
