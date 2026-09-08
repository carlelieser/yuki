plugins {
    id("yuki.android.library")
    id("yuki.android.hilt")
}

android {
    namespace = "app.yuki.core.database"
}

dependencies {
    api(projects.core.model)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.kotlinx.coroutines.test)
}
