import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "org.thebytearray.app.android.openloader.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradle.plugin)
    compileOnly(libs.kotlin.gradle.plugin)
    compileOnly(libs.compose.gradle.plugin)
    compileOnly(libs.ksp.gradle.plugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "org.thebytearray.app.android.openloader.android.application"
            implementationClass =
                "org.thebytearray.app.android.openloader.buildlogic.OpenLoaderAndroidApplicationConventionPlugin"
        }
        register("androidApplicationCompose") {
            id = "org.thebytearray.app.android.openloader.android.application.compose"
            implementationClass =
                "org.thebytearray.app.android.openloader.buildlogic.OpenLoaderAndroidApplicationComposeConventionPlugin"
        }
        register("androidLibrary") {
            id = "org.thebytearray.app.android.openloader.android.library"
            implementationClass =
                "org.thebytearray.app.android.openloader.buildlogic.OpenLoaderAndroidLibraryConventionPlugin"
        }
        register("androidLibraryCompose") {
            id = "org.thebytearray.app.android.openloader.android.library.compose"
            implementationClass =
                "org.thebytearray.app.android.openloader.buildlogic.OpenLoaderAndroidLibraryComposeConventionPlugin"
        }
        register("hilt") {
            id = "org.thebytearray.app.android.openloader.hilt"
            implementationClass = "org.thebytearray.app.android.openloader.buildlogic.OpenLoaderHiltConventionPlugin"
        }
        register("androidFeatureApi") {
            id = "org.thebytearray.app.android.openloader.android.feature.api"
            implementationClass =
                "org.thebytearray.app.android.openloader.buildlogic.OpenLoaderAndroidFeatureApiConventionPlugin"
        }
        register("androidFeatureImpl") {
            id = "org.thebytearray.app.android.openloader.android.feature.impl"
            implementationClass =
                "org.thebytearray.app.android.openloader.buildlogic.OpenLoaderAndroidFeatureImplConventionPlugin"
        }
        register("jvmLibrary") {
            id = "org.thebytearray.app.android.openloader.jvm.library"
            implementationClass =
                "org.thebytearray.app.android.openloader.buildlogic.OpenLoaderJvmLibraryConventionPlugin"
        }
    }
}
