plugins {
    alias(libs.plugins.openloader.library)
    alias(libs.plugins.openloader.library.compose)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "org.thebytearray.app.android.openloader.core.navigation"
}

dependencies {
    api(libs.androidx.navigation3.runtime)
    api(libs.androidx.navigation3.ui)
    implementation(libs.androidx.savedstate.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
}
