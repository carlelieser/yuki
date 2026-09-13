package yuki

import com.android.build.api.dsl.ApplicationExtension
import java.io.File
import org.gradle.api.Project

internal class ReleaseSigningCredentials(
    val storeFile: File,
    val storePassword: String,
    val keyAlias: String,
    val keyPassword: String,
)

internal fun Project.configureReleaseSigning(extension: ApplicationExtension) {
    val credentials = releaseSigningCredentials() ?: return

    val release = extension.signingConfigs.create("release") {
        storeFile = credentials.storeFile
        storePassword = credentials.storePassword
        keyAlias = credentials.keyAlias
        keyPassword = credentials.keyPassword
    }

    extension.buildTypes.getByName("release") { signingConfig = release }
}

private fun Project.releaseSigningCredentials(): ReleaseSigningCredentials? {
    val keystore = signingSetting("YUKI_KEYSTORE_PATH", "yukiKeystorePath")
        ?.let(::File)
        ?.takeIf(File::isFile)
        ?: return null

    val storePassword = signingSetting("YUKI_KEYSTORE_PASSWORD", "yukiKeystorePassword")
    val keyAlias = signingSetting("YUKI_KEY_ALIAS", "yukiKeyAlias")
    val keyPassword = signingSetting("YUKI_KEY_PASSWORD", "yukiKeyPassword")

    if (storePassword == null || keyAlias == null || keyPassword == null) {
        error(
            "Release keystore ${keystore.path} is configured but its credentials are " +
                "incomplete. Set YUKI_KEYSTORE_PASSWORD, YUKI_KEY_ALIAS and YUKI_KEY_PASSWORD.",
        )
    }

    return ReleaseSigningCredentials(keystore, storePassword, keyAlias, keyPassword)
}

private fun Project.signingSetting(environmentName: String, propertyName: String): String? {
    val value = providers.environmentVariable(environmentName)
        .orElse(providers.gradleProperty(propertyName))
        .orNull

    return value?.takeIf(String::isNotBlank)
}
