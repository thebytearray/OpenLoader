package org.thebytearray.app.android.openloader.core.datastore.history

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.thebytearray.app.android.openloader.data.InstallHistory
import org.thebytearray.app.android.openloader.data.InstallRecord
import javax.inject.Inject
import javax.inject.Singleton

data class InstallHistoryEntry(
    val packageName: String,
    val appLabel: String,
    val installedAtEpochMs: Long,
    val success: Boolean,
    val resultMessage: String,
)

@Singleton
class InstallHistoryRepository @Inject constructor(
    private val dataStore: DataStore<InstallHistory>,
) {

    val history: Flow<List<InstallHistoryEntry>> = dataStore.data.map { proto ->
        proto.entriesList.map { r ->
            InstallHistoryEntry(
                packageName = r.packageName,
                appLabel = r.appLabel,
                installedAtEpochMs = r.installedAtEpochMs,
                success = r.success,
                resultMessage = r.resultMessage,
            )
        }
    }

    suspend fun append(
        packageName: String,
        appLabel: String,
        success: Boolean,
        resultMessage: String?,
    ) {
        val record = InstallRecord.newBuilder()
            .setPackageName(packageName)
            .setAppLabel(appLabel)
            .setInstalledAtEpochMs(System.currentTimeMillis())
            .setSuccess(success)
            .setResultMessage(resultMessage.orEmpty())
            .build()

        dataStore.updateData { prev ->
            val merged = mutableListOf<InstallRecord>()
            merged.add(record)
            merged.addAll(prev.entriesList)
            while (merged.size > MAX_ENTRIES) {
                merged.removeAt(merged.lastIndex)
            }
            InstallHistory.newBuilder()
                .addAllEntries(merged)
                .build()
        }
    }

    suspend fun clearAll() {
        dataStore.updateData { InstallHistory.getDefaultInstance() }
    }

    companion object {
        private const val MAX_ENTRIES = 50
    }
}
