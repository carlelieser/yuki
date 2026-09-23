package app.yuki.feature.settings

import android.graphics.drawable.Drawable
import app.yuki.core.shizuku.ShizukuDetail
import app.yuki.core.shizuku.ShizukuMode
import app.yuki.core.shizuku.ShizukuState
import app.yuki.core.shizuku.UNKNOWN_API_VERSION
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal class FakeShizukuSource(initial: ShizukuDetail = detailOf(ShizukuState.NotInstalled)) :
    ShizukuSource {
    private val details = MutableStateFlow(initial)

    var refreshCount: Int = 0
        private set

    var permissionRequests: Int = 0
        private set

    override val detail: StateFlow<ShizukuDetail> = details.asStateFlow()

    override fun refresh() {
        refreshCount += 1
    }

    override fun requestPermission() {
        permissionRequests += 1
    }

    fun emit(next: ShizukuDetail) {
        details.value = next
    }
}

internal class FakePermissionStatusReader(
    private var statuses: Map<String, PermissionStatus> = emptyMap(),
) : PermissionStatusReader {
    override fun statusOf(permission: AppPermission): PermissionStatus =
        statuses[permission.permission] ?: PermissionStatus.Denied

    fun grant(permission: String) {
        statuses = statuses + (permission to PermissionStatus.Granted)
    }
}

internal class FakePreferenceStore(initial: YukiPreferences = YukiPreferences.Defaults) :
    PreferenceStore {
    private val stored = MutableStateFlow(initial)

    override val preferences: Flow<YukiPreferences> = stored.asStateFlow()

    override suspend fun setIncludePrereleases(isEnabled: Boolean) {
        stored.value = stored.value.copy(includePrereleases = isEnabled)
    }

    override suspend fun setInstallMode(mode: InstallMode) {
        stored.value = stored.value.copy(installMode = mode)
    }

    override suspend fun setAppearance(mode: AppearanceMode) {
        stored.value = stored.value.copy(appearance = mode)
    }

    override suspend fun setDynamicColorEnabled(isEnabled: Boolean) {
        stored.value = stored.value.copy(isDynamicColorEnabled = isEnabled)
    }

    override suspend fun setInstallerPackage(packageName: String) {
        stored.value = stored.value.copy(installerPackage = packageName)
    }
}

internal class FakeInstalledAppsReader(
    private var apps: List<InstalledApp> = emptyList(),
) : InstalledAppsReader {
    var reads: Int = 0
        private set

    fun install(app: InstalledApp) {
        apps = apps + app
    }

    override suspend fun read(): List<InstalledApp> {
        reads += 1

        return apps.sortedBy { app -> app.label.lowercase() }
    }

    override suspend fun labelOf(packageName: String): String? =
        apps.firstOrNull { app -> app.packageName == packageName }?.label

    override suspend fun iconOf(packageName: String): Drawable? = null
}

internal fun installedAppOf(packageName: String, label: String): InstalledApp =
    InstalledApp(packageName = packageName, label = label)

internal class RecordingSystemDestinations : SystemDestinations {
    var websiteOpens: Int = 0
        private set

    var launches: Int = 0
        private set

    val settingsOpened: MutableList<String> = mutableListOf()

    override fun openShizukuWebsite() {
        websiteOpens += 1
    }

    override fun launchShizuku() {
        launches += 1
    }

    override fun openPermissionSettings(permission: String) {
        settingsOpened += permission
    }
}

internal fun detailOf(
    state: ShizukuState,
    mode: ShizukuMode = ShizukuMode.Unknown,
    apiVersion: Int = UNKNOWN_API_VERSION,
): ShizukuDetail = ShizukuDetail(state = state, mode = mode, apiVersion = apiVersion)
