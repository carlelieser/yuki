plugins {
    id("yuki.android.library")
    id("yuki.android.hilt")
}

android {
    namespace = "app.yuki.core.installer"
}

dependencies {
    api(projects.core.model)
    implementation(libs.kotlinx.coroutines.android)
}
