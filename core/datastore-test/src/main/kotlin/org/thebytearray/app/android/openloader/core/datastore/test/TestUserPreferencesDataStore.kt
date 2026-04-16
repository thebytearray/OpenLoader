package org.thebytearray.app.android.openloader.core.datastore.test

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.CoroutineScope
import java.io.File


fun createTestUserPreferencesDataStore(
    scope: CoroutineScope,
    file: File,
): DataStore<Preferences> = PreferenceDataStoreFactory.create(
    scope = scope,
    produceFile = { file },
)
