package app.yuki.core.shizuku;

import app.yuki.core.shizuku.IInstallCallback;

interface IYukiInstaller {
    void install(in ParcelFileDescriptor apk, String packageName, long size, String installerPackage, IInstallCallback callback) = 1;

    void uninstall(String packageName, IInstallCallback callback) = 2;

    void destroy() = 16777114;
}
