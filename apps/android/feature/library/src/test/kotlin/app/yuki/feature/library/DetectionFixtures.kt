package app.yuki.feature.library

import app.yuki.core.model.CatalogPackage
import app.yuki.core.model.InstallSource
import app.yuki.core.model.InstalledApp
import app.yuki.core.model.SigningIdentity
import java.time.Instant

internal fun devicePackage(
    packageName: String,
    versionName: String = "1.0.0",
    versionCode: Long = 1L,
    signing: DeviceSigning = singleSigner(OBTAINIUM_KEY),
): DevicePackage = DevicePackage(
    packageName = packageName,
    versionName = versionName,
    versionCode = versionCode,
    firstInstalledAt = Instant.ofEpochMilli(1_700_000_000_000),
    signing = signing,
)

internal fun singleSigner(current: String, vararg past: String): DeviceSigning =
    DeviceSigning(signers = setOf(current), history = setOf(current, *past))

internal fun catalogEntry(
    githubRepoId: Long,
    packageName: String,
    vararg identities: SigningIdentity,
): CatalogPackage = CatalogPackage(
    packageName = packageName,
    githubRepoId = githubRepoId,
    slug = "listing-$githubRepoId",
    title = "Listing $githubRepoId",
    iconUrl = null,
    identities = identities.toList(),
)

internal const val OBTAINIUM_KEY = "b353"
internal const val TERMUX_KEY = "7e70"
internal const val GOOGLE_KEY = "a0c0"
internal const val NEKO_KEY = "6e91"
internal const val OLD_KEY = "0001"
internal const val NEW_KEY = "0002"
internal const val ARCORE = "com.google.ar.core"

internal fun detectedApp(entry: CatalogPackage): InstalledApp = InstalledApp(
    githubRepoId = entry.githubRepoId,
    packageName = entry.packageName,
    slug = entry.slug,
    title = entry.title,
    iconUrl = entry.iconUrl,
    versionTag = "1.6.17",
    source = InstallSource.DETECTED,
)

internal val OBTAINIUM = CatalogPackage(
    packageName = "dev.imranr.obtainium.fdroid",
    githubRepoId = 4_242L,
    slug = "imranr98-obtainium",
    title = "Obtainium",
    iconUrl = null,
    identities = listOf(SigningIdentity(setOf(OBTAINIUM_KEY))),
)

internal val TERMUX_PACKAGE = CatalogPackage(
    packageName = "com.termux",
    githubRepoId = 1_234L,
    slug = "termux",
    title = "Termux",
    iconUrl = null,
    identities = listOf(SigningIdentity(setOf(TERMUX_KEY))),
)
