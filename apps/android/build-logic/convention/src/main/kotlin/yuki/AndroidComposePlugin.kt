package yuki

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

class AndroidComposePlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")

        pluginManager.withPlugin("com.android.application") {
            extensions.configure<ApplicationExtension> { enableCompose(this) }
        }
        pluginManager.withPlugin("com.android.library") {
            extensions.configure<LibraryExtension> { enableCompose(this) }
        }

        val bom = libs.findLibrary("androidx-compose-bom").get()

        dependencies {
            add("implementation", platform(bom))
            add("implementation", libs.findBundle("compose").get())
            add("androidTestImplementation", platform(bom))
            add("androidTestImplementation", libs.findLibrary("androidx-compose-ui-test-junit4").get())
            add("debugImplementation", libs.findLibrary("androidx-compose-ui-tooling").get())
            add("debugImplementation", libs.findLibrary("androidx-compose-ui-test-manifest").get())
        }
    }

    private fun enableCompose(extension: CommonExtension<*, *, *, *, *, *>) {
        extension.buildFeatures.compose = true
    }
}
