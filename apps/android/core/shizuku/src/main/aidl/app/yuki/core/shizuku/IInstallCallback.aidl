package app.yuki.core.shizuku;

oneway interface IInstallCallback {
    void onFinished(int status, String message);
}
