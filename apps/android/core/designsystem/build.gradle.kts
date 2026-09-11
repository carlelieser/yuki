plugins {
    id("yuki.android.library")
    id("yuki.android.compose")
}

android {
    namespace = "app.yuki.core.designsystem"
}

dependencies {
    api(projects.core.model)
    implementation(libs.androidx.compose.material3.adaptive.navigation.suite)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.coil.svg)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
