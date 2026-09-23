package app.yuki.core.shizuku

internal const val INSTALL_STATUS_SUCCESS = 0
internal const val INSTALL_STATUS_FAILURE = 1

internal const val UNINSTALL_SUCCESS = "Success"

internal object PackageManagerCommands {
    fun createSession(packageName: String, installerPackage: String): List<String> = listOf(
        "pm",
        "install-create",
        "-r",
        "-i",
        installerPackage,
        "--pkg",
        packageName,
    )

    fun writeSession(sessionId: Int, size: Long): List<String> = listOf(
        "pm",
        "install-write",
        "-S",
        size.toString(),
        sessionId.toString(),
        "base.apk",
        "-",
    )

    fun commitSession(sessionId: Int): List<String> =
        listOf("pm", "install-commit", sessionId.toString())

    fun abandonSession(sessionId: Int): List<String> =
        listOf("pm", "install-abandon", sessionId.toString())

    fun uninstall(packageName: String): List<String> =
        listOf("pm", "uninstall", "--user", "0", packageName)
}

private val SESSION_ID = Regex("""\[(\d+)]""")

internal fun parseSessionId(output: String): Int =
    SESSION_ID.find(output)?.groupValues?.get(1)?.toIntOrNull()
        ?: throw PrivilegedInstallException(
            "Could not read a session id from 'pm install-create' output: ${output.trim()}",
        )

internal class PrivilegedInstallException(message: String, cause: Throwable? = null) :
    Exception(message, cause)
