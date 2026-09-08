plugins {
    `kotlin-dsl`
}

group = "app.yuki.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "yuki.android.application"
            implementationClass = "yuki.AndroidApplicationPlugin"
        }
        register("androidLibrary") {
            id = "yuki.android.library"
            implementationClass = "yuki.AndroidLibraryPlugin"
        }
        register("androidCompose") {
            id = "yuki.android.compose"
            implementationClass = "yuki.AndroidComposePlugin"
        }
        register("androidHilt") {
            id = "yuki.android.hilt"
            implementationClass = "yuki.AndroidHiltPlugin"
        }
        register("androidFeature") {
            id = "yuki.android.feature"
            implementationClass = "yuki.AndroidFeaturePlugin"
        }
    }
}
