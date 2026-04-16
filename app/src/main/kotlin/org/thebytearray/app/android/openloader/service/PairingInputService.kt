package org.thebytearray.app.android.openloader.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import org.thebytearray.app.android.openloader.R

class PairingInputService : Service() {

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "onCreate: Creating notification channel and starting foreground")
        createNotificationChannel()
        getSystemService(NotificationManager::class.java).cancel(RESULT_NOTIFICATION_ID)
        try {
            startForeground(NOTIFICATION_ID, createPairingNotification())
            Log.d(TAG, "onCreate: Foreground service started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "onCreate: Failed to start foreground service", e)
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "onStartCommand: action=${intent?.action} flags=$flags, startId=$startId")
        when (intent?.action) {
            ACTION_UPDATE_FOREGROUND_PROGRESS -> {
                val text = intent.getStringExtra(EXTRA_PROGRESS_TEXT)
                if (text != null) {
                    try {
                        startForeground(NOTIFICATION_ID, buildProgressForegroundNotification(text))
                        Log.d(TAG, "onStartCommand: Foreground notification updated (progress)")
                    } catch (e: Exception) {
                        Log.e(TAG, "onStartCommand: Failed progress foreground update", e)
                    }
                }
            }
            ACTION_SHOW_PAIRING_RESULT -> {
                val success = intent.getBooleanExtra(EXTRA_PAIRING_SUCCESS, false)
                val errorMessage = intent.getStringExtra(EXTRA_PAIRING_ERROR_MESSAGE)
                finishPairingUiAndNotify(success, errorMessage)
            }
            else -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    try {
                        startForeground(NOTIFICATION_ID, createPairingNotification())
                        Log.d(TAG, "onStartCommand: Foreground notification refreshed (default)")
                    } catch (e: Exception) {
                        Log.e(TAG, "onStartCommand: Failed to update foreground notification", e)
                    }
                }
            }
        }
        return START_STICKY
    }

    private fun createPairingNotification(): Notification {
        Log.d(TAG, "createPairingNotification: Building notification")
        
        val remoteInput = RemoteInput.Builder(KEY_PAIRING_INPUT)
            .setLabel(getString(R.string.pairing_reply_label))
            .build()

        val replyIntent = Intent(this, PairingInputReceiver::class.java).apply {
            action = ACTION_PAIRING_INPUT
        }

        val replyPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            replyIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        Log.d(TAG, "createPairingNotification: Reply action created with action=$ACTION_PAIRING_INPUT")

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(getString(R.string.pairing_notification_title))
            .setContentText(getString(R.string.pairing_notification_text))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(getString(R.string.pairing_notification_big_text)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .addAction(
                NotificationCompat.Action.Builder(
                    android.R.drawable.ic_input_add,
                    getString(R.string.pairing_reply_action),
                    replyPendingIntent
                )
                    .addRemoteInput(remoteInput)
                    .setAllowGeneratedReplies(false)
                    .build()
            )
            .build()
    }

   
    private fun buildProgressForegroundNotification(statusText: String): Notification {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(getString(R.string.pairing_progress))
            .setContentText(statusText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(statusText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(0, 0, true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        if (Build.VERSION.SDK_INT >= 34) {
            builder.setForegroundServiceBehavior(Notification.FOREGROUND_SERVICE_IMMEDIATE)
        }
        return builder.build()
    }


    private fun finishPairingUiAndNotify(success: Boolean, errorMessage: String?) {
        try {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.w(TAG, "finishPairingUiAndNotify: stopForeground", e)
        }
        val nm = getSystemService(NotificationManager::class.java)
        val notification = if (success) {
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(getString(R.string.pairing_success))
                .setContentText(getString(R.string.pairing_success_text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
        } else {
            NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_notification)
                .setContentTitle(getString(R.string.pairing_failed))
                .setContentText(errorMessage ?: getString(R.string.pairing_failed))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .build()
        }
        nm.notify(RESULT_NOTIFICATION_ID, notification)
        stopSelf()
    }

    private fun createNotificationChannel() {
        Log.d(TAG, "createNotificationChannel: Creating notification channel for API ${Build.VERSION.SDK_INT}")
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Pairing Input",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Enter pairing code directly from notification"
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            enableVibration(true)
            enableLights(true)
        }
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
        Log.d(TAG, "createNotificationChannel: Channel created successfully")
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "onDestroy: Service destroyed")
    }

    companion object {
        const val TAG = "PairingInputService"
        const val CHANNEL_ID = "pairing_input_channel"
        const val NOTIFICATION_ID = 3001
        const val KEY_PAIRING_INPUT = "pairing_input"
        const val ACTION_PAIRING_INPUT = "org.thebytearray.app.android.openloader.PAIRING_INPUT"

        const val ACTION_UPDATE_FOREGROUND_PROGRESS =
            "org.thebytearray.app.android.openloader.UPDATE_PAIRING_FOREGROUND_PROGRESS"

        const val EXTRA_PROGRESS_TEXT = "extra_progress_text"

        const val ACTION_SHOW_PAIRING_RESULT =
            "org.thebytearray.app.android.openloader.SHOW_PAIRING_RESULT"

        const val EXTRA_PAIRING_SUCCESS = "extra_pairing_success"
        const val EXTRA_PAIRING_ERROR_MESSAGE = "extra_pairing_error_message"

        const val RESULT_NOTIFICATION_ID = 3002

        fun requestProgressForegroundUpdate(context: android.content.Context, statusText: String) {
            val i = Intent(context, PairingInputService::class.java).apply {
                action = ACTION_UPDATE_FOREGROUND_PROGRESS
                putExtra(EXTRA_PROGRESS_TEXT, statusText)
            }
            ContextCompat.startForegroundService(context, i)
        }

        fun requestPairingFinished(context: android.content.Context, success: Boolean, errorMessage: String? = null) {
            val i = Intent(context, PairingInputService::class.java).apply {
                action = ACTION_SHOW_PAIRING_RESULT
                putExtra(EXTRA_PAIRING_SUCCESS, success)
                putExtra(EXTRA_PAIRING_ERROR_MESSAGE, errorMessage)
            }
            ContextCompat.startForegroundService(context, i)
        }
    }
}