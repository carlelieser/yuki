package app.yuki.feature.library

import app.yuki.core.model.CatalogPackage
import app.yuki.core.model.InstallSource
import app.yuki.core.model.InstalledApp
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PackageDetectionTest {
    @Test
    fun detectsACatalogAppThatIsAlreadyOnTheDevice() {
        val plan = planDetection(
            device = listOf(devicePackage(OBTAINIUM.packageName)),
            index = listOf(OBTAINIUM),
            recorded = emptyList(),
        )

        assertEquals(listOf(OBTAINIUM.githubRepoId), plan.detected.map { row -> row.app.githubRepoId })
        assertEquals(InstallSource.DETECTED, plan.detected.single().app.source)
    }

    @Test
    fun ignoresCatalogAppsThatAreNotInstalled() {
        val plan = planDetection(
            device = emptyList(),
            index = listOf(OBTAINIUM, TERMUX_PACKAGE),
            recorded = emptyList(),
        )

        assertTrue(plan.detected.isEmpty())
    }

    @Test
    fun ignoresInstalledAppsThatAreNotInTheCatalog() {
        val plan = planDetection(
            device = listOf(devicePackage("com.example.unknown")),
            index = listOf(OBTAINIUM),
            recorded = emptyList(),
        )

        assertTrue(plan.detected.isEmpty())
    }

    @Test
    fun neverDowngradesAnAppYukiInstalledItself() {
        val installed = InstalledApp(
            githubRepoId = OBTAINIUM.githubRepoId,
            packageName = OBTAINIUM.packageName,
            slug = OBTAINIUM.slug,
            title = OBTAINIUM.title,
            iconUrl = null,
            versionTag = "v1.6.17",
        )

        val plan = planDetection(
            device = listOf(devicePackage(OBTAINIUM.packageName)),
            index = listOf(OBTAINIUM),
            recorded = listOf(installed),
        )

        assertTrue(plan.detected.isEmpty())
        assertTrue(plan.forgotten.isEmpty())
    }

    @Test
    fun forgetsADetectedAppThatIsNoLongerInstalled() {
        val plan = planDetection(
            device = emptyList(),
            index = listOf(OBTAINIUM),
            recorded = listOf(detectedApp(OBTAINIUM)),
        )

        assertEquals(listOf(OBTAINIUM.githubRepoId), plan.forgotten)
    }

    @Test
    fun keepsAYukiInstallThatIsMissingFromTheIndex() {
        val installed = InstalledApp(
            githubRepoId = 999L,
            packageName = "com.termux",
            slug = "termux",
            title = "Termux",
            iconUrl = null,
            versionTag = "v0.118.0",
        )

        val plan = planDetection(
            device = emptyList(),
            index = emptyList(),
            recorded = listOf(installed),
        )

        assertTrue(plan.forgotten.isEmpty())
    }

    @Test
    fun recordsTheVersionTheDeviceReports() {
        val plan = planDetection(
            device = listOf(devicePackage(OBTAINIUM.packageName, versionName = "1.6.17")),
            index = listOf(OBTAINIUM),
            recorded = emptyList(),
        )

        assertEquals("1.6.17", plan.detected.single().app.versionTag)
    }

    @Test
    fun recordsTheVersionCodeTheDeviceReports() {
        val plan = planDetection(
            device = listOf(devicePackage(OBTAINIUM.packageName, versionCode = 2_026L)),
            index = listOf(OBTAINIUM),
            recorded = emptyList(),
        )

        assertEquals(2_026L, plan.detected.single().versionCode)
    }

    @Test
    fun datesADetectedAppFromWhenTheDeviceFirstInstalledIt() {
        val installedAt = Instant.ofEpochMilli(1_600_000_000_000)

        val plan = planDetection(
            device = listOf(
                DevicePackage(OBTAINIUM.packageName, "1.6.17", 1L, installedAt),
            ),
            index = listOf(OBTAINIUM),
            recorded = emptyList(),
        )

        assertEquals(installedAt, plan.detected.single().installedAt)
    }

    @Test
    fun keepsDetectingAnAppThatIsAlreadyRecordedAsDetected() {
        val plan = planDetection(
            device = listOf(devicePackage(OBTAINIUM.packageName)),
            index = listOf(OBTAINIUM),
            recorded = listOf(detectedApp(OBTAINIUM)),
        )

        assertEquals(1, plan.detected.size)
        assertTrue(plan.forgotten.isEmpty())
    }
}

private fun devicePackage(
    packageName: String,
    versionName: String = "1.0.0",
    versionCode: Long = 1L,
): DevicePackage = DevicePackage(
    packageName = packageName,
    versionName = versionName,
    versionCode = versionCode,
    firstInstalledAt = Instant.ofEpochMilli(1_700_000_000_000),
)

private fun detectedApp(entry: CatalogPackage): InstalledApp = InstalledApp(
    githubRepoId = entry.githubRepoId,
    packageName = entry.packageName,
    slug = entry.slug,
    title = entry.title,
    iconUrl = entry.iconUrl,
    versionTag = "1.6.17",
    source = InstallSource.DETECTED,
)

private val OBTAINIUM = CatalogPackage(
    packageName = "dev.imranr.obtainium.fdroid",
    githubRepoId = 4_242L,
    slug = "imranr98-obtainium",
    title = "Obtainium",
    iconUrl = null,
)

private val TERMUX_PACKAGE = CatalogPackage(
    packageName = "com.termux",
    githubRepoId = 1_234L,
    slug = "termux",
    title = "Termux",
    iconUrl = null,
)
