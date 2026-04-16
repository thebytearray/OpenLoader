plugins {
    alias(libs.plugins.openloader.jvm.library)
}

dependencies {
    compileOnly(libs.lint.api)
    testImplementation(libs.lint.tests)
    testImplementation(libs.lint.checks)
    testImplementation(libs.kotlin.test)
}
