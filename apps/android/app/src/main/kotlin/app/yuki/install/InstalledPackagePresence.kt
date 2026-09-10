package app.yuki.install

import android.content.Context
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class InstalledPackagePresence @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun isPresent(packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (absent: PackageManager.NameNotFoundException) {
        false
    }
}
