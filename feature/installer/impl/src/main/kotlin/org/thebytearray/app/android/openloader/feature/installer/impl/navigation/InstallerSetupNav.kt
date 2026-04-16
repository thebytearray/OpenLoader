package org.thebytearray.app.android.openloader.feature.installer.impl.navigation

import androidx.activity.compose.LocalActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.thebytearray.app.android.openloader.core.navigation.Navigator
import org.thebytearray.app.android.openloader.feature.installer.api.navigation.AdbSetupNavKey
import org.thebytearray.app.android.openloader.feature.installer.api.navigation.InstallQueueNavKey
import org.thebytearray.app.android.openloader.feature.installer.api.navigation.ModeSelectNavKey
import org.thebytearray.app.android.openloader.feature.installer.api.navigation.ShizukuSetupNavKey
import org.thebytearray.app.android.openloader.feature.installer.impl.InstallerViewModel
import org.thebytearray.app.android.openloader.feature.installer.impl.ui.InstallQueueRoute
import org.thebytearray.app.android.openloader.feature.installer.impl.ui.ModeSelectRoute
import org.thebytearray.app.android.openloader.feature.installer.impl.ui.ShizukuSetupRoute

/** Install method setup and queue routes. ADB pairing UI is composed from the app module. */
fun EntryProviderScope<NavKey>.installerSetupEntries(
    navigator: Navigator,
) {
    entry<ModeSelectNavKey> {
        val activity = LocalActivity.current as androidx.activity.ComponentActivity
        val viewModel: InstallerViewModel = viewModel(activity)
        ModeSelectRoute(
            viewModel = viewModel,
            onShizuku = { navigator.navigate(ShizukuSetupNavKey) },
            onAdb = { navigator.navigate(AdbSetupNavKey) },
            onShizukuAlreadyReady = { navigator.goBack() },
        )
    }
    entry<ShizukuSetupNavKey> {
        val activity = LocalActivity.current as androidx.activity.ComponentActivity
        val viewModel: InstallerViewModel = viewModel(activity)
        ShizukuSetupRoute(
            viewModel = viewModel,
            onContinue = {
                navigator.goBack()
                navigator.goBack()
            },
            onBack = { navigator.goBack() },
        )
    }
    entry<InstallQueueNavKey> {
        val activity = LocalActivity.current as androidx.activity.ComponentActivity
        val viewModel: InstallerViewModel = viewModel(activity)
        InstallQueueRoute(
            viewModel = viewModel,
            onBack = { navigator.goBack() },
        )
    }
}
