package app.yuki.feature.listing

import app.yuki.core.designsystem.component.InstallAction

internal class VersionInstallDispatch(private val gateway: ListingInstallGateway) {
    suspend fun dispatch(action: InstallAction, request: ListingInstallRequest): Boolean {
        val repoId = request.detail.githubRepoId

        when (action) {
            InstallAction.Install, InstallAction.Update, InstallAction.Retry ->
                gateway.install(request)
            InstallAction.Cancel -> gateway.cancel(repoId)
            InstallAction.Open -> gateway.open(repoId)
            InstallAction.Uninstall -> return false
        }

        return true
    }
}
