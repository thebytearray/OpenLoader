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
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "OpenLoader"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
include(":app")
include(":core:model")
include(":core:common")
include(":core:testing")
include(":core:datastore-test")
include(":lint")
include(":core:designsystem")
include(":core:ui")
include(":core:datastore")
include(":core:datastore-proto")
include(":core:navigation")
include(":core:domain")
include(":core:adb")
include(":core:shizuku")
include(":feature:installer:api")
include(":feature:installer:impl")
