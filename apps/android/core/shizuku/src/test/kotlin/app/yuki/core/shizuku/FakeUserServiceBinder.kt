package app.yuki.core.shizuku

internal class FakeUserServiceBinder : UserServiceBinder {
    override suspend fun <T> withInstaller(block: suspend (IYukiInstaller) -> T): T =
        throw PrivilegedInstallException(
            "The fake user service binder never reaches a privileged process",
        )
}
