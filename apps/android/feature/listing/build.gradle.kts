plugins {
    id("yuki.android.feature")
}

android {
    namespace = "app.yuki.feature.listing"
}

dependencies {
    implementation(projects.core.network)
    implementation(projects.core.auth)
    implementation(libs.coil.compose)
    implementation(libs.androidx.paging.compose)

    testImplementation(libs.androidx.paging.testing)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
