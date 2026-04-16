package org.thebytearray.app.android.openloader.core.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.thebytearray.app.android.openloader.core.model.InstallMode
import org.thebytearray.app.android.openloader.core.model.ThemeColor
import org.thebytearray.app.android.openloader.core.model.ThemeMode
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesDataSource @Inject constructor(
    @param:UserPreferences private val dataStore: DataStore<Preferences>,
) {

    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        val themeName = prefs[OpenLoaderPreferenceKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(themeName)
        } catch (_: IllegalArgumentException) {
            ThemeMode.SYSTEM
        }
    }

    val dynamicColor: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[OpenLoaderPreferenceKeys.DYNAMIC_COLOR] ?: true
    }

    val themeColor: Flow<ThemeColor> = dataStore.data.map { prefs ->
        val colorName = prefs[OpenLoaderPreferenceKeys.THEME_COLOR] ?: ThemeColor.Coral.name
        try {
            ThemeColor.valueOf(colorName)
        } catch (_: IllegalArgumentException) {
            ThemeColor.Coral
        }
    }

    val installMode: Flow<InstallMode> = dataStore.data.map { prefs ->
        val modeName = prefs[OpenLoaderPreferenceKeys.INSTALL_MODE] ?: InstallMode.SHIZUKU.name
        try {
            InstallMode.valueOf(modeName)
        } catch (_: IllegalArgumentException) {
            InstallMode.SHIZUKU
        }
    }

    val setupCompleted: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[OpenLoaderPreferenceKeys.SETUP_COMPLETED] ?: false
    }

    val skipSetup: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[OpenLoaderPreferenceKeys.SKIP_SETUP] ?: false
    }

    suspend fun saveThemeMode(themeMode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[OpenLoaderPreferenceKeys.THEME_MODE] = themeMode.name
        }
    }

    suspend fun saveDynamicColor(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[OpenLoaderPreferenceKeys.DYNAMIC_COLOR] = enabled
        }
    }

    suspend fun saveThemeColor(themeColor: ThemeColor) {
        dataStore.edit { prefs ->
            prefs[OpenLoaderPreferenceKeys.THEME_COLOR] = themeColor.name
        }
    }

    suspend fun saveInstallMode(installMode: InstallMode) {
        dataStore.edit { prefs ->
            prefs[OpenLoaderPreferenceKeys.INSTALL_MODE] = installMode.name
        }
    }

    suspend fun saveSetupCompleted(completed: Boolean) {
        dataStore.edit { prefs ->
            prefs[OpenLoaderPreferenceKeys.SETUP_COMPLETED] = completed
        }
    }

    suspend fun saveSkipSetup(skip: Boolean) {
        dataStore.edit { prefs ->
            prefs[OpenLoaderPreferenceKeys.SKIP_SETUP] = skip
        }
    }
}
