plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.protobuf) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.openloader.application) apply false
    alias(libs.plugins.openloader.application.compose) apply false
    alias(libs.plugins.openloader.library) apply false
    alias(libs.plugins.openloader.library.compose) apply false
    alias(libs.plugins.openloader.hilt) apply false
    alias(libs.plugins.openloader.feature.api) apply false
    alias(libs.plugins.openloader.feature.impl) apply false
    alias(libs.plugins.openloader.jvm.library) apply false
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
