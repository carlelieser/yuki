package app.yuki.core.shizuku

import java.io.ByteArrayOutputStream
import java.io.OutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivilegedSessionsTest {
    private val shell = FakeShellCommandRunner()
    private val sessions = PrivilegedSessions(shell)

    @Test
    fun `creates writes and commits a session in order`() {
        sessions.install(targetOf(size = 4L)) { sink -> sink.write(byteArrayOf(1, 2, 3, 4)) }

        assertEquals(
            listOf("install-create", "install-write", "install-commit"),
            shell.subcommands(),
        )
    }

    @Test
    fun `names shell as the installer package so the install is silent`() {
        sessions.install(targetOf()) { sink -> sink.write(byteArrayOf(0)) }

        assertTrue(shell.commands.first().containsInOrder("-i", SHELL_INSTALLER_PACKAGE))
    }

    @Test
    fun `names the chosen installer package on the session`() {
        val target = targetOf(installerPackage = "com.android.vending")

        sessions.install(target) { sink -> sink.write(byteArrayOf(0)) }

        assertTrue(shell.commands.first().containsInOrder("-i", "com.android.vending"))
    }

    @Test
    fun `streams the apk bytes into the write command`() {
        sessions.install(targetOf(size = 3L)) { sink -> sink.write("apk".toByteArray()) }

        assertEquals("apk", shell.writtenInput)
    }

    @Test
    fun `abandons the session when the commit fails`() {
        shell.failOn = "install-commit"

        assertThrows(PrivilegedInstallException::class.java) {
            sessions.install(targetOf()) { sink -> sink.write(byteArrayOf(0)) }
        }

        assertEquals("install-abandon", shell.subcommands().last())
    }

    @Test
    fun `abandons the session when the apk cannot be streamed`() {
        shell.failOn = "install-write"

        assertThrows(PrivilegedInstallException::class.java) {
            sessions.install(targetOf()) { sink -> sink.write(byteArrayOf(0)) }
        }

        assertEquals("install-abandon", shell.subcommands().last())
    }

    @Test
    fun `names the package when a session cannot be created`() {
        shell.failOn = "install-create"

        val error = assertThrows(PrivilegedInstallException::class.java) {
            sessions.install(targetOf()) { sink -> sink.write(byteArrayOf(0)) }
        }

        assertTrue(error.message.orEmpty().contains("com.acme.app"))
    }

    @Test
    fun `names the package when an uninstall fails`() {
        shell.failOn = "uninstall"

        val error = assertThrows(PrivilegedInstallException::class.java) {
            sessions.uninstall("com.acme.app")
        }

        assertTrue(error.message.orEmpty().contains("com.acme.app"))
    }

    @Test
    fun `uninstalls through the package manager`() {
        sessions.uninstall("com.acme.app")

        assertEquals(listOf("uninstall"), shell.subcommands())
    }

    @Test
    fun `removes the package for the primary user`() {
        sessions.uninstall("com.acme.app")

        assertTrue(shell.commands.first().containsInOrder("--user", "0"))
    }

    @Test
    fun `fails when the package manager reports failure without a failing exit code`() {
        shell.output = "Failure [DELETE_FAILED_INTERNAL_ERROR]"

        val error = assertThrows(PrivilegedInstallException::class.java) {
            sessions.uninstall("com.acme.app")
        }

        assertTrue(error.message.orEmpty().contains("DELETE_FAILED_INTERNAL_ERROR"))
    }
}

private fun targetOf(
    size: Long = 1L,
    installerPackage: String = SHELL_INSTALLER_PACKAGE,
): InstallTarget = InstallTarget(
    packageName = "com.acme.app",
    size = size,
    installerPackage = installerPackage,
)

private fun List<String>.containsInOrder(first: String, second: String): Boolean {
    val index = indexOf(first)

    return index >= 0 && getOrNull(index + 1) == second
}

private class FakeShellCommandRunner : ShellCommandRunner {
    val commands: MutableList<List<String>> = mutableListOf()
    var failOn: String? = null
    var writtenInput: String = ""
    var output: String? = null

    override fun run(command: List<String>, input: ((OutputStream) -> Unit)?): ShellResult {
        commands += command
        input?.let { write -> writtenInput = captureInput(write) }

        val subcommand = command.getOrNull(1)
        if (subcommand == failOn) return ShellResult(1, "$subcommand failed")

        return ShellResult(0, output ?: "Success: created install session [42]")
    }

    fun subcommands(): List<String> = commands.mapNotNull { command -> command.getOrNull(1) }

    private fun captureInput(write: (OutputStream) -> Unit): String {
        val buffer = ByteArrayOutputStream()
        write(buffer)

        return buffer.toString(Charsets.UTF_8.name())
    }
}
