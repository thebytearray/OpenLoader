plugins {
    alias(libs.plugins.openloader.library)
    alias(libs.plugins.openloader.library.compose)
}

android {
    namespace = "org.thebytearray.app.android.openloader.core.ui"
}

dependencies {
    api(projects.core.model)
    api(projects.core.designsystem)
    implementation(libs.androidx.core.ktx)
}
