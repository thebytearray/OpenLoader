plugins {
    alias(libs.plugins.openloader.feature.impl)
    alias(libs.plugins.openloader.library.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "org.thebytearray.app.android.openloader.feature.installer.impl"
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.datastore)
    implementation(projects.core.domain)
    implementation(projects.core.adb)
    implementation(projects.core.shizuku)
    implementation(projects.feature.installer.api)

    implementation(libs.shizuku.api)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(projects.core.testing)
    testImplementation(projects.core.datastoreTest)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.junit)
}
