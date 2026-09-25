package app.yuki.core.installer

import app.yuki.core.model.InstallFailure
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

class ArchiveIdentityTest {
    @Test
    fun `a file that does not parse as an apk is not an apk`() {
        assertEquals(InstallFailure.NotAnApk, failureOf(parsed = null))
    }

    @Test
    fun `an archive without a package name is not an apk`() {
        assertEquals(InstallFailure.NotAnApk, failureOf(ApkIdentity("", versionCode = 1L)))
    }

    @Test
    fun `a parsed archive keeps its identity`() {
        val identity = ApkIdentity("com.termux", versionCode = 118L)

        assertEquals(identity, archiveIdentity(DOWNLOADED, identity))
    }
}

private val DOWNLOADED = File("/downloads/app.apk")

private fun failureOf(parsed: ApkIdentity?): InstallFailure = runCatching {
    archiveIdentity(DOWNLOADED, parsed)
}.exceptionOrNull().let { error -> (error as InstallException).failure }
