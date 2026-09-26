package app.yuki.core.installer

import app.yuki.core.model.InstallFailure
import java.io.File

internal fun verifyDownloadedApk(apk: File, source: InstallSource): File {
    if (!apk.isFile) {
        throw InstallException(
            InstallFailure.DownloadUnreadable,
            "Downloaded file ${apk.absolutePath} for ${source.downloadUrl} does not exist",
        )
    }

    if (apk.length() <= 0L) {
        throw InstallException(
            InstallFailure.DownloadUnreadable,
            "Downloaded file ${apk.absolutePath} for ${source.downloadUrl} is empty",
        )
    }

    return apk
}
