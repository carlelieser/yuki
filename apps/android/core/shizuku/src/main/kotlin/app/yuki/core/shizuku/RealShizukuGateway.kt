package app.yuki.core.shizuku

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuProvider
import rikka.sui.Sui

@Singleton
internal class RealShizukuGateway @Inject constructor(
    @ApplicationContext private val context: Context,
) : ShizukuGateway {
    private val binderReceivedListeners = ListenerRegistry<BinderListener, Shizuku.OnBinderReceivedListener>()
    private val binderDeadListeners = ListenerRegistry<BinderListener, Shizuku.OnBinderDeadListener>()
    private val permissionListeners =
        ListenerRegistry<PermissionResultListener, Shizuku.OnRequestPermissionResultListener>()

    override fun isInstalled(): Boolean = hasShizukuPermissionDeclared() || Sui.isSui()

    override fun isPreV11(): Boolean = Shizuku.isPreV11()

    override fun pingBinder(): Boolean = Shizuku.pingBinder()

    override fun checkSelfPermission(): Int = Shizuku.checkSelfPermission()

    override fun requestPermission(requestCode: Int) = Shizuku.requestPermission(requestCode)

    override fun uid(): Int = Shizuku.getUid()

    override fun apiVersion(): Int = Shizuku.getVersion()

    override fun addBinderReceivedListener(listener: BinderListener) {
        val adapter = Shizuku.OnBinderReceivedListener { listener.onBinderChanged() }
        binderReceivedListeners.put(listener, adapter)
        Shizuku.addBinderReceivedListenerSticky(adapter)
    }

    override fun removeBinderReceivedListener(listener: BinderListener) {
        binderReceivedListeners.take(listener)?.let(Shizuku::removeBinderReceivedListener)
    }

    override fun addBinderDeadListener(listener: BinderListener) {
        val adapter = Shizuku.OnBinderDeadListener { listener.onBinderChanged() }
        binderDeadListeners.put(listener, adapter)
        Shizuku.addBinderDeadListener(adapter)
    }

    override fun removeBinderDeadListener(listener: BinderListener) {
        binderDeadListeners.take(listener)?.let(Shizuku::removeBinderDeadListener)
    }

    override fun addPermissionResultListener(listener: PermissionResultListener) {
        val adapter = Shizuku.OnRequestPermissionResultListener(listener::onPermissionResult)
        permissionListeners.put(listener, adapter)
        Shizuku.addRequestPermissionResultListener(adapter)
    }

    override fun removePermissionResultListener(listener: PermissionResultListener) {
        permissionListeners.take(listener)?.let(Shizuku::removeRequestPermissionResultListener)
    }

    private fun hasShizukuPermissionDeclared(): Boolean = try {
        context.packageManager.getPermissionInfo(ShizukuProvider.PERMISSION, 0) != null
    } catch (missing: PackageManager.NameNotFoundException) {
        false
    }
}

private class ListenerRegistry<Key : Any, Adapter : Any> {
    private val adapters = mutableMapOf<Key, Adapter>()

    @Synchronized
    fun put(key: Key, adapter: Adapter) {
        adapters[key] = adapter
    }

    @Synchronized
    fun take(key: Key): Adapter? = adapters.remove(key)
}
