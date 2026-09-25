package app.yuki.core.database

import android.database.sqlite.SQLiteException
import app.yuki.core.installer.InstallException
import app.yuki.core.installer.InstallRecord
import app.yuki.core.installer.InstallRecorder
import app.yuki.core.model.InstallFailure
import app.yuki.core.model.InstalledApp
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class RoomInstallRecorder @Inject constructor(
    private val store: InstallStore,
    private val clock: Clock,
) : InstallRecorder {
    override suspend fun recordedPackageName(githubRepoId: Long): String? =
        store.packageNameOf(githubRepoId)

    override suspend fun record(record: InstallRecord) {
        try {
            store.record(record.toRecording())
        } catch (error: SQLiteException) {
            throw InstallException(
                InstallFailure.NotRecorded,
                "Could not record the install of ${record.target.slug}",
                error,
            )
        }
    }

    private fun InstallRecord.toRecording(): InstallRecording = InstallRecording(
        app = InstalledApp(
            githubRepoId = target.githubRepoId,
            packageName = identity.packageName,
            slug = target.slug,
            title = target.title,
            iconUrl = target.iconUrl,
            versionTag = versionTag,
        ),
        versionCode = identity.versionCode,
        installedAt = clock.instant(),
    )
}
