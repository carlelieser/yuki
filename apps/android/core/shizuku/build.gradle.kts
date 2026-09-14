plugins {
    id("yuki.android.library")
    id("yuki.android.hilt")
}

android {
    namespace = "app.yuki.core.shizuku"

    buildFeatures {
        aidl = true
        buildConfig = true
    }

    defaultConfig {
        buildConfigField("int", "VERSION_CODE", "1")
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(projects.core.installer)
    implementation(projects.core.model)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
}
