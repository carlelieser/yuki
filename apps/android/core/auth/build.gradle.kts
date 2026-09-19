plugins {
    id("yuki.android.library")
    id("yuki.android.hilt")
}

android {
    namespace = "app.yuki.core.auth"
}

dependencies {
    implementation(projects.core.model)
    implementation(projects.core.network)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}
