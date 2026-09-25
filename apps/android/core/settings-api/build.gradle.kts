plugins {
    id("yuki.android.library")
    id("yuki.android.compose")
}

android {
    namespace = "app.yuki.core.settings.api"
}

dependencies {
    implementation(projects.core.designsystem)
    testImplementation(libs.junit)
}
