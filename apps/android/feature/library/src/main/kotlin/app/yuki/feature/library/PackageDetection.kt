package app.yuki.feature.library

import app.yuki.core.model.CatalogPackage
import app.yuki.core.model.InstallSource
import app.yuki.core.model.InstalledApp
import java.time.Instant

data class DevicePackage(
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val firstInstalledAt: Instant,
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

    val detected = index
        .filter { entry -> entry.packageName in present }
        .filterNot { entry -> claimedByYuki(recordedByRepo[entry.githubRepoId]) }
        .mapNotNull { entry ->
            present[entry.packageName]?.let { device -> detectionFor(entry, device) }
        }

    val matchedRepoIds = detected.mapTo(mutableSetOf()) { row -> row.app.githubRepoId }

    val forgotten = recorded
        .filter(InstalledApp::isDetected)
        .filterNot { app -> app.githubRepoId in matchedRepoIds }
        .map(InstalledApp::githubRepoId)

    return DetectionPlan(detected = detected, forgotten = forgotten)
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
