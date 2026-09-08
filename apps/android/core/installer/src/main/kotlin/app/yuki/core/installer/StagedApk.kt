package app.yuki.core.installer

import java.io.File

internal data class StagedApk(
    val file: File,
    val identity: ApkIdentity,
)
