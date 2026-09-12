package app.yuki.feature.listing

import app.yuki.core.model.InstallState
import app.yuki.core.model.ListingDetail
import app.yuki.core.model.ListingVersion

data class ListingUiModel(
    val detail: ListingDetail,
    val installableVersion: ListingVersion?,
) {
    val isInstallable: Boolean get() = installableVersion != null
}

data class VersionInstallState(
    val state: InstallState,
    val isEnabled: Boolean,
)

private fun ListingVersion.isInstallable(): Boolean = !isPrerelease && downloadUrl != null

fun ListingVersion.hasDownloadableAsset(): Boolean = downloadUrl != null

fun installableVersion(detail: ListingDetail): ListingVersion? =
    detail.versions.firstOrNull(ListingVersion::isInstallable)

fun ListingDetail.toUiModel(): ListingUiModel = ListingUiModel(
    detail = this,
    installableVersion = installableVersion(this),
)

private fun InstallState.isActive(): Boolean = when (this) {
    is InstallState.Downloading, InstallState.PendingUserAction -> true
    else -> false
}

fun versionInstallState(
    version: ListingVersion,
    status: ListingInstallStatus,
): VersionInstallState {
    val isMatch = status.versionTag == version.tag

    if (isMatch) return VersionInstallState(state = status.state, isEnabled = true)

    return VersionInstallState(
        state = InstallState.NotInstalled,
        isEnabled = !status.state.isActive(),
    )
}
