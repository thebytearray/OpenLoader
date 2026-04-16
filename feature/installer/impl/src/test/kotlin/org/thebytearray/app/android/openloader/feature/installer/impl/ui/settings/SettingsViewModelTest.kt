package org.thebytearray.app.android.openloader.feature.installer.impl.ui.settings

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.thebytearray.app.android.openloader.core.datastore.UserPreferencesDataSource
import org.thebytearray.app.android.openloader.core.datastore.test.createTestUserPreferencesDataStore
import org.thebytearray.app.android.openloader.core.model.ThemeMode
import org.thebytearray.app.android.openloader.core.testing.MainDispatcherRule

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun themeMode_initialValue_isSystem() = runBlocking {
        val file = tempFolder.newFile("prefs.pb")
        val dataStore = createTestUserPreferencesDataStore(this, file)
        val viewModel = SettingsViewModel(UserPreferencesDataSource(dataStore))
        assertEquals(ThemeMode.SYSTEM, viewModel.themeMode.value)
    }
}
