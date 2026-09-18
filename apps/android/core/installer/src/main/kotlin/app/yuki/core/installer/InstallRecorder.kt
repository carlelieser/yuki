package app.yuki.core.installer

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LocalInstallRecorder

interface InstallRecorder {
    suspend fun recordedPackageName(githubRepoId: Long): String?

    suspend fun record(record: InstallRecord)
}

data class InstallRecord(
    val target: InstallTarget,
    val identity: ApkIdentity,
    val versionTag: String,
)
