package org.thebytearray.app.android.openloader.core.adb

import android.content.Context
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.datastore.preferences.core.edit
import io.github.muntashirakon.adb.AbsAdbConnectionManager
import io.github.muntashirakon.adb.AdbAuthenticationFailedException
import io.github.muntashirakon.adb.AdbPairingRequiredException
import io.github.muntashirakon.adb.AdbStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.thebytearray.app.android.openloader.core.datastore.OpenLoaderPreferenceKeys
import org.thebytearray.app.android.openloader.core.datastore.openLoaderUserPreferencesDataStore
import java.io.File


object WirelessAdbClient {

    private const val TAG = "WirelessAdbClient"

    private val clientIoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private const val LOCALHOST = "127.0.0.1"

    /** Same fallback list as aShellYou [WifiAdbRepositoryImpl] direct connect. */
    private val directConnectFallbackPorts = listOf(5555, 37373, 42069, 5037)

    private const val MDNS_DISCOVERY_TIMEOUT_MS = 5000L
    private const val NSD_CONNECT_WAIT_MS = 8_000L

    private const val CONNECTION_CHECK_CACHE_MS = 2000L
    private const val AUTO_CONNECT_TIMEOUT_MS = 10_000L
    private const val QUICK_CHECK_TIMEOUT_MS = 3_000L

    @Volatile
    private var lastConnectionCheck: Long = 0

    @Volatile
    private var lastConnectionStatus: ConnectionStatus = ConnectionStatus.NEEDS_PAIRING

    enum class ConnectionStatus {
        NOT_CONNECTED,
        CONNECTED,
        NEEDS_PAIRING,
        ERROR,
    }

    private fun invalidateStatusCache() {
        lastConnectionCheck = 0
    }


    private fun Throwable?.indicatesRevokedOrUnauthorizedWirelessSession(): Boolean {
        var t: Throwable? = this
        while (t != null) {
            when (t) {
                is AdbPairingRequiredException -> return true
                is AdbAuthenticationFailedException -> return true
            }
            t = t.cause
        }
        return false
    }

    private fun clearConnectionStateOnRevokedSession(context: Context, throwable: Throwable?) {
        if (!throwable.indicatesRevokedOrUnauthorizedWirelessSession()) return
        val app = context.applicationContext
        if (Looper.getMainLooper().isCurrentThread) {
            clientIoScope.launch { clearConnectionState(app) }
        } else {
            runBlocking { clearConnectionState(app) }
        }
    }

    private suspend fun clearConnectionStateIfRevokedSession(context: Context, throwable: Throwable?): Boolean {
        if (!throwable.indicatesRevokedOrUnauthorizedWirelessSession()) return false
        clearConnectionState(context.applicationContext)
        return true
    }

    suspend fun autoConnectWithStoredState(
        context: Context,
        timeoutMs: Long = AUTO_CONNECT_TIMEOUT_MS,
    ): Boolean = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        val repository = AdbConnectionRepository.getInstance(app)
        val connectionInfo = repository.getConnectionInfo()

        Log.d(
            TAG,
            "autoConnectWithStoredState: isPaired=${connectionInfo.isPaired}, " +
                "host=${connectionInfo.host} port=${connectionInfo.lastPort}",
        )

        if (!connectionInfo.isPaired || connectionInfo.lastPort == null) {
            Log.d(TAG, "autoConnectWithStoredState: missing paired connect endpoint")
            return@withContext false
        }

        val manager = AdbConnectionManager.getInstance(app)

        try {
            if (manager.isConnected) {
                if (verifyEchoBlocking(app, manager)) {
                    repository.updateLastConnected()
                    return@withContext true
                }
                manager.disconnect()
            }
            if (manager.connect(connectionInfo.host, connectionInfo.lastPort)) {
                if (verifyEchoBlocking(app, manager)) {
                    repository.updateLastConnected()
                    return@withContext true
                }
            }
        } catch (e: Exception) {
            if (clearConnectionStateIfRevokedSession(app, e)) {
                return@withContext false
            }
            Log.w(TAG, "autoConnectWithStoredState: stored connect failed", e)
        }

        try {
            if (manager.isConnected) manager.disconnect()
        } catch (_: Exception) {
        }

        if (tryAutoConnectAndVerify(app, manager, timeoutMs)) {
            repository.updateLastConnected()
            return@withContext true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.d(TAG, "autoConnectWithStoredState: trying mDNS for connect port")
            val discoveredPort = withTimeoutOrNull(MDNS_DISCOVERY_TIMEOUT_MS) {
                AdbPortDetector(app).detectConnectionPort().first { it > 0 }
            }
            discoveredPort?.let { port ->
                Log.d(TAG, "autoConnectWithStoredState: mDNS port=$port, updating stored port")
                repository.updateLastPort(port)
                if (tryAutoConnectAndVerify(app, manager, timeoutMs)) {
                    repository.updateLastConnected()
                    return@withContext true
                }
            }
        }

        Log.d(TAG, "autoConnectWithStoredState: failed")
        false
    }

    /**
     * Tries the persisted TLS connect port first. Must not [runBlocking] on the main thread.
     * (The old fixed localhost port-list probe caused false "not connected" when the daemon used
     * a port outside that list — system wireless debugging could still be active.)
     */
    private fun tryStoredReconnectBlocking(app: Context, quickCheck: Boolean): Boolean {
        if (Looper.getMainLooper().isCurrentThread) return false
        val timeoutMs = if (quickCheck) QUICK_CHECK_TIMEOUT_MS else AUTO_CONNECT_TIMEOUT_MS
        return runBlocking {
            val info = AdbConnectionRepository.getInstance(app).getConnectionInfo()
            if (info.isPaired && info.lastPort != null) {
                autoConnectWithStoredState(app, timeoutMs)
            } else {
                false
            }
        }
    }

    private suspend fun tryAutoConnectAndVerify(
        context: Context,
        manager: AbsAdbConnectionManager,
        timeoutMs: Long,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!manager.autoConnect(context, timeoutMs)) {
                return@withContext false
            }
            verifyEchoBlocking(context, manager)
        } catch (e: Exception) {
            if (clearConnectionStateIfRevokedSession(context, e)) {
                return@withContext false
            }
            Log.w(TAG, "tryAutoConnectAndVerify failed", e)
            false
        }
    }

    fun getConnectionStatus(
        context: Context,
        forceCheck: Boolean = false,
        quickCheck: Boolean = true,
    ): ConnectionStatus {
        val now = System.currentTimeMillis()
        if (!forceCheck && (now - lastConnectionCheck) < CONNECTION_CHECK_CACHE_MS) {
            return lastConnectionStatus
        }

        val timeoutMs = if (quickCheck) QUICK_CHECK_TIMEOUT_MS else AUTO_CONNECT_TIMEOUT_MS
        val streamTimeout = if (quickCheck) 2000 else 5000
        val app = context.applicationContext

        if (tryStoredReconnectBlocking(app, quickCheck)) {
            lastConnectionCheck = now
            lastConnectionStatus = ConnectionStatus.CONNECTED
            return ConnectionStatus.CONNECTED
        }

        var stream: AdbStream? = null
        val status = try {
            val manager = AdbConnectionManager.getInstance(app)
            if (!manager.autoConnect(app, timeoutMs)) {
                ConnectionStatus.NEEDS_PAIRING
            } else {
                try {
                    stream = manager.openStream("shell:echo test")
                    val inputStream = stream.openInputStream()
                    val buffer = ByteArray(128)
                    var totalWait = 0
                    var bytesRead = 0
                    while (totalWait < streamTimeout) {
                        if (inputStream.available() > 0) {
                            val n = inputStream.read(buffer)
                            if (n > 0) {
                                bytesRead = n
                                break
                            }
                        } else {
                            try {
                                Thread.sleep(if (quickCheck) 50L else 100L)
                            } catch (_: InterruptedException) {
                                break
                            }
                            totalWait += if (quickCheck) 50 else 100
                        }
                    }
                    stream.close()
                    if (bytesRead > 0) ConnectionStatus.CONNECTED else ConnectionStatus.NEEDS_PAIRING
                } catch (e: Exception) {
                    clearConnectionStateOnRevokedSession(app, e)
                    Log.e(TAG, "getConnectionStatus stream test failed", e)
                    try {
                        stream?.close()
                    } catch (_: Exception) {
                    }
                    ConnectionStatus.NEEDS_PAIRING
                }
            }
        } catch (e: Exception) {
            clearConnectionStateOnRevokedSession(app, e)
            Log.e(TAG, "getConnectionStatus failed", e)
            ConnectionStatus.NEEDS_PAIRING
        }

        lastConnectionCheck = now
        lastConnectionStatus = status
        return status
    }

    suspend fun getConnectionStatusAsync(
        context: Context,
        forceCheck: Boolean = false,
        quickCheck: Boolean = true,
    ): ConnectionStatus =
        withContext(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            if (!forceCheck && (now - lastConnectionCheck) < CONNECTION_CHECK_CACHE_MS) {
                return@withContext lastConnectionStatus
            }

            val repository = AdbConnectionRepository.getInstance(context)
            val connectionInfo = repository.getConnectionInfo()
            val timeoutMs = if (quickCheck) QUICK_CHECK_TIMEOUT_MS else AUTO_CONNECT_TIMEOUT_MS

            if (connectionInfo.isPaired && connectionInfo.lastPort != null) {
                if (autoConnectWithStoredState(context, timeoutMs)) {
                    lastConnectionCheck = now
                    lastConnectionStatus = ConnectionStatus.CONNECTED
                    return@withContext ConnectionStatus.CONNECTED
                }
            }

            val status = if (quickCheck) {
                tryStandardConnectionQuick(context)
            } else {
                tryStandardConnection(context)
            }

            lastConnectionCheck = now
            lastConnectionStatus = status
            status
        }

    private suspend fun tryStandardConnection(context: Context): ConnectionStatus =
        withContext(Dispatchers.IO) {
            var stream: AdbStream? = null
            try {
                val manager = AdbConnectionManager.getInstance(context)
                if (!manager.autoConnect(context, AUTO_CONNECT_TIMEOUT_MS)) {
                    return@withContext ConnectionStatus.NEEDS_PAIRING
                }
                stream = manager.openStream("shell:echo test")
                val inputStream = stream.openInputStream()
                val buffer = ByteArray(128)
                var totalWait = 0
                var bytesRead = 0
                while (totalWait < 5000) {
                    if (inputStream.available() > 0) {
                        val n = inputStream.read(buffer)
                        if (n > 0) {
                            bytesRead = n
                            break
                        }
                    } else {
                        delay(100)
                        totalWait += 100
                    }
                }
                stream.close()
                if (bytesRead > 0) {
                    AdbConnectionRepository.getInstance(context).updateLastConnected()
                    ConnectionStatus.CONNECTED
                } else {
                    ConnectionStatus.NEEDS_PAIRING
                }
            } catch (e: Exception) {
                clearConnectionStateIfRevokedSession(context, e)
                Log.e(TAG, "tryStandardConnection", e)
                try {
                    stream?.close()
                } catch (_: Exception) {
                }
                ConnectionStatus.NEEDS_PAIRING
            }
        }

    private suspend fun tryStandardConnectionQuick(context: Context): ConnectionStatus =
        withContext(Dispatchers.IO) {
            var stream: AdbStream? = null
            try {
                val manager = AdbConnectionManager.getInstance(context)
                if (!manager.autoConnect(context, QUICK_CHECK_TIMEOUT_MS)) {
                    return@withContext ConnectionStatus.NEEDS_PAIRING
                }
                stream = manager.openStream("shell:echo test")
                val inputStream = stream.openInputStream()
                val buffer = ByteArray(128)
                var totalWait = 0
                var bytesRead = 0
                while (totalWait < 2000) {
                    if (inputStream.available() > 0) {
                        val n = inputStream.read(buffer)
                        if (n > 0) {
                            bytesRead = n
                            break
                        }
                    } else {
                        delay(50)
                        totalWait += 50
                    }
                }
                stream.close()
                if (bytesRead > 0) {
                    AdbConnectionRepository.getInstance(context).updateLastConnected()
                    ConnectionStatus.CONNECTED
                } else {
                    ConnectionStatus.NEEDS_PAIRING
                }
            } catch (e: Exception) {
                clearConnectionStateIfRevokedSession(context, e)
                Log.e(TAG, "tryStandardConnectionQuick", e)
                try {
                    stream?.close()
                } catch (_: Exception) {
                }
                ConnectionStatus.NEEDS_PAIRING
            }
        }

    /**
     * Pair with the TLS pairing service, resolve the TLS **connect** port (parallel NSD + fallbacks),
     * connect to the daemon, verify the session, then persist the **connect** host/port.
     */
    suspend fun pair(context: Context, pairingCode: String, pairingPort: Int): Result<Boolean> =
        withContext(Dispatchers.IO) {
            val app = context.applicationContext
            Log.d(TAG, "pair: pairingPort=$pairingPort")
            val parallel = ParallelTlsConnectDiscovery(app)
            parallel.start()
            try {
                val manager = AdbConnectionManager.getInstance(app)
                if (manager.isConnected) {
                    try {
                        manager.disconnect()
                    } catch (e: Exception) {
                        Log.w(TAG, "pair: disconnect before pair", e)
                    }
                }

                manager.pair(LOCALHOST, pairingPort, pairingCode)

                var connectPort = parallel.getCachedPortForHost(LOCALHOST)
                    ?: parallel.getAnyCachedLocalConnectPort()

                if (connectPort == null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    connectPort = withTimeoutOrNull(NSD_CONNECT_WAIT_MS) {
                        AdbPortDetector(app).detectConnectionPort().first { it > 0 }
                    }
                }

                if (connectPort == null) {
                    connectPort = tryDirectConnectPorts(app, manager)
                }

                if (connectPort == null) {
                    return@withContext Result.failure(
                        Exception("Could not resolve ADB TLS connect port after pairing."),
                    )
                }

                if (!manager.connect(LOCALHOST, connectPort)) {
                    return@withContext Result.failure(
                        Exception("ADB connect failed after pairing (port $connectPort)."),
                    )
                }

                if (!verifyEchoSuspend(app, manager)) {
                    try {
                        manager.disconnect()
                    } catch (_: Exception) {
                    }
                    return@withContext Result.failure(
                        Exception("ADB verification failed after connect."),
                    )
                }

                AdbConnectionRepository.getInstance(app).markPaired(host = LOCALHOST, port = connectPort)
                invalidateStatusCache()
                Result.success(true)
            } catch (e: Exception) {
                if (clearConnectionStateIfRevokedSession(app, e)) {
                    return@withContext Result.failure(
                        Exception("Wireless debugging no longer trusts this device. Pair again from developer options.", e),
                    )
                }
                Log.e(TAG, "pair failed", e)
                Result.failure(e)
            } finally {
                parallel.stop()
                parallel.clearCache()
            }
        }

    private suspend fun tryDirectConnectPorts(
        context: Context,
        manager: AbsAdbConnectionManager,
    ): Int? {
        for (port in directConnectFallbackPorts) {
            try {
                if (manager.isConnected) {
                    try {
                        manager.disconnect()
                    } catch (_: Exception) {
                    }
                }
                if (manager.connect(LOCALHOST, port) && verifyEchoSuspend(context, manager)) {
                    return port
                }
            } catch (e: Exception) {
                clearConnectionStateIfRevokedSession(context, e)
                Log.d(TAG, "tryDirectConnectPorts port=$port: ${e.message}")
            }
        }
        return null
    }

    private suspend fun verifyEchoSuspend(context: Context, manager: AbsAdbConnectionManager): Boolean =
        withContext(Dispatchers.IO) {
            var stream: AdbStream? = null
            try {
                stream = manager.openStream("shell:echo test")
                val inputStream = stream.openInputStream()
                val buffer = ByteArray(128)
                var totalWait = 0
                while (totalWait < 4000) {
                    if (inputStream.available() > 0) {
                        val n = inputStream.read(buffer)
                        if (n > 0) return@withContext true
                    } else {
                        delay(50)
                        totalWait += 50
                    }
                }
                false
            } catch (e: Exception) {
                clearConnectionStateIfRevokedSession(context, e)
                Log.w(TAG, "verifyEchoSuspend failed", e)
                false
            } finally {
                try {
                    stream?.close()
                } catch (_: Exception) {
                }
            }
        }

    private fun verifyEchoBlocking(context: Context, manager: AbsAdbConnectionManager): Boolean {
        var stream: AdbStream? = null
        return try {
            stream = manager.openStream("shell:echo test")
            val inputStream = stream.openInputStream()
            val buffer = ByteArray(128)
            var totalWait = 0
            while (totalWait < 3000) {
                if (inputStream.available() > 0) {
                    val n = inputStream.read(buffer)
                    if (n > 0) return true
                } else {
                    try {
                        Thread.sleep(100)
                    } catch (_: InterruptedException) {
                        break
                    }
                    totalWait += 100
                }
            }
            false
        } catch (e: Exception) {
            clearConnectionStateOnRevokedSession(context, e)
            Log.w(TAG, "verifyEchoBlocking failed", e)
            false
        } finally {
            try {
                stream?.close()
            } catch (_: Exception) {
            }
        }
    }

    suspend fun clearConnectionState(context: Context) = withContext(Dispatchers.IO) {
        val app = context.applicationContext
        runCatching { AdbConnectionManager.getInstance(app).disconnect() }
        AdbConnectionRepository.getInstance(app).clearConnectionState()
        app.openLoaderUserPreferencesDataStore.edit { prefs ->
            prefs[OpenLoaderPreferenceKeys.WIRELESS_ADB_CONFIGURED] = false
        }
        invalidateStatusCache()
        lastConnectionStatus = ConnectionStatus.NEEDS_PAIRING
    }

    suspend fun testConnection(context: Context): Result<Boolean> = withContext(Dispatchers.IO) {
        var stream: AdbStream? = null
        try {
            val manager = AdbConnectionManager.getInstance(context)
            val repository = AdbConnectionRepository.getInstance(context)

            val connected = if (repository.getConnectionInfo().isValidForAutoReconnect()) {
                autoConnectWithStoredState(context, 10_000)
            } else {
                manager.autoConnect(context, 10_000)
            }

            if (!connected) {
                return@withContext Result.failure(
                    Exception("Could not connect to ADB. Enable wireless debugging and pair."),
                )
            }

            stream = manager.openStream("shell:echo test")
            val output = StringBuilder()
            val inputStream = stream.openInputStream()
            val buffer = ByteArray(128)
            var totalWait = 0
            while (totalWait < 5000) {
                if (inputStream.available() > 0) {
                    val bytesRead = inputStream.read(buffer)
                    if (bytesRead > 0) {
                        output.append(String(buffer, 0, bytesRead))
                        break
                    }
                } else {
                    delay(100)
                    totalWait += 100
                }
            }
            stream.close()

            if (output.contains("test")) {
                lastConnectionCheck = System.currentTimeMillis()
                lastConnectionStatus = ConnectionStatus.CONNECTED
                repository.updateLastConnected()
                Result.success(true)
            } else {
                Result.failure(Exception("Connection test failed. Authorize the debugging prompt if shown."))
            }
        } catch (e: Exception) {
            try {
                stream?.close()
            } catch (_: Exception) {
            }
            if (clearConnectionStateIfRevokedSession(context, e)) {
                return@withContext Result.failure(
                    Exception("Wireless debugging no longer trusts this device. Pair again from developer options.", e),
                )
            }
            Result.failure(
                Exception("Authorization required. Check for the wireless debugging authorization prompt."),
            )
        }
    }

    suspend fun install(context: Context, apkPath: String): Result<String> = withContext(Dispatchers.IO) {
        var stream: AdbStream? = null
        try {
            invalidateStatusCache()

            val repository = AdbConnectionRepository.getInstance(context)
            val manager = AdbConnectionManager.getInstance(context)

            val connected = if (repository.getConnectionInfo().isValidForAutoReconnect()) {
                autoConnectWithStoredState(context, 10_000)
            } else {
                manager.autoConnect(context, 10_000)
            }

            if (!connected) {
                return@withContext Result.failure(
                    Exception("Failed to connect to ADB. Enable wireless debugging and pair this device."),
                )
            }

            val apkFile = File(apkPath)
            val apkSize = apkFile.length()

            val installResult = suspendCancellableCoroutine<Result<String>> { cont ->
                cont.invokeOnCancellation {
                    runCatching { stream?.close() }
                }
                try {
                    val installStream = manager.openStream("exec:cmd package install -S $apkSize")
                    stream = installStream
                    val outputStream = installStream.openOutputStream()
                    java.io.FileInputStream(apkFile).use { input ->
                        val buffer = ByteArray(8192)
                        var bytesRead: Int
                        while (input.read(buffer).also { bytesRead = it } != -1) {
                            if (!cont.isActive) throw CancellationException()
                            outputStream.write(buffer, 0, bytesRead)
                        }
                        outputStream.flush()
                    }

                    val output = StringBuilder()
                    val inputStream = installStream.openInputStream()
                    val readBuffer = ByteArray(1024)
                    var totalWait = 0
                    val maxWait = 30000

                    while (totalWait < maxWait) {
                        if (!cont.isActive) throw CancellationException()
                        if (inputStream.available() > 0) {
                            val br = inputStream.read(readBuffer)
                            if (br > 0) {
                                output.append(String(readBuffer, 0, br))
                            }
                            if (br == -1) break
                        } else {
                            Thread.sleep(100)
                            totalWait += 100
                            val currentOutput = output.toString()
                            if (currentOutput.contains("Success", ignoreCase = true) ||
                                currentOutput.contains("Failure", ignoreCase = true)
                            ) {
                                break
                            }
                        }
                    }

                    val result = output.toString().trim()
                    runCatching { installStream.close() }

                    if (!cont.isActive) throw CancellationException()

                    val success = result.contains("Success", ignoreCase = true)
                    if (success) {
                        lastConnectionCheck = System.currentTimeMillis()
                        lastConnectionStatus = ConnectionStatus.CONNECTED
                    }
                    val finalResult = if (success) {
                        Result.success("Installation successful")
                    } else {
                        Result.failure(Exception(result.ifEmpty { "Unknown install error" }))
                    }
                    cont.resumeWith(kotlin.Result.success(finalResult))
                } catch (e: Exception) {
                    if (e is CancellationException) {
                        cont.cancel(e)
                    } else {
                        runBlocking {
                            clearConnectionStateIfRevokedSession(context, e)
                        }
                        cont.resumeWith(kotlin.Result.success(Result.failure(e)))
                    }
                }
            }

            if (installResult.isSuccess) {
                repository.updateLastConnected()
            }

            installResult
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            if (clearConnectionStateIfRevokedSession(context, e)) {
                return@withContext Result.failure(
                    Exception("Wireless debugging no longer trusts this device. Pair again from developer options.", e),
                )
            }
            try {
                stream?.close()
            } catch (_: Exception) {
            }
            Result.failure(e)
        }
    }
}
