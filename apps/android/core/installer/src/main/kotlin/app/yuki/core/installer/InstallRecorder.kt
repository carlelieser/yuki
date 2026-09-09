package app.yuki.core.installer

interface InstallRecorder {
    suspend fun recordedPackageName(githubRepoId: Long): String?

    suspend fun record(record: InstallRecord)
}

data class InstallRecord(
    val target: InstallTarget,
    val identity: ApkIdentity,
    val versionTag: String,
)
