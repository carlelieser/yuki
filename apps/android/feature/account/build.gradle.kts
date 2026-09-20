plugins {
    id("yuki.android.feature")
}

android {
    namespace = "app.yuki.feature.account"
}

dependencies {
    implementation(projects.core.auth)
    implementation(projects.core.settingsApi)
    implementation(projects.core.network)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
