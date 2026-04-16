plugins {
    alias(libs.plugins.openloader.application)
    alias(libs.plugins.openloader.application.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.openloader.hilt)
    jacoco
}

android {
    namespace = "org.thebytearray.app.android.openloader"

    defaultConfig {
        applicationId = "org.thebytearray.app.android.openloader"
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1-beta"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val releaseKeystorePath = System.getenv("OPENLOADER_RELEASE_STORE_FILE")
    val releaseStorePassword = System.getenv("OPENLOADER_RELEASE_STORE_PASSWORD")
    val releaseKeyAlias = System.getenv("OPENLOADER_RELEASE_KEY_ALIAS")
    val releaseKeyPassword = System.getenv("OPENLOADER_RELEASE_KEY_PASSWORD")
    val ciReleaseSigning = listOf(
        releaseKeystorePath,
        releaseStorePassword,
        releaseKeyAlias,
        releaseKeyPassword,
    ).all { !it.isNullOrBlank() }

    signingConfigs {
        if (ciReleaseSigning) {
            create("openloaderRelease") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            enableUnitTestCoverage = true
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (ciReleaseSigning) {
                signingConfig = signingConfigs.getByName("openloaderRelease")
            }
        }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(projects.core.common)
    implementation(projects.core.datastore)
    implementation(projects.core.ui)
    implementation(projects.core.navigation)
    implementation(projects.core.domain)
    implementation(projects.core.adb)
    implementation(projects.core.shizuku)
    implementation(projects.feature.installer.api)
    implementation(projects.feature.installer.impl)

    implementation(libs.androidx.profileinstaller)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.materialkolor)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.shizuku.api)

    lintChecks(projects.lint)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
