plugins {
    id("yuki.android.feature")
}

android {
    namespace = "app.yuki.feature.search"
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(projects.core.datastore)
    implementation(projects.core.network)
    implementation(libs.androidx.paging.compose)

    testImplementation(libs.androidx.paging.testing)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
