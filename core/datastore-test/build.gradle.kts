plugins {
    alias(libs.plugins.openloader.library)
}

android {
    namespace = "org.thebytearray.app.android.openloader.core.datastore.test"
}

dependencies {
    implementation(projects.core.datastore)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)
}
