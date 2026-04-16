package org.thebytearray.app.android.openloader.navigation

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import org.thebytearray.app.android.openloader.R
import org.thebytearray.app.android.openloader.core.navigation.Navigator
import org.thebytearray.app.android.openloader.feature.installer.api.navigation.AdbSetupNavKey
import org.thebytearray.app.android.openloader.feature.installer.impl.InstallerViewModel
import org.thebytearray.app.android.openloader.feature.installer.impl.ui.AdbSetupRoute
import org.thebytearray.app.android.openloader.service.PairingInputService

private const val API_WIRELESS_DEBUGGING_SETTINGS = 34
private const val ACTION_WIRELESS_DEBUGGING_SETTINGS = "android.settings.WIRELESS_DEBUGGING_SETTINGS"

private fun android.content.Context.launchWirelessDebuggingOrDeveloperSettings() {
    val newTask = Intent.FLAG_ACTIVITY_NEW_TASK
    val pm = packageManager
    fun tryStart(intent: Intent): Boolean =
        try {
            if (intent.resolveActivity(pm) != null) {
                startActivity(intent)
                true
            } else {
                false
            }
        } catch (_: ActivityNotFoundException) {
            false
        }
    if (Build.VERSION.SDK_INT >= API_WIRELESS_DEBUGGING_SETTINGS) {
        val wireless = Intent(ACTION_WIRELESS_DEBUGGING_SETTINGS).apply { addFlags(newTask) }
        if (tryStart(wireless)) return
    }
    val dev = Intent(android.provider.Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
        addFlags(newTask)
    }
    if (!tryStart(dev)) {
        Toast.makeText(this, getString(R.string.settings_wireless_debugging_open_failed), Toast.LENGTH_SHORT).show()
    }
}

fun EntryProviderScope<NavKey>.adbSetupAppEntry(
    navigator: Navigator,
) {
    entry<AdbSetupNavKey> {
        val context = LocalContext.current
        val activity = LocalActivity.current as androidx.activity.ComponentActivity
        val viewModel: InstallerViewModel = viewModel(activity)

        var hasNotificationPermission by remember {
            mutableStateOf(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED
                } else {
                    true
                },
            )
        }

        val permissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestPermission(),
        ) { isGranted ->
            hasNotificationPermission = isGranted
            if (isGranted) {
                val serviceIntent = Intent(context, PairingInputService::class.java)
                context.startForegroundService(serviceIntent)
                context.launchWirelessDebuggingOrDeveloperSettings()
            } else {
                Toast.makeText(
                    context,
                    "Notification permission required for pairing input",
                    Toast.LENGTH_LONG,
                ).show()
            }
        }

        AdbSetupRoute(
            viewModel = viewModel,
            onContinue = {
                viewModel.markWirelessAdbSetupFinished()
                navigator.goBack()
                navigator.goBack()
            },
            onBack = { navigator.goBack() },
            onStartNotificationPairing = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    if (hasNotificationPermission) {
                        val serviceIntent = Intent(context, PairingInputService::class.java)
                        context.startForegroundService(serviceIntent)
                        context.launchWirelessDebuggingOrDeveloperSettings()
                    } else {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                } else {
                    val serviceIntent = Intent(context, PairingInputService::class.java)
                    context.startForegroundService(serviceIntent)
                    context.launchWirelessDebuggingOrDeveloperSettings()
                }
            },
        )
    }
}
