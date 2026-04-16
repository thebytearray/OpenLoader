package org.thebytearray.app.android.openloader.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.SharedPreferencesMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

private const val USER_PREFERENCES_NAME = "openloader_preferences"
private const val ADB_KEYS_NAME = "openloader_adb_keys"


val Context.openLoaderUserPreferencesDataStore: DataStore<Preferences> by preferencesDataStore(
    name = USER_PREFERENCES_NAME,
)


val Context.openLoaderAdbKeysDataStore: DataStore<Preferences> by preferencesDataStore(
    name = ADB_KEYS_NAME,
    produceMigrations = { context ->
        listOf(SharedPreferencesMigration(context, ADB_KEYS_NAME))
    },
)
