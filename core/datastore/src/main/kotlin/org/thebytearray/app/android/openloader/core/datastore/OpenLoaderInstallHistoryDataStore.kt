package org.thebytearray.app.android.openloader.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import org.thebytearray.app.android.openloader.data.InstallHistory

private const val INSTALL_HISTORY_FILE = "install_history.pb"

internal val Context.openLoaderInstallHistoryDataStore: DataStore<InstallHistory> by dataStore(
    fileName = INSTALL_HISTORY_FILE,
    serializer = InstallHistorySerializer,
)
