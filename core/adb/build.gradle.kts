plugins {
    alias(libs.plugins.openloader.library)
    alias(libs.plugins.openloader.hilt)
}

android {
    namespace = "org.thebytearray.app.android.openloader.core.adb"
    lint {
        baseline = file("lint-baseline.xml")
    }
}

dependencies {
    implementation(projects.core.datastore)
    implementation(projects.core.domain)
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.annotation)
    implementation(libs.bcprov.jdk15to18)
    implementation(libs.spake2.android)
    implementation(libs.conscrypt.android)
    implementation(libs.sun.security.android)
}
