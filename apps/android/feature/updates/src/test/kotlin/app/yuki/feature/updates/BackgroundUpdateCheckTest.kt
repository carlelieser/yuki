package app.yuki.feature.updates

import app.yuki.core.database.PendingUpdate
import app.yuki.core.model.FailureReason
import app.yuki.core.model.InstalledApp
import app.yuki.core.model.ListingDetail
import app.yuki.core.network.RemoteRequestException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class BackgroundUpdateCheckTest {
    private val pending = FakePendingUpdateStore()
    private val notifier = RecordingUpdateNotifier()
    private val isNotificationEnabled = MutableStateFlow(true)

    @Test
    fun aNewVersionIsAnnouncedWithEveryOutstandingUpdate() = runTest {
        val check = checkWith(TERMUX to "v0.119.0", AURORA to "4.7.0")

        assertEquals(BackgroundCheckOutcome.Checked, check.run())
        assertEquals(listOf(listOf(TERMUX, AURORA)), notifier.shown)
    }

    @Test
    fun theSameVersionIsNotAnnouncedTwice() = runTest {
        val check = checkWith(TERMUX to "v0.119.0")

        check.run()
        check.run()

        assertEquals(1, notifier.shown.size)
    }

    @Test
    fun aNewerVersionIsAnnouncedAgain() = runTest {
        checkWith(TERMUX to "v0.119.0").run()

        checkWith(TERMUX to "v0.120.0").run()

        assertEquals(2, notifier.shown.size)
    }

    @Test
    fun anUpdateAlreadySeenOnScreenIsNotAnnounced() = runTest {
        val seen = PendingUpdate(TERMUX.githubRepoId, "v0.119.0", isNotified = true)
        val store = FakePendingUpdateStore(listOf(seen))
        val check = backgroundCheck(store, detailsFor(TERMUX to "v0.119.0"))

        check.run()

        assertEquals(emptyList<List<InstalledApp>>(), notifier.shown)
    }

    @Test
    fun turnedOffNotificationsAnnounceNothing() = runTest {
        isNotificationEnabled.value = false
        val check = checkWith(TERMUX to "v0.119.0")

        assertEquals(BackgroundCheckOutcome.Checked, check.run())
        assertEquals(emptyList<List<InstalledApp>>(), notifier.shown)
    }

    @Test
    fun turningNotificationsBackOnDoesNotAnnounceVersionsFoundWhileOff() = runTest {
        isNotificationEnabled.value = false
        val check = checkWith(TERMUX to "v0.119.0")
        check.run()

        isNotificationEnabled.value = true
        check.run()

        assertEquals(emptyList<List<InstalledApp>>(), notifier.shown)
    }

    @Test
    fun anUpdateThatCouldNotBePostedIsAnnouncedOncePostingIsAllowed() = runTest {
        notifier.canPost = false
        val check = checkWith(TERMUX to "v0.119.0")
        check.run()

        notifier.canPost = true
        check.run()

        assertEquals(listOf(listOf(TERMUX)), notifier.shown)
    }

    @Test
    fun noUpdatesMeansNoAnnouncement() = runTest {
        val check = checkWith(TERMUX to TERMUX.versionTag)

        assertEquals(BackgroundCheckOutcome.Checked, check.run())
        assertEquals(emptyList<List<InstalledApp>>(), notifier.shown)
    }

    @Test
    fun failingToReachEveryListingAsksForARetry() = runTest {
        val unreachable = Result.failure<ListingDetail>(
            RemoteRequestException(FailureReason.Offline, "Load listing detail for slug=termux"),
        )
        val check = backgroundCheck(pending, mapOf(TERMUX.slug to unreachable))

        assertEquals(BackgroundCheckOutcome.Unreachable, check.run())
    }

    private fun checkWith(vararg found: Pair<InstalledApp, String>): BackgroundUpdateCheck =
        backgroundCheck(pending, detailsFor(*found))

    private fun backgroundCheck(
        store: FakePendingUpdateStore,
        details: Map<String, Result<ListingDetail>>,
    ): BackgroundUpdateCheck {
        val installs = FakeInstallStore(details.keys.map(::installFor))
        val check = UpdateCheck(FakeListingRepository(details), store) { flowOf(false) }

        val notice = UpdateNotice(store, notifier) { isNotificationEnabled }

        return BackgroundUpdateCheck(installs, check, notice)
    }
}

private val TERMUX = installedApp(1_234L, "termux", "v0.118.0")

private val AURORA = installedApp(5_678L, "aurora-store", "4.6.4")

private fun installFor(slug: String): InstalledApp =
    listOf(TERMUX, AURORA).first { app -> app.slug == slug }

private fun detailsFor(
    vararg found: Pair<InstalledApp, String>,
): Map<String, Result<ListingDetail>> =
    found.associate { (app, tag) ->
        app.slug to Result.success(listingDetail(app.githubRepoId, app.slug, listOf(version(tag))))
    }
