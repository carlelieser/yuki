plugins {
    id("yuki.android.library")
    id("yuki.android.compose")
}

android {
    namespace = "app.yuki.core.designsystem"
}

dependencies {
    implementation(projects.core.model)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
}
