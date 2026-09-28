package app.yuki.feature.library

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.security.MessageDigest
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.onStart

interface InstalledPackages {
    fun isPresent(packageName: String): Boolean

    fun launchIntentExists(packageName: String): Boolean

    fun findAll(packageNames: List<String>): List<DevicePackage>

    fun observeChanges(): Flow<Unit>
}

@Singleton
internal class PackageManagerPresence @Inject constructor(
    private val packageManager: PackageManager,
    @ApplicationContext private val context: Context,
) : InstalledPackages {
    override fun isPresent(packageName: String): Boolean = infoFor(packageName) != null

    override fun launchIntentExists(packageName: String): Boolean =
        packageManager.getLaunchIntentForPackage(packageName) != null

    override fun findAll(packageNames: List<String>): List<DevicePackage> =
        packageNames.mapNotNull { packageName ->
            infoFor(packageName, signingFlags())?.let { info ->
                DevicePackage(
                    packageName = packageName,
                    versionName = info.versionName.orEmpty(),
                    versionCode = info.longVersionCode(),
                    firstInstalledAt = Instant.ofEpochMilli(info.firstInstallTime),
                    signing = info.deviceSigning(),
                )
            }
        }

    override fun observeChanges(): Flow<Unit> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.getBooleanExtra(Intent.EXTRA_REPLACING, false)) return

                trySend(Unit)
            }
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            packageChanges(),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )

        awaitClose { context.unregisterReceiver(receiver) }
    }.onStart { emit(Unit) }.conflate()

    private fun infoFor(packageName: String, flags: Int = 0): PackageInfo? = try {
        packageManager.getPackageInfo(packageName, flags)
    } catch (absent: PackageManager.NameNotFoundException) {
        null
    }
}

private fun packageChanges(): IntentFilter = IntentFilter().apply {
    addAction(Intent.ACTION_PACKAGE_ADDED)
    addAction(Intent.ACTION_PACKAGE_REMOVED)
    addAction(Intent.ACTION_PACKAGE_FULLY_REMOVED)
    addDataScheme("package")
}

private fun signingFlags(): Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
    } else {
        @Suppress("DEPRECATION")
        PackageManager.GET_SIGNATURES
    }

private fun PackageInfo.deviceSigning(): DeviceSigning {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
        @Suppress("DEPRECATION")
        val signers = signatures.orEmpty().digests()
        return DeviceSigning(signers = signers, history = signers)
    }

    val info = signingInfo ?: return DeviceSigning(signers = emptySet(), history = emptySet())
    val signers = info.apkContentsSigners.orEmpty().digests()
    if (info.hasMultipleSigners()) return DeviceSigning(signers = signers, history = signers)

    return DeviceSigning(
        signers = signers,
        history = signers + info.signingCertificateHistory.orEmpty().digests(),
    )
}

private fun Array<out Signature?>.digests(): Set<String> =
    filterNotNull().mapTo(mutableSetOf()) { signature -> sha256Hex(signature.toByteArray()) }

internal fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { byte -> "%02x".format(byte) }

private fun PackageInfo.longVersionCode(): Long =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        longVersionCode
    } else {
        @Suppress("DEPRECATION")
        versionCode.toLong()
    }

@Module
@InstallIn(SingletonComponent::class)
internal object InstalledPackagesModule {
    @Provides
    @Singleton
    fun packageManager(@ApplicationContext context: Context): PackageManager =
        context.packageManager

    @Provides
    @Singleton
    fun installedPackages(implementation: PackageManagerPresence): InstalledPackages =
        implementation
}
