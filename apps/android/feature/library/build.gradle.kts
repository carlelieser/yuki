plugins {
    id("yuki.android.feature")
}

android {
    namespace = "app.yuki.feature.library"
}

dependencies {
    implementation(projects.core.database)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
