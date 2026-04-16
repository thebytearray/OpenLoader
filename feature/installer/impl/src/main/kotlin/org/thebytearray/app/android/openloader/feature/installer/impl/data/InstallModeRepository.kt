package org.thebytearray.app.android.openloader.feature.installer.impl.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.thebytearray.app.android.openloader.core.datastore.OpenLoaderPreferenceKeys
import org.thebytearray.app.android.openloader.core.datastore.UserPreferences
import org.thebytearray.app.android.openloader.core.model.InstallMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstallModeRepository @Inject constructor(
    @param:UserPreferences private val dataStore: DataStore<Preferences>,
) {
    val installMode: Flow<InstallMode> = dataStore.data.map { prefs ->
        val raw = prefs[OpenLoaderPreferenceKeys.INSTALL_MODE] ?: InstallMode.SHIZUKU.name
        try {
            InstallMode.valueOf(raw)
        } catch (_: IllegalArgumentException) {
            when (raw) {
                "adb" -> InstallMode.WIRELESS_ADB
                "shizuku" -> InstallMode.SHIZUKU
                else -> InstallMode.SHIZUKU
            }
        }
    }

    val wirelessAdbConfigured: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[OpenLoaderPreferenceKeys.WIRELESS_ADB_CONFIGURED] ?: false
    }

    suspend fun setWirelessAdbConfigured(configured: Boolean) {
        dataStore.edit { prefs ->
            prefs[OpenLoaderPreferenceKeys.WIRELESS_ADB_CONFIGURED] = configured
        }
    }

    suspend fun setInstallMode(mode: InstallMode) {
        dataStore.edit { prefs ->
            prefs[OpenLoaderPreferenceKeys.INSTALL_MODE] = mode.name
        }
    }
}
