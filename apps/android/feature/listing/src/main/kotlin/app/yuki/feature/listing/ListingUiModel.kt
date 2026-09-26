package app.yuki.feature.listing

import app.yuki.core.model.InstallState
import app.yuki.core.model.ListingDetail
import app.yuki.core.model.ListingVersion

data class InstallableVersion(
    val version: ListingVersion,
    val downloadUrl: String,
) {
    val tag: String get() = version.tag
}

fun ListingVersion.toInstallable(): InstallableVersion? =
    downloadUrl?.let { url -> InstallableVersion(version = this, downloadUrl = url) }

data class ListingUiModel(
    val detail: ListingDetail,
    val installableVersion: InstallableVersion?,
) {
    val isInstallable: Boolean get() = installableVersion != null
}

data class VersionInstallState(
    val state: InstallState,
    val isEnabled: Boolean,
)

fun installableVersion(detail: ListingDetail): InstallableVersion? =
    detail.versions
        .filterNot(ListingVersion::isPrerelease)
        .firstNotNullOfOrNull(ListingVersion::toInstallable)

fun ListingDetail.toUiModel(): ListingUiModel = ListingUiModel(
    detail = this,
    installableVersion = installableVersion(this),
)

private fun InstallState.isActive(): Boolean = when (this) {
    InstallState.Queued,
    is InstallState.Downloading,
    InstallState.Installing,
    InstallState.PendingUserAction,
    -> true
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
