package org.thebytearray.app.android.openloader.core.datastore

import androidx.datastore.core.Serializer
import org.thebytearray.app.android.openloader.data.InstallHistory
import java.io.InputStream
import java.io.OutputStream

object InstallHistorySerializer : Serializer<InstallHistory> {
    override val defaultValue: InstallHistory = InstallHistory.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): InstallHistory = try {
        InstallHistory.parseFrom(input)
    } catch (_: Exception) {
        defaultValue
    }

    override suspend fun writeTo(t: InstallHistory, output: OutputStream) {
        t.writeTo(output)
    }
}
