plugins {
    id("yuki.android.feature")
}

android {
    namespace = "app.yuki.feature.settings"
}

dependencies {
    implementation(projects.core.settingsApi)
    implementation(projects.core.shizuku)
    implementation(libs.androidx.datastore.preferences)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
