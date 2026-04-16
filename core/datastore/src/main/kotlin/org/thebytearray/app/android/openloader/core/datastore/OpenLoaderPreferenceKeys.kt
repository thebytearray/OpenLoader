package org.thebytearray.app.android.openloader.core.datastore

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object OpenLoaderPreferenceKeys {
    val THEME_MODE = stringPreferencesKey("pref_theme_mode")
    val DYNAMIC_COLOR = booleanPreferencesKey("pref_dynamic_color")
    val THEME_COLOR = stringPreferencesKey("pref_theme_color")
    val INSTALL_MODE = stringPreferencesKey("pref_install_mode")
    val WIRELESS_ADB_CONFIGURED = booleanPreferencesKey("pref_wireless_adb_configured")
    val SETUP_COMPLETED = booleanPreferencesKey("pref_setup_completed")
    val SKIP_SETUP = booleanPreferencesKey("pref_skip_setup")
}


object AdbConnectionPreferenceKeys {
    val ADB_PAIRED = booleanPreferencesKey("adb_paired")
    val ADB_LAST_PORT = intPreferencesKey("adb_last_port")
    val ADB_LAST_CONNECTED_AT = longPreferencesKey("adb_last_connected_at")
    val ADB_HOST = stringPreferencesKey("adb_host")
}

object AdbKeysPreferenceKeys {
    val PRIVATE_KEY = stringPreferencesKey("private_key")
    val CERTIFICATE = stringPreferencesKey("certificate")
}
