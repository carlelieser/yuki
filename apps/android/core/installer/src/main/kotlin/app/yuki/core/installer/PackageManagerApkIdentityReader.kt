package app.yuki.core.installer

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import app.yuki.core.model.InstallFailure
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

internal class PackageManagerApkIdentityReader @Inject constructor(
    @ApplicationContext private val context: Context,
) : ApkIdentityReader {
    override fun read(apk: File): ApkIdentity {
        requireReadableFile(apk)

        val info = context.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
            ?: throw InstallException(
                InstallFailure.DownloadUnreadable,
                "Could not parse an APK from the file downloaded to ${apk.absolutePath}",
            )

        if (info.packageName.isEmpty()) {
            throw InstallException(
                InstallFailure.DownloadUnreadable,
                "The file downloaded to ${apk.absolutePath} declares no package name",
            )
        }

        return ApkIdentity(info.packageName, info.longVersionCode())
    }

    private fun requireReadableFile(apk: File) {
        if (apk.isFile && apk.length() > 0L) return

        throw InstallException(
            InstallFailure.DownloadUnreadable,
            "No readable download is present at ${apk.absolutePath}",
        )
    }
}

private fun PackageInfo.longVersionCode(): Long =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        longVersionCode
    } else {
        @Suppress("DEPRECATION")
        versionCode.toLong()
    }
