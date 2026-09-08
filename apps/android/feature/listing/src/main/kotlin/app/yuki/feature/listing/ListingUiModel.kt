package app.yuki.feature.listing

import app.yuki.core.model.ListingDetail
import app.yuki.core.model.ListingVersion

data class ListingUiModel(
    val detail: ListingDetail,
    val installableVersion: ListingVersion?,
) {
    val isInstallable: Boolean get() = installableVersion != null
}

private fun ListingVersion.isInstallable(): Boolean = !isPrerelease && downloadUrl != null

fun installableVersion(detail: ListingDetail): ListingVersion? =
    detail.versions.firstOrNull(ListingVersion::isInstallable)

fun ListingDetail.toUiModel(): ListingUiModel = ListingUiModel(
    detail = this,
    installableVersion = installableVersion(this),
)
