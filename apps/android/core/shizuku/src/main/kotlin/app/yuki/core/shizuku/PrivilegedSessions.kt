package app.yuki.core.shizuku

import java.io.OutputStream

internal class PrivilegedSessions(private val shell: ShellCommandRunner) {
    fun install(packageName: String, size: Long, source: (OutputStream) -> Unit) {
        val sessionId = createSession(packageName)

        try {
            writeApk(sessionId, size, source)
            commit(sessionId, packageName)
        } catch (error: PrivilegedInstallException) {
            shell.run(PackageManagerCommands.abandonSession(sessionId))
            throw error
        }
    }

    fun uninstall(packageName: String) {
        val result = shell.run(PackageManagerCommands.uninstall(packageName))
        if (result.isSuccess) return

        throw PrivilegedInstallException(
            "Failed to uninstall $packageName: ${result.output.trim()}",
        )
    }

    private fun createSession(packageName: String): Int {
        val result = shell.run(PackageManagerCommands.createSession(packageName))
        if (!result.isSuccess) {
            throw PrivilegedInstallException(
                "Failed to create an install session for $packageName: ${result.output.trim()}",
            )
        }

        return parseSessionId(result.output)
    }

    private fun writeApk(sessionId: Int, size: Long, source: (OutputStream) -> Unit) {
        val result = shell.run(PackageManagerCommands.writeSession(sessionId, size), source)
        if (result.isSuccess) return

        throw PrivilegedInstallException(
            "Failed to stream the APK into session $sessionId: ${result.output.trim()}",
        )
    }

    private fun commit(sessionId: Int, packageName: String) {
        val result = shell.run(PackageManagerCommands.commitSession(sessionId))
        if (result.isSuccess) return

        throw PrivilegedInstallException(
            "Failed to commit session $sessionId for $packageName: ${result.output.trim()}",
        )
    }
}
