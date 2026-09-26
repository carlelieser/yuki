package app.yuki.feature.updates

import app.yuki.core.database.InstallStore
import app.yuki.core.database.PendingUpdate
import app.yuki.core.database.PendingUpdateStore
import app.yuki.core.model.InstalledApp
import javax.inject.Inject
import kotlinx.coroutines.flow.first

internal enum class BackgroundCheckOutcome {
    Checked,
    Unreachable,
}

internal class UpdateNotice @Inject constructor(
    private val pending: PendingUpdateStore,
    private val notifier: UpdateNotifier,
    private val preference: UpdateNotificationPreference,
) {
    suspend fun announce(installs: List<InstalledApp>) {
        val unnotified = pending.unnotified()
        val outstanding = outstandingUpdates(pending.observe().first(), installs)
        val unnotifiedIds = unnotified.map(PendingUpdate::githubRepoId).toSet()
        val hasNewVersion = outstanding.any { app -> app.githubRepoId in unnotifiedIds }

        if (hasNewVersion && preference.isEnabled().first()) {
            val isShown = notifier.show(outstanding)
            if (!isShown) return
        }

        pending.markNotified(unnotifiedIds.toList())
    }
}

internal class BackgroundUpdateCheck @Inject constructor(
    private val store: InstallStore,
    private val check: UpdateCheck,
    private val notice: UpdateNotice,
) {
    suspend fun run(): BackgroundCheckOutcome {
        val installs = store.installs()
        val content = check.run(installs, isSeen = false)
        val isUnreachable = installs.isNotEmpty() && content.unchecked.size == installs.size

        notice.announce(installs)
        return if (isUnreachable) BackgroundCheckOutcome.Unreachable else BackgroundCheckOutcome.Checked
    }
}
