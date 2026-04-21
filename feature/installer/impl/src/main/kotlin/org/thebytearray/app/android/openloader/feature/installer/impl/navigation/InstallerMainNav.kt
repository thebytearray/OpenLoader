package org.thebytearray.app.android.openloader.feature.installer.impl.navigation

import androidx.activity.compose.LocalActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.thebytearray.app.android.openloader.core.navigation.Navigator
import org.thebytearray.app.android.openloader.feature.installer.api.navigation.AboutNavKey
import org.thebytearray.app.android.openloader.feature.installer.api.navigation.HistoryNavKey
import org.thebytearray.app.android.openloader.feature.installer.api.navigation.HomeNavKey
import org.thebytearray.app.android.openloader.feature.installer.api.navigation.ModeSelectNavKey
import org.thebytearray.app.android.openloader.feature.installer.api.navigation.SettingsNavKey
import org.thebytearray.app.android.openloader.feature.installer.impl.InstallerViewModel
import org.thebytearray.app.android.openloader.feature.installer.impl.ui.about.AboutScreen
import org.thebytearray.app.android.openloader.feature.installer.impl.ui.history.InstallHistoryScreen
import org.thebytearray.app.android.openloader.feature.installer.impl.ui.home.HomeScreen
import org.thebytearray.app.android.openloader.feature.installer.impl.ui.settings.SettingsScreen

fun EntryProviderScope<NavKey>.installerMainEntries(
    navigator: Navigator,
) {
    entry<HomeNavKey> {
        val activity = LocalActivity.current as androidx.activity.ComponentActivity
        val viewModel: InstallerViewModel = viewModel(activity)
        HomeScreen(
            viewModel = viewModel,
            onNavigateToSettings = { navigator.navigate(SettingsNavKey) },
            onNavigateToHistory = { navigator.navigate(HistoryNavKey) },
            onNavigateToSetup = { navigator.navigate(ModeSelectNavKey) },
        )
    }

    entry<HistoryNavKey> {
        val activity = LocalActivity.current as androidx.activity.ComponentActivity
        val viewModel: InstallerViewModel = viewModel(activity)
        InstallHistoryScreen(
            viewModel = viewModel,
            onNavigateBack = { navigator.goBack() },
        )
    }

    entry<SettingsNavKey> {
        SettingsScreen(
            onNavigateBack = { navigator.goBack() },
            onNavigateToAbout = { navigator.navigate(AboutNavKey) },
        )
    }

    entry<AboutNavKey> {
        AboutScreen(
            onNavigateBack = { navigator.goBack() },
        )
    }
}
