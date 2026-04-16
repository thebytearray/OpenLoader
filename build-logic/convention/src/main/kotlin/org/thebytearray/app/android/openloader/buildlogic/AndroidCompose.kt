package org.thebytearray.app.android.openloader.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

private fun Project.addComposeDependencies() {
    dependencies {
        val bom = libs.findLibrary("androidx-compose-bom").get()
        "implementation"(platform(bom))
        "androidTestImplementation"(platform(bom))
        "implementation"(libs.findLibrary("androidx-compose-ui-tooling-preview").get())
        "debugImplementation"(libs.findLibrary("androidx-compose-ui-tooling").get())
    }
}

internal fun Project.configureAndroidCompose(extension: LibraryExtension) {
    extension.apply {
        buildFeatures {
            compose = true
        }
    }
    addComposeDependencies()
}

internal fun Project.configureAndroidCompose(extension: ApplicationExtension) {
    extension.apply {
        buildFeatures {
            compose = true
        }
    }
    addComposeDependencies()
}
