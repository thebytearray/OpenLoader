plugins {
    alias(libs.plugins.openloader.feature.api)
}

android {
    namespace = "org.thebytearray.app.android.openloader.feature.installer.api"
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
}
