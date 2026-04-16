package org.thebytearray.app.android.openloader.feature.installer.impl.data

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApkStaging @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    fun stageUri(uri: Uri): File {
        val outFile = File(context.cacheDir, "staged_${UUID.randomUUID()}.apk")
        context.contentResolver.openInputStream(uri)?.use { input ->
            FileOutputStream(outFile).use { output ->
                input.copyTo(output)
            }
        } ?: error("Cannot read APK from $uri")
        return outFile
    }
}
