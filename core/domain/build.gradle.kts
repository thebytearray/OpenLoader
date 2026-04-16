plugins {
    alias(libs.plugins.openloader.library)
}

android {
    namespace = "org.thebytearray.app.android.openloader.core.domain"
}

dependencies {
    api(projects.core.model)
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.android)
}
