package org.thebytearray.app.android.openloader.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.core.app.RemoteInput
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.thebytearray.app.android.openloader.R
import org.thebytearray.app.android.openloader.core.adb.WirelessAdbClient
import androidx.datastore.preferences.core.edit
import org.thebytearray.app.android.openloader.core.datastore.OpenLoaderPreferenceKeys
import org.thebytearray.app.android.openloader.core.datastore.openLoaderUserPreferencesDataStore

class PairingInputReceiver : BroadcastReceiver() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive: Received broadcast with action=${intent.action}")
        
        if (intent.action != PairingInputService.ACTION_PAIRING_INPUT) {
            Log.w(TAG, "onReceive: Ignoring unknown action: ${intent.action}")
            return
        }

        val pendingResult = goAsync()

        val remoteInput = RemoteInput.getResultsFromIntent(intent)
        if (remoteInput == null) {
            Log.e(TAG, "onReceive: RemoteInput is null!")
            Toast.makeText(context, "Error: Could not read input", Toast.LENGTH_SHORT).show()
            pendingResult.finish()
            return
        }

        val input = remoteInput.getCharSequence(PairingInputService.KEY_PAIRING_INPUT)?.toString()

        if (input.isNullOrEmpty()) {
            Log.w(TAG, "onReceive: Input is null or empty")
            Toast.makeText(context, "Please enter the pairing code", Toast.LENGTH_SHORT).show()
            pendingResult.finish()
            return
        }

        val pairingCode = input.trim()

        if (pairingCode.length < 4 || !pairingCode.all { it.isDigit() }) {
            Log.w(TAG, "onReceive: Invalid pairing code format")
            Toast.makeText(
                context,
                context.getString(R.string.error_invalid_format),
                Toast.LENGTH_LONG
            ).show()
            pendingResult.finish()
            return
        }

        createNotificationChannelIfNeeded(context)
        
        Log.d(TAG, "onReceive: Showing progress notification (via FGS)")
        PairingInputService.requestProgressForegroundUpdate(
            context,
            context.getString(R.string.pairing_progress_detecting_port),
        )

        scope.launch {
            try {
                Log.d(TAG, "onReceive: Starting port auto-detection")
                val port = detectPort(context)

                if (port == null) {
                    Log.e(TAG, "onReceive: Could not detect ADB pairing port")
                    PairingInputService.requestPairingFinished(
                        context,
                        success = false,
                        errorMessage = context.getString(R.string.error_invalid_port),
                    )
                    Toast.makeText(
                        context,
                        context.getString(R.string.error_invalid_port),
                        Toast.LENGTH_LONG
                    ).show()
                    pendingResult.finish()
                    return@launch
                }
                
                Log.d(TAG, "onReceive: Detected port=$port, starting pairing")
                PairingInputService.requestProgressForegroundUpdate(
                    context,
                    context.getString(R.string.pairing_progress_pairing_with_device),
                )
                val result = WirelessAdbClient.pair(context, pairingCode, port)
                
                result.onSuccess {
                    Log.d(TAG, "onReceive: Pairing successful!")

                    withContext(Dispatchers.IO) {
                        context.openLoaderUserPreferencesDataStore.edit { prefs ->
                            prefs[OpenLoaderPreferenceKeys.WIRELESS_ADB_CONFIGURED] = true
                        }
                        Log.d(TAG, "onReceive: Marked wireless ADB as configured in DataStore")

                        val configured = context.openLoaderUserPreferencesDataStore
                            .data
                            .first()[OpenLoaderPreferenceKeys.WIRELESS_ADB_CONFIGURED]
                        Log.d(TAG, "onReceive: Verified WIRELESS_ADB_CONFIGURED = $configured")

                        WirelessAdbClient.getConnectionStatus(
                            context.applicationContext,
                            forceCheck = true,
                        )
                    }
                    PairingInputService.requestPairingFinished(context, success = true)
                    Toast.makeText(
                        context,
                        context.getString(R.string.pairing_success),
                        Toast.LENGTH_LONG
                    ).show()
                }

                result.onFailure { error ->
                    Log.e(TAG, "onReceive: Pairing failed", error)
                    PairingInputService.requestPairingFinished(
                        context,
                        success = false,
                        errorMessage = error.message ?: "Unknown error",
                    )
                    Toast.makeText(
                        context,
                        "${context.getString(R.string.pairing_failed)}: ${error.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "onReceive: Exception during pairing", e)
                PairingInputService.requestPairingFinished(
                    context,
                    success = false,
                    errorMessage = e.message ?: "Unknown error",
                )
                Toast.makeText(
                    context,
                    "Error: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            } finally {
                Log.d(TAG, "onReceive: Finishing pending result")
                pendingResult.finish()
            }
        }
    }

    private suspend fun detectPort(context: Context): Int? {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Log.d(TAG, "detectPort: Trying mDNS (_adb-tls-pairing._tcp), timeout 20s")
                val port = org.thebytearray.app.android.openloader.core.adb.AdbPortDetector.awaitPairingPort(
                    context,
                    timeoutMs = 20_000L,
                )
                if (port != null) {
                    Log.d(TAG, "detectPort: mDNS pairing port=$port")
                    return port
                }
                Log.w(TAG, "detectPort: mDNS timed out or returned no pairing service")
            } catch (e: Exception) {
                Log.w(TAG, "detectPort: mDNS detection failed", e)
            }
        }

        val commonPorts = listOf(37059, 37551, 40001, 42073, 42173, 42273, 42373, 42473)
        Log.d(TAG, "detectPort: Trying common pairing ports: $commonPorts")
        for (port in commonPorts) {
            if (isPortInUse(port)) {
                Log.d(TAG, "detectPort: Using in-use port $port as pairing candidate")
                return port
            }
        }

        Log.w(TAG, "detectPort: No pairing port found; user should check Wireless debugging is ON")
        return null
    }

    private fun isPortInUse(port: Int): Boolean = try {
        java.net.ServerSocket().use {
            it.bind(java.net.InetSocketAddress("127.0.0.1", port), 1)
            false
        }
    } catch (e: java.io.IOException) {
        true
    }

    private fun createNotificationChannelIfNeeded(context: Context) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (notificationManager.getNotificationChannel(PairingInputService.CHANNEL_ID) == null) {
            Log.d(TAG, "createNotificationChannel: Creating channel for progress notifications")
            val channel = NotificationChannel(
                PairingInputService.CHANNEL_ID,
                "Pairing Input",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Enter pairing code directly from notification"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val TAG = "PairingInputReceiver"
    }
}