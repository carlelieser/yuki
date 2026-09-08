plugins {
    id("yuki.android.feature")
}

android {
    namespace = "app.yuki.feature.listing"
}

dependencies {
    implementation(projects.core.network)
    implementation(libs.coil.compose)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
