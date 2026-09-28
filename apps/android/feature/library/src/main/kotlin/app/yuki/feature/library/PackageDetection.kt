package app.yuki.feature.library

import app.yuki.core.model.CatalogPackage
import app.yuki.core.model.InstallSource
import app.yuki.core.model.InstalledApp
import app.yuki.core.model.SigningIdentity
import java.time.Instant

data class DevicePackage(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val firstInstalledAt: Instant,
    val signing: DeviceSigning,
)

data class DeviceSigning(
    val signers: Set<String>,
    val history: Set<String>,
)

data class DetectedInstall(
    val app: InstalledApp,
    val versionCode: Long,
    val installedAt: Instant,
)

data class DetectionPlan(
    val detected: List<DetectedInstall>,
    val forgotten: List<Long>,
)

fun planDetection(
    device: List<DevicePackage>,
    index: List<CatalogPackage>,
    recorded: List<InstalledApp>,
): DetectionPlan {
    val present = device.associateBy(DevicePackage::packageName)
    val recordedByRepo = recorded.associateBy(InstalledApp::githubRepoId)

    val matched = index.mapNotNull { entry ->
        present[entry.packageName]
            ?.takeIf { device -> entry.isSignedLike(device) }
            ?.let { device -> entry to device }
    }
    val unambiguous = matched
        .groupBy { (entry, _) -> entry.packageName }
        .values
        .mapNotNull { matches -> matches.singleOrNull() }

    val detected = unambiguous
        .filterNot { (entry, _) -> claimedByYuki(recordedByRepo[entry.githubRepoId]) }
        .map { (entry, device) -> detectionFor(entry, device) }

    val matchedRepoIds = detected.mapTo(mutableSetOf()) { row -> row.app.githubRepoId }

    val forgotten = recorded
        .filter(InstalledApp::isDetected)
        .filterNot { app -> app.githubRepoId in matchedRepoIds }
        .map(InstalledApp::githubRepoId)

    return DetectionPlan(detected = detected, forgotten = forgotten)
}

private fun CatalogPackage.isSignedLike(device: DevicePackage): Boolean =
    identities.any { identity -> identity.matches(device.signing) }

internal fun SigningIdentity.matches(device: DeviceSigning): Boolean {
    if (device.signers.size > 1) return signers == device.signers
    if (signers.size != 1) return false

    return (signers + lineage).any { digest -> digest in device.history }
}

private fun claimedByYuki(recorded: InstalledApp?): Boolean =
    recorded != null && !recorded.isDetected

private fun detectionFor(entry: CatalogPackage, device: DevicePackage): DetectedInstall =
    DetectedInstall(
        app = InstalledApp(
            githubRepoId = entry.githubRepoId,
            packageName = entry.packageName,
            slug = entry.slug,
            title = entry.title,
            iconUrl = entry.iconUrl,
            versionTag = device.versionName,
            source = InstallSource.DETECTED,
        ),
        versionCode = device.versionCode,
        installedAt = device.firstInstalledAt,
    )
