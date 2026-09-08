plugins {
    id("yuki.android.library")
    id("yuki.android.hilt")
}

android {
    namespace = "app.yuki.core.installer"
}

dependencies {
    implementation(projects.core.database)
    implementation(projects.core.model)
    implementation(projects.core.shizuku)
}
