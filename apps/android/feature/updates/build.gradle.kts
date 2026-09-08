plugins {
    id("yuki.android.feature")
}

android {
    namespace = "app.yuki.feature.updates"
}

dependencies {
    implementation(projects.core.database)
    implementation(projects.core.installer)
    implementation(projects.core.network)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
