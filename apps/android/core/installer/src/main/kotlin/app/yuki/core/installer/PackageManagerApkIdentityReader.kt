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
        val info = context.packageManager.getPackageArchiveInfo(apk.absolutePath, 0)
            ?: throw InstallException(
                InstallFailure.Incompatible,
                "Could not read package identity from APK at ${apk.absolutePath}",
            )

        if (info.packageName.isEmpty()) {
            throw InstallException(
                InstallFailure.Incompatible,
                "APK at ${apk.absolutePath} declares no package name",
            )
        }

        return ApkIdentity(info.packageName, info.longVersionCode())
    }
}

private fun PackageInfo.longVersionCode(): Long =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        longVersionCode
    } else {
        @Suppress("DEPRECATION")
        versionCode.toLong()
    }
