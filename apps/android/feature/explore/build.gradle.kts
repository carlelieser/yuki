plugins {
    id("yuki.android.feature")
}

android {
    namespace = "app.yuki.feature.explore"
}

dependencies {
    implementation(projects.core.datastore)
    implementation(projects.core.network)
    implementation(libs.androidx.paging.compose)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
