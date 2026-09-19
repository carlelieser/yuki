package app.yuki.install

import app.yuki.core.installer.ApkIdentity
import app.yuki.core.installer.InstallRecord
import app.yuki.core.installer.InstallRecorder
import app.yuki.core.installer.InstallTarget
import app.yuki.core.model.FailureAware
import app.yuki.core.model.FailureReason
import app.yuki.core.model.LibraryEntry
import app.yuki.core.network.LibraryRepository
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private val TERMUX = InstallRecord(
    target = InstallTarget(
        githubRepoId = 1_234L,
        slug = "termux",
        title = "Termux",
        iconUrl = null,
    ),
    identity = ApkIdentity(packageName = "com.termux", versionCode = 118L),
    versionTag = "v0.118.0",
)

class SyncingInstallRecorderTest {
    private val local = RecordingInstallRecorder()
    private val library = FakeLibraryRepository()
    private val failures = mutableListOf<String>()

    private fun recorder() = SyncingInstallRecorder(
        local = local,
        library = library,
        logFailure = { message, _ -> failures.add(message) },
    )

    @Test
    fun `an install is recorded locally and published to the account`() = runTest {
        recorder().record(TERMUX)

        assertEquals(listOf(TERMUX), local.recorded)
        assertEquals(listOf("termux" to "v0.118.0"), library.published)
    }

    @Test
    fun `a failed publish leaves the local record in place`() = runTest {
        library.result = Result.failure(TypedFailure(FailureReason.Offline))

        recorder().record(TERMUX)

        assertEquals(listOf(TERMUX), local.recorded)
    }

    @Test
    fun `a failed publish is reported rather than swallowed silently`() = runTest {
        library.result = Result.failure(TypedFailure(FailureReason.Offline))

        recorder().record(TERMUX)

        assertTrue(failures.single().contains("termux"))
    }

    @Test
    fun `the recorded package name comes from the local record`() = runTest {
        local.packageNames[1_234L] = "com.termux"

        assertEquals("com.termux", recorder().recordedPackageName(1_234L))
    }
}

private class TypedFailure(override val reason: FailureReason) :
    IOException("failed: $reason"), FailureAware

private class RecordingInstallRecorder : InstallRecorder {
    val recorded = mutableListOf<InstallRecord>()
    val packageNames = mutableMapOf<Long, String>()

    override suspend fun recordedPackageName(githubRepoId: Long): String? =
        packageNames[githubRepoId]

    override suspend fun record(record: InstallRecord) {
        recorded.add(record)
    }
}

private class FakeLibraryRepository : LibraryRepository {
    val published = mutableListOf<Pair<String, String>>()
    var result: Result<Unit> = Result.success(Unit)

    override suspend fun library(): Result<List<LibraryEntry>> =
        error("the recorder never reads the library")

    override suspend fun record(slug: String, versionTag: String): Result<Unit> {
        published.add(slug to versionTag)
        return result
    }
}
