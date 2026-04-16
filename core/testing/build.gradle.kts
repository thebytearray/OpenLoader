plugins {
    alias(libs.plugins.openloader.library)
}

android {
    namespace = "org.thebytearray.app.android.openloader.core.testing"
}

dependencies {
    api(libs.kotlinx.coroutines.test)
    api(libs.junit)
    implementation(libs.androidx.core.ktx)
}
