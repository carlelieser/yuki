package yuki

import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

internal const val JVM_TOOLCHAIN_VERSION = 17

const val MANAGED_DEVICE_NAME = "ci"
private const val MANAGED_DEVICE_API_LEVEL = 30

internal val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

internal fun VersionCatalog.version(name: String): String =
    findVersion(name).orElseThrow {
        IllegalStateException("Version catalog is missing a version named '$name'")
    }.requiredVersion

internal fun Project.configureAndroid(extension: CommonExtension) {
    extension.compileSdk = libs.version("compileSdk").toInt()
    extension.defaultConfig.minSdk = libs.version("minSdk").toInt()

    configureManagedDevices(extension)

    extension.compileOptions.sourceCompatibility = JavaVersion.VERSION_17
    extension.compileOptions.targetCompatibility = JavaVersion.VERSION_17

    configureJvmToolchain()
}

private fun configureManagedDevices(extension: CommonExtension) {
    extension.testOptions.managedDevices.localDevices.create(MANAGED_DEVICE_NAME) {
        device = "Pixel 6"
        apiLevel = MANAGED_DEVICE_API_LEVEL
        systemImageSource = "aosp-atd"
    }
}

private fun Project.configureJvmToolchain() {
    extensions.getByType<JavaPluginExtension>().toolchain {
        languageVersion.set(JavaLanguageVersion.of(JVM_TOOLCHAIN_VERSION))
    }

    extensions.getByType<KotlinAndroidProjectExtension>().compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        allWarningsAsErrors.set(false)
    }
}
