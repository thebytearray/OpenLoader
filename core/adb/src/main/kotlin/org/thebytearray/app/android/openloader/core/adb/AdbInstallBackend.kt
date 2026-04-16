package org.thebytearray.app.android.openloader.core.adb

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.thebytearray.app.android.openloader.core.domain.install.InstallBackend
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AdbInstallBackend @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : InstallBackend {

    override suspend fun installStagedFile(apkFile: File): Result<String> {
        return WirelessAdbClient.install(context, apkFile.absolutePath)
    }
}
