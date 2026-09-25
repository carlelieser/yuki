package app.yuki.install

import app.yuki.core.installer.InstallRequest
import app.yuki.core.installer.InstallSource
import app.yuki.core.installer.InstallTarget
import app.yuki.core.model.deviceArchitecture
import app.yuki.core.model.downloadUrl
import app.yuki.feature.listing.ListingInstallRequest

internal fun ListingInstallRequest.toInstallRequest(baseUrl: String): InstallRequest =
    InstallRequest(
        target = InstallTarget(
            githubRepoId = detail.githubRepoId,
            slug = detail.slug,
            title = detail.title,
            iconUrl = detail.summary.iconUrl,
        ),
        source = InstallSource(
            downloadUrl = downloadUrl(
                baseUrl = baseUrl,
                slug = detail.slug,
                versionTag = version.tag,
                architecture = deviceArchitecture(),
            ),
            versionTag = version.tag,
            assetName = version.version.assetName,
        ),
    )
