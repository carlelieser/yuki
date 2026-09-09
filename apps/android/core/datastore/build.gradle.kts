plugins {
    id("yuki.android.library")
    id("yuki.android.hilt")
}

android {
    namespace = "app.yuki.core.datastore"
}

dependencies {
    implementation(projects.core.model)
    implementation(libs.androidx.datastore.preferences)
}
