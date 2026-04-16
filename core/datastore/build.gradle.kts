plugins {
    alias(libs.plugins.openloader.library)
    alias(libs.plugins.openloader.hilt)
}

android {
    namespace = "org.thebytearray.app.android.openloader.core.datastore"
}

dependencies {
    api(projects.core.model)
    api(projects.core.datastoreProto)
    api(libs.androidx.datastore.preferences)
    api(libs.androidx.datastore.core)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
}
