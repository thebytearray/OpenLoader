package org.thebytearray.app.android.openloader.feature.installer.impl.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.thebytearray.app.android.openloader.core.datastore.UserPreferencesDataSource
import org.thebytearray.app.android.openloader.core.model.InstallMode
import org.thebytearray.app.android.openloader.core.model.ThemeColor
import org.thebytearray.app.android.openloader.core.model.ThemeMode
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesDataSource: UserPreferencesDataSource
) : ViewModel() {

    val themeMode: StateFlow<ThemeMode> = userPreferencesDataSource.themeMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeMode.SYSTEM
        )

    val dynamicColor: StateFlow<Boolean> = userPreferencesDataSource.dynamicColor
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val themeColor: StateFlow<ThemeColor> = userPreferencesDataSource.themeColor
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ThemeColor.Coral
        )

    val installMode: StateFlow<InstallMode> = userPreferencesDataSource.installMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = InstallMode.SHIZUKU
        )

    val setupCompleted: StateFlow<Boolean> = userPreferencesDataSource.setupCompleted
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    val skipSetup: StateFlow<Boolean> = userPreferencesDataSource.skipSetup
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = false
        )

    fun setThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            userPreferencesDataSource.saveThemeMode(themeMode)
        }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch {
            userPreferencesDataSource.saveDynamicColor(enabled)
        }
    }

    fun setThemeColor(themeColor: ThemeColor) {
        viewModelScope.launch {
            userPreferencesDataSource.saveThemeColor(themeColor)
        }
    }

    fun setInstallMode(installMode: InstallMode) {
        viewModelScope.launch {
            userPreferencesDataSource.saveInstallMode(installMode)
        }
    }

    fun setSetupCompleted(completed: Boolean) {
        viewModelScope.launch {
            userPreferencesDataSource.saveSetupCompleted(completed)
        }
    }

    fun setSkipSetup(skip: Boolean) {
        viewModelScope.launch {
            userPreferencesDataSource.saveSkipSetup(skip)
        }
    }
}