plugins {
    alias(libs.plugins.openloader.library)
    alias(libs.plugins.openloader.hilt)
}

android {
    namespace = "org.thebytearray.app.android.openloader.core.shizuku"
}

dependencies {
    implementation(projects.core.domain)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)
}
