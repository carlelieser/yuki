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

    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test:runner:1.7.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}
