plugins {
    id("yuki.android.application")
    id("yuki.android.compose")
    id("yuki.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

val YUKI_STOREFRONT_URL = "https://yukistore.org/"

val YUKI_VERSION_CODE = providers.gradleProperty("yukiVersionCode").orNull?.toInt() ?: 1
val YUKI_VERSION_NAME = providers.gradleProperty("yukiVersionName").orNull ?: "0.1.0"

android {
    namespace = "app.yuki"

    buildFeatures.buildConfig = true

    defaultConfig {
        applicationId = "app.yuki"
        versionCode = YUKI_VERSION_CODE
        versionName = YUKI_VERSION_NAME
        testInstrumentationRunner = "app.yuki.YukiTestRunner"

        buildConfigField("String", "YUKI_BASE_URL", "\"$YUKI_STOREFRONT_URL\"")
    }
}

dependencies {
    implementation(projects.core.database)
    implementation(projects.core.designsystem)
    implementation(projects.core.installer)
    implementation(projects.core.model)
    implementation(projects.core.network)
    implementation(projects.core.shizuku)
    implementation(projects.feature.explore)
    implementation(projects.feature.library)
    implementation(projects.feature.listing)
    implementation(projects.feature.search)
    implementation(projects.feature.settings)
    implementation(projects.feature.updates)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}
