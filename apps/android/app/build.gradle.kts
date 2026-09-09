plugins {
    id("yuki.android.application")
    id("yuki.android.compose")
    id("yuki.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

val YUKI_STOREFRONT_URL = "https://yukistore.org/"

android {
    namespace = "app.yuki"

    buildFeatures.buildConfig = true

    defaultConfig {
        applicationId = "app.yuki"
        versionCode = 1
        versionName = "0.1.0"
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
    implementation(projects.feature.settings)
    implementation(projects.feature.updates)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}
