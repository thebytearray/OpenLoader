package org.thebytearray.app.android.openloader.core.domain.install

import java.io.File


fun interface InstallBackend {
    suspend fun installStagedFile(apkFile: File): Result<String>
}
