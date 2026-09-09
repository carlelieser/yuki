package app.yuki.core.installer

import java.io.File
import kotlinx.coroutines.flow.Flow

interface InstallStrategy {
    fun install(apk: File, identity: ApkIdentity): Flow<InstallOutcome>
}

interface PrivilegedInstaller {
    suspend fun isReady(): Boolean

    fun strategy(): InstallStrategy
}
