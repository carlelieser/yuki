package app.yuki.core.installer

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageInstaller
import app.yuki.core.model.InstallFailure
import java.io.File
import java.io.IOException

internal interface InstallSessions {
    fun createSession(identity: ApkIdentity): Int

    fun writeApk(sessionId: Int, apk: File)

    fun commit(sessionId: Int)

    fun abandon(sessionId: Int)
}

internal class InstallSessionWriter(private val context: Context) : InstallSessions {
    private val installer: PackageInstaller get() = context.packageManager.packageInstaller

    override fun createSession(identity: ApkIdentity): Int {
        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL,
        ).apply { setAppPackageName(identity.packageName) }

        try {
            return installer.createSession(params)
        } catch (error: IOException) {
            throw InstallException(
                sessionFailure(error),
                "Could not create an install session for ${identity.packageName}",
                error,
            )
        }
    }

    override fun writeApk(sessionId: Int, apk: File) {
        try {
            streamApk(sessionId, apk)
        } catch (error: IOException) {
            abandon(sessionId)
            throw InstallException(
                sessionFailure(error),
                "Could not stream ${apk.absolutePath} into install session $sessionId",
                error,
            )
        }
    }

    private fun streamApk(sessionId: Int, apk: File) {
        installer.openSession(sessionId).use { session ->
            session.openWrite(apk.name, 0, apk.length()).use { target ->
                apk.inputStream().use { source -> source.copyTo(target) }
                session.fsync(target)
            }
        }
    }

    override fun commit(sessionId: Int) {
        try {
            installer.openSession(sessionId).use { session ->
                session.commit(statusSender(sessionId))
            }
        } catch (error: IOException) {
            abandon(sessionId)
            throw InstallException(
                InstallFailure.SessionFailed,
                "Could not commit install session $sessionId",
                error,
            )
        }
    }

    override fun abandon(sessionId: Int) {
        installer.abandonSession(sessionId)
    }

    private fun statusSender(sessionId: Int): IntentSender {
        val intent = Intent(ACTION_INSTALL_STATUS).setPackage(context.packageName)

        return PendingIntent.getBroadcast(
            context,
            sessionId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
        ).intentSender
    }
}

internal fun sessionFailure(error: IOException): InstallFailure =
    if (error.isOutOfSpace()) InstallFailure.InsufficientStorage else InstallFailure.SessionFailed
