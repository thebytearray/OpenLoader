package org.thebytearray.app.android.openloader.core.adb

import android.content.Context
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.thebytearray.app.android.openloader.core.datastore.AdbConnectionPreferenceKeys
import org.thebytearray.app.android.openloader.core.datastore.openLoaderAdbKeysDataStore


class AdbConnectionRepository(private val context: Context) {

    private val dataStore = context.openLoaderAdbKeysDataStore


    val isPaired: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[AdbConnectionPreferenceKeys.ADB_PAIRED] == true
    }


    val lastPort: Flow<Int?> = dataStore.data.map { prefs ->
        prefs[AdbConnectionPreferenceKeys.ADB_LAST_PORT]
    }


    val host: Flow<String?> = dataStore.data.map { prefs ->
        prefs[AdbConnectionPreferenceKeys.ADB_HOST]
    }


    val lastConnectedAt: Flow<Long?> = dataStore.data.map { prefs ->
        prefs[AdbConnectionPreferenceKeys.ADB_LAST_CONNECTED_AT]
    }


    suspend fun getConnectionInfo(): AdbConnectionInfo = withContext(Dispatchers.IO) {
        val prefs = dataStore.data.first()
        AdbConnectionInfo(
            isPaired = prefs[AdbConnectionPreferenceKeys.ADB_PAIRED] == true,
            lastPort = prefs[AdbConnectionPreferenceKeys.ADB_LAST_PORT],
            host = prefs[AdbConnectionPreferenceKeys.ADB_HOST] ?: DEFAULT_HOST,
            lastConnectedAt = prefs[AdbConnectionPreferenceKeys.ADB_LAST_CONNECTED_AT] ?: 0L
        )
    }


    /**
     * Persists the **TLS connect** daemon endpoint (not the pairing port) after a successful
     * `AbsAdbConnectionManager.connect` to that host:port.
     */
    suspend fun markPaired(port: Int, host: String = DEFAULT_HOST) = withContext(Dispatchers.IO) {
        dataStore.edit { prefs ->
            prefs[AdbConnectionPreferenceKeys.ADB_PAIRED] = true
            prefs[AdbConnectionPreferenceKeys.ADB_LAST_PORT] = port
            prefs[AdbConnectionPreferenceKeys.ADB_HOST] = host
            prefs[AdbConnectionPreferenceKeys.ADB_LAST_CONNECTED_AT] = System.currentTimeMillis()
        }
    }


    suspend fun updateLastPort(port: Int) = withContext(Dispatchers.IO) {
        dataStore.edit { prefs ->
            prefs[AdbConnectionPreferenceKeys.ADB_LAST_PORT] = port
        }
    }


    suspend fun updateLastConnected() = withContext(Dispatchers.IO) {
        dataStore.edit { prefs ->
            prefs[AdbConnectionPreferenceKeys.ADB_LAST_CONNECTED_AT] = System.currentTimeMillis()
        }
    }


    suspend fun clearConnectionState() = withContext(Dispatchers.IO) {
        dataStore.edit { prefs ->
            prefs.remove(AdbConnectionPreferenceKeys.ADB_PAIRED)
            prefs.remove(AdbConnectionPreferenceKeys.ADB_LAST_PORT)
            prefs.remove(AdbConnectionPreferenceKeys.ADB_HOST)
            prefs.remove(AdbConnectionPreferenceKeys.ADB_LAST_CONNECTED_AT)
        }
    }

    companion object {
        private const val DEFAULT_HOST = "127.0.0.1"

        @Volatile
        private var INSTANCE: AdbConnectionRepository? = null

        fun getInstance(context: Context): AdbConnectionRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AdbConnectionRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }
}

/**
 * Data class representing ADB connection state.
 */
data class AdbConnectionInfo(
    val isPaired: Boolean,
    val lastPort: Int?,
    val host: String,
    val lastConnectedAt: Long
) {
    /**
     * Check if the connection info is valid for auto-reconnect.
     * We consider it valid if paired and last connection was within 7 days.
     */
    fun isValidForAutoReconnect(): Boolean {
        if (!isPaired) return false
        val sevenDaysInMs = 7L * 24 * 60 * 60 * 1000
        return (System.currentTimeMillis() - lastConnectedAt) < sevenDaysInMs
    }
}
