package app.yuki.core.shizuku

internal class ShizukuStateResolver(private val gateway: ShizukuGateway) {
    fun resolve(): ShizukuDetail {
        if (!gateway.isInstalled()) return ShizukuDetail.Unavailable
        if (!gateway.pingBinder()) return detailFor(ShizukuState.NotRunning)
        if (gateway.isPreV11()) return ShizukuDetail.Unavailable

        return detailFor(permissionState())
    }

    private fun permissionState(): ShizukuState =
        if (gateway.checkSelfPermission() == PERMISSION_GRANTED) {
            ShizukuState.Ready
        } else {
            ShizukuState.PermissionRequired
        }

    private fun detailFor(state: ShizukuState): ShizukuDetail = ShizukuDetail(
        state = state,
        mode = if (state == ShizukuState.Ready) uidToMode(gateway.uid()) else ShizukuMode.Unknown,
        apiVersion = if (state == ShizukuState.NotRunning) UNKNOWN_API_VERSION else gateway.apiVersion(),
    )
}
