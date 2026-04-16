plugins {
    alias(libs.plugins.openloader.library)
    alias(libs.plugins.openloader.hilt)
}

android {
    namespace = "org.thebytearray.app.android.openloader.core.common"
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.javax.inject)
}
