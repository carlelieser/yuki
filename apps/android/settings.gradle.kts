pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "yuki-android"

include(":app")

include(":core:database")
include(":core:datastore")
include(":core:designsystem")
include(":core:installer")
include(":core:model")
include(":core:network")
include(":core:shizuku")

include(":feature:explore")
include(":feature:library")
include(":feature:listing")
include(":feature:settings")
include(":feature:updates")
