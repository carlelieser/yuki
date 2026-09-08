package app.yuki.core.shizuku

fun interface BinderListener {
    fun onBinderChanged()
}

fun interface PermissionResultListener {
    fun onPermissionResult(requestCode: Int, grantResult: Int)
}

const val PERMISSION_GRANTED: Int = 0

interface ShizukuGateway {
    fun isInstalled(): Boolean

    fun isPreV11(): Boolean

    fun pingBinder(): Boolean

    fun checkSelfPermission(): Int

    fun requestPermission(requestCode: Int)

    fun uid(): Int

    fun apiVersion(): Int

    fun addBinderReceivedListener(listener: BinderListener)

    fun removeBinderReceivedListener(listener: BinderListener)

    fun addBinderDeadListener(listener: BinderListener)

    fun removeBinderDeadListener(listener: BinderListener)

    fun addPermissionResultListener(listener: PermissionResultListener)

    fun removePermissionResultListener(listener: PermissionResultListener)
}
