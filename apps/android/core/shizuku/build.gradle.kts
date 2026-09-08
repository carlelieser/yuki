plugins {
    id("yuki.android.library")
    id("yuki.android.hilt")
}

android {
    namespace = "app.yuki.core.shizuku"
}

dependencies {
    implementation(projects.core.model)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
}
