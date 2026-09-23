package app.yuki.core.shizuku

import java.io.OutputStream

internal data class InstallTarget(
    val packageName: String,
    val size: Long,
    val installerPackage: String,
)

internal class PrivilegedSessions(private val shell: ShellCommandRunner) {
    fun install(target: InstallTarget, source: (OutputStream) -> Unit) {
        val packageName = target.packageName
        val sessionId = createSession(packageName, target.installerPackage)

        try {
            writeApk(sessionId, target.size, source)
            commit(sessionId, packageName)
        } catch (error: PrivilegedInstallException) {
            shell.run(PackageManagerCommands.abandonSession(sessionId))
            throw error
        }
    }

    fun uninstall(packageName: String) {
        val result = shell.run(PackageManagerCommands.uninstall(packageName))
        if (result.isSuccess && result.output.contains(UNINSTALL_SUCCESS)) return

        throw PrivilegedInstallException(
            "Failed to uninstall $packageName: ${result.output.trim()}",
        )
    }

    private fun createSession(packageName: String, installerPackage: String): Int {
        val command = PackageManagerCommands.createSession(packageName, installerPackage)
        val result = shell.run(command)
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
