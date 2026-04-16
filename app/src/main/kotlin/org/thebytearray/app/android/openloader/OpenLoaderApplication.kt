package org.thebytearray.app.android.openloader

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import io.github.muntashirakon.adb.PRNGFixes

@HiltAndroidApp
class OpenLoaderApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        PRNGFixes.apply()
    }
}
