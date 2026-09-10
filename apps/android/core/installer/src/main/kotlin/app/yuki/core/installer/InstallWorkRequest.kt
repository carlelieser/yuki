package app.yuki.core.installer

import androidx.work.Data

internal object InstallWorkKeys {
    const val GITHUB_REPO_ID = "githubRepoId"
    const val SLUG = "slug"
    const val TITLE = "title"
    const val ICON_URL = "iconUrl"
    const val DOWNLOAD_URL = "downloadUrl"
    const val VERSION_TAG = "versionTag"
    const val ASSET_NAME = "assetName"
}

internal fun InstallRequest.toWorkData(): Data = Data.Builder()
    .putLong(InstallWorkKeys.GITHUB_REPO_ID, target.githubRepoId)
    .putString(InstallWorkKeys.SLUG, target.slug)
    .putString(InstallWorkKeys.TITLE, target.title)
    .putString(InstallWorkKeys.ICON_URL, target.iconUrl)
    .putString(InstallWorkKeys.DOWNLOAD_URL, source.downloadUrl)
    .putString(InstallWorkKeys.VERSION_TAG, source.versionTag)
    .putString(InstallWorkKeys.ASSET_NAME, source.assetName)
    .build()

internal fun Data.toInstallRequest(): InstallRequest = InstallRequest(
    target = InstallTarget(
        githubRepoId = requireLong(InstallWorkKeys.GITHUB_REPO_ID),
        slug = requireString(InstallWorkKeys.SLUG),
        title = requireString(InstallWorkKeys.TITLE),
        iconUrl = getString(InstallWorkKeys.ICON_URL),
    ),
    source = InstallSource(
        downloadUrl = requireString(InstallWorkKeys.DOWNLOAD_URL),
        versionTag = requireString(InstallWorkKeys.VERSION_TAG),
        assetName = getString(InstallWorkKeys.ASSET_NAME),
    ),
)

internal fun installWorkName(githubRepoId: Long): String = "yuki-install-$githubRepoId"

private fun Data.requireString(key: String): String = requireNotNull(getString(key)) {
    "Install work data is missing a '$key' value"
}

private fun Data.requireLong(key: String): Long {
    val value = getLong(key, MISSING_LONG)
    check(value != MISSING_LONG) { "Install work data is missing a '$key' value" }

    return value
}

private const val MISSING_LONG = Long.MIN_VALUE
