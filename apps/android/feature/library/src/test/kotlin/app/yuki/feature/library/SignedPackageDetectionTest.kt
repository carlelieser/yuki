package app.yuki.feature.library

import app.yuki.core.model.SigningIdentity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignedPackageDetectionTest {
    @Test
    fun ignoresAnAppThatSharesTheCatalogPackageNameButNotItsKey() {
        val neko = catalogEntry(9L, ARCORE, SigningIdentity(setOf(NEKO_KEY)))

        val plan = planDetection(
            device = listOf(devicePackage(ARCORE, signing = singleSigner(GOOGLE_KEY))),
            index = listOf(neko),
            recorded = emptyList(),
        )

        assertTrue(plan.detected.isEmpty())
    }

    @Test
    fun forgetsADetectionWhoseKeyNoLongerMatches() {
        val neko = catalogEntry(9L, ARCORE, SigningIdentity(setOf(NEKO_KEY)))

        val plan = planDetection(
            device = listOf(devicePackage(ARCORE, signing = singleSigner(GOOGLE_KEY))),
            index = listOf(neko),
            recorded = listOf(detectedApp(neko)),
        )

        assertEquals(listOf(9L), plan.forgotten)
    }

    @Test
    fun neverDetectsAnEntryWhoseSignersAreUnknown() {
        val unsigned = catalogEntry(9L, OBTAINIUM.packageName)

        val plan = planDetection(
            device = listOf(devicePackage(OBTAINIUM.packageName)),
            index = listOf(unsigned),
            recorded = emptyList(),
        )

        assertTrue(plan.detected.isEmpty())
    }

    @Test
    fun tellsApartTwoListingsThatShareAPackageNameBySigner() {
        val neko = catalogEntry(9L, ARCORE, SigningIdentity(setOf(NEKO_KEY)))
        val google = catalogEntry(10L, ARCORE, SigningIdentity(setOf(GOOGLE_KEY)))

        val plan = planDetection(
            device = listOf(devicePackage(ARCORE, signing = singleSigner(NEKO_KEY))),
            index = listOf(google, neko),
            recorded = emptyList(),
        )

        assertEquals(listOf(9L), plan.detected.map { row -> row.app.githubRepoId })
    }

    @Test
    fun detectsNoneWhenTwoListingsMatchTheSameInstalledApp() {
        val first = catalogEntry(9L, ARCORE, SigningIdentity(setOf(NEKO_KEY)))
        val second = catalogEntry(10L, ARCORE, SigningIdentity(setOf(NEKO_KEY)))

        val plan = planDetection(
            device = listOf(devicePackage(ARCORE, signing = singleSigner(NEKO_KEY))),
            index = listOf(first, second),
            recorded = emptyList(),
        )

        assertTrue(plan.detected.isEmpty())
    }

    @Test
    fun detectsAnInstallSignedByAKeyAnOlderReleaseUsed() {
        val entry = catalogEntry(
            9L,
            OBTAINIUM.packageName,
            SigningIdentity(setOf(NEW_KEY)),
            SigningIdentity(setOf(OLD_KEY)),
        )

        val plan = planDetection(
            device = listOf(devicePackage(OBTAINIUM.packageName, signing = singleSigner(OLD_KEY))),
            index = listOf(entry),
            recorded = emptyList(),
        )

        assertEquals(1, plan.detected.size)
    }

    @Test
    fun detectsAnOldInstallThroughTheRotationLineageOfANewerRelease() {
        val entry = catalogEntry(
            9L,
            OBTAINIUM.packageName,
            SigningIdentity(signers = setOf(NEW_KEY), lineage = setOf(OLD_KEY)),
        )

        val plan = planDetection(
            device = listOf(devicePackage(OBTAINIUM.packageName, signing = singleSigner(OLD_KEY))),
            index = listOf(entry),
            recorded = emptyList(),
        )

        assertEquals(1, plan.detected.size)
    }

    @Test
    fun detectsARotatedInstallFromTheKeyHistoryTheDeviceKeeps() {
        val entry = catalogEntry(9L, OBTAINIUM.packageName, SigningIdentity(setOf(OLD_KEY)))

        val plan = planDetection(
            device = listOf(
                devicePackage(OBTAINIUM.packageName, signing = singleSigner(NEW_KEY, OLD_KEY)),
            ),
            index = listOf(entry),
            recorded = emptyList(),
        )

        assertEquals(1, plan.detected.size)
    }

    @Test
    fun requiresEverySignerOfAnAppSignedBySeveralKeys() {
        val both = DeviceSigning(signers = setOf(OLD_KEY, NEW_KEY), history = setOf(OLD_KEY, NEW_KEY))
        val partial = catalogEntry(9L, OBTAINIUM.packageName, SigningIdentity(setOf(OLD_KEY)))
        val exact = catalogEntry(10L, TERMUX_PACKAGE.packageName, SigningIdentity(setOf(OLD_KEY, NEW_KEY)))

        val plan = planDetection(
            device = listOf(
                devicePackage(OBTAINIUM.packageName, signing = both),
                devicePackage(TERMUX_PACKAGE.packageName, signing = both),
            ),
            index = listOf(partial, exact),
            recorded = emptyList(),
        )

        assertEquals(listOf(10L), plan.detected.map { row -> row.app.githubRepoId })
    }
}
