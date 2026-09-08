plugins {
    id("yuki.android.application")
    id("yuki.android.compose")
    id("yuki.android.hilt")
}

android {
    namespace = "app.yuki"

    defaultConfig {
        applicationId = "app.yuki"
        versionCode = 1
        versionName = "0.1.0"
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.model)
    implementation(projects.feature.explore)
    implementation(projects.feature.library)
    implementation(projects.feature.listing)
    implementation(projects.feature.settings)
    implementation(projects.feature.updates)

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
}
