plugins {
    alias(libs.plugins.openloader.library)
    alias(libs.plugins.openloader.library.compose)
}

android {
    namespace = "org.thebytearray.app.android.openloader.core.designsystem"
}

dependencies {
    api(libs.androidx.compose.foundation)
    api(libs.androidx.compose.material.icons.extended)
    api(libs.androidx.compose.material3)
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui)
    api(libs.androidx.compose.ui.tooling.preview)
    api(libs.androidx.ui.text.google.fonts)
    api(libs.materialkolor)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
