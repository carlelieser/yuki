plugins {
    id("yuki.android.library")
    id("yuki.android.hilt")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.yuki.core.network"
}

dependencies {
    api(projects.core.model)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.ktor.client.mock)
}
