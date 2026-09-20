package app.yuki.core.shizuku

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

internal const val PERMISSION_REQUEST_CODE: Int = 4801

@Singleton
class ShizukuMonitor @Inject constructor(private val gateway: ShizukuGateway) {
    private val resolver = ShizukuStateResolver(gateway)

    private val details = MutableStateFlow(resolver.resolve())
    private val permissionResults = MutableSharedFlow<Int>(extraBufferCapacity = 4)

    private val binderReceived = BinderListener { refresh() }
    private val binderDead = BinderListener { refresh() }
    private val permissionResult = PermissionResultListener { requestCode, grantResult ->
        if (requestCode == PERMISSION_REQUEST_CODE) permissionResults.tryEmit(grantResult)
        refresh()
    }

    val detail: StateFlow<ShizukuDetail> = details.asStateFlow()

    val permissionOutcomes: SharedFlow<Int> = permissionResults.asSharedFlow()

    val state: ShizukuState get() = details.value.state

    fun start() {
        gateway.addBinderReceivedListener(binderReceived)
        gateway.addBinderDeadListener(binderDead)
        gateway.addPermissionResultListener(permissionResult)
        refresh()
    }

    fun stop() {
        gateway.removeBinderReceivedListener(binderReceived)
        gateway.removeBinderDeadListener(binderDead)
        gateway.removePermissionResultListener(permissionResult)
    }

    fun refresh() {
        details.value = resolver.resolve()
    }

    fun requestPermission() {
        if (state != ShizukuState.PermissionRequired) return
        gateway.requestPermission(PERMISSION_REQUEST_CODE)
    }

    fun isReady(): Boolean {
        refresh()
        return state == ShizukuState.Ready
    }
}
