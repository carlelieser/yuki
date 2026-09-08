package app.yuki.core.shizuku

internal const val GRANTED = PERMISSION_GRANTED
internal const val DENIED = -1

internal class FakeShizukuGateway(
    var installed: Boolean = true,
    var running: Boolean = true,
) : ShizukuGateway {
    var preV11: Boolean = false
    var permission: Int = GRANTED
    var shizukuUid: Int = ADB_SHELL_UID
    var apiVersion: Int = 13

    val permissionRequests: MutableList<Int> = mutableListOf()

    private val binderReceived = mutableListOf<BinderListener>()
    private val binderDead = mutableListOf<BinderListener>()
    private val permissionListeners = mutableListOf<PermissionResultListener>()

    val liveListenerCount: Int
        get() = binderReceived.size + binderDead.size + permissionListeners.size

    override fun isInstalled(): Boolean = installed

    override fun isPreV11(): Boolean = preV11

    override fun pingBinder(): Boolean = running

    override fun checkSelfPermission(): Int = permission

    override fun requestPermission(requestCode: Int) {
        permissionRequests += requestCode
    }

    override fun uid(): Int = shizukuUid

    override fun apiVersion(): Int = apiVersion

    override fun addBinderReceivedListener(listener: BinderListener) {
        binderReceived += listener
    }

    override fun removeBinderReceivedListener(listener: BinderListener) {
        binderReceived -= listener
    }

    override fun addBinderDeadListener(listener: BinderListener) {
        binderDead += listener
    }

    override fun removeBinderDeadListener(listener: BinderListener) {
        binderDead -= listener
    }

    override fun addPermissionResultListener(listener: PermissionResultListener) {
        permissionListeners += listener
    }

    override fun removePermissionResultListener(listener: PermissionResultListener) {
        permissionListeners -= listener
    }

    fun emitBinderReceived() {
        binderReceived.toList().forEach(BinderListener::onBinderChanged)
    }

    fun emitBinderDead() {
        binderDead.toList().forEach(BinderListener::onBinderChanged)
    }

    fun emitPermissionResult(requestCode: Int, grantResult: Int) {
        permissionListeners.toList().forEach { listener ->
            listener.onPermissionResult(requestCode, grantResult)
        }
    }
}
