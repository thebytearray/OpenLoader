package org.thebytearray.app.android.openloader.buildlogic

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

class OpenLoaderAndroidFeatureImplConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            apply(plugin = "org.thebytearray.app.android.openloader.android.library")
            apply(plugin = "org.thebytearray.app.android.openloader.hilt")

            dependencies {
                "implementation"(project(":core:ui"))
                "implementation"(project(":core:navigation"))
                "implementation"(libs.findLibrary("androidx-lifecycle-runtime-compose").get())
                "implementation"(libs.findLibrary("androidx-lifecycle-viewmodel-compose").get())
                "implementation"(libs.findLibrary("hilt-navigation-compose").get())
                "implementation"(libs.findLibrary("hilt-lifecycle-viewmodel-compose").get())
                "implementation"(libs.findLibrary("androidx-navigation3-ui").get())
            }
        }
    }
}
