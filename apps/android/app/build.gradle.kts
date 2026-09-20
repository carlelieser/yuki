plugins {
    id("yuki.android.application")
    id("yuki.android.compose")
    id("yuki.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

val YUKI_STOREFRONT_URL =
    providers.gradleProperty("yukiBaseUrl").orNull ?: "https://yukistore.org/"
val YUKI_LISTING_SLUG = "carlelieser-yuki"
val YUKI_LISTING_REPO_ID = 1359590051L
val YUKI_LISTING_TITLE = "Yuki"
val YUKI_LISTING_ICON_URL =
    "https://raw.githubusercontent.com/carlelieser/yuki/main/apps/android/app/src/main/ic_launcher-playstore.png"

val YUKI_VERSION_CODE = providers.gradleProperty("yukiVersionCode").orNull?.toInt() ?: 1
val YUKI_VERSION_NAME = providers.gradleProperty("yukiVersionName").orNull ?: "1.0.0"
val YUKI_RELEASE_TAG = providers.gradleProperty("yukiReleaseTag").orNull ?: ""

android {
    namespace = "app.yuki"

    buildFeatures.buildConfig = true

    defaultConfig {
        applicationId = "app.yuki"
        versionCode = YUKI_VERSION_CODE
        versionName = YUKI_VERSION_NAME
        testInstrumentationRunner = "app.yuki.YukiTestRunner"

        buildConfigField("String", "YUKI_BASE_URL", "\"$YUKI_STOREFRONT_URL\"")
        buildConfigField("String", "YUKI_LISTING_SLUG", "\"$YUKI_LISTING_SLUG\"")
        buildConfigField("long", "YUKI_LISTING_REPO_ID", "${YUKI_LISTING_REPO_ID}L")
        buildConfigField("String", "YUKI_LISTING_TITLE", "\"$YUKI_LISTING_TITLE\"")
        buildConfigField("String", "YUKI_LISTING_ICON_URL", "\"$YUKI_LISTING_ICON_URL\"")
        buildConfigField("String", "YUKI_RELEASE_TAG", "\"$YUKI_RELEASE_TAG\"")
    }
}

dependencies {
    implementation(projects.core.auth)
    implementation(projects.core.database)
    implementation(projects.core.designsystem)
    implementation(projects.core.installer)
    implementation(projects.core.model)
    implementation(projects.core.network)
    implementation(projects.core.settingsApi)
    implementation(projects.core.shizuku)
    implementation(projects.feature.account)
    implementation(projects.feature.explore)
    implementation(projects.feature.library)
    implementation(projects.feature.listing)
    implementation(projects.feature.search)
    implementation(projects.feature.settings)
    implementation(projects.feature.updates)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)
}
