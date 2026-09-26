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
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.work.runtime.ktx)
    ksp(libs.androidx.hilt.compiler)

    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
}
