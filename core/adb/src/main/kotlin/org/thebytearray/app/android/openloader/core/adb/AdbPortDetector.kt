package org.thebytearray.app.android.openloader.core.adb

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.ServerSocket


@RequiresApi(Build.VERSION_CODES.R)
class AdbPortDetector(private val context: Context) {

    private val nsdManager: NsdManager = context.getSystemService(NsdManager::class.java)


    fun detectConnectionPort(): Flow<Int> = callbackFlow {
        var registered = false
        var serviceName: String? = null

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "Connection port discovery started: $serviceType")
                registered = true
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Connection port discovery failed to start: $serviceType, error=$errorCode")
                trySend(-1)
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Connection port discovery stopped: $serviceType")
                registered = false
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(TAG, "Connection port discovery failed to stop: $serviceType, error=$errorCode")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Connection service found: ${serviceInfo.serviceName}")
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(nsdServiceInfo: NsdServiceInfo, errorCode: Int) {
                        Log.w(TAG, "Failed to resolve connection service: ${nsdServiceInfo.serviceName}, error=$errorCode")
                    }

                    override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                        val port = resolvedService.port
                        Log.d(TAG, "Connection service resolved: ${resolvedService.serviceName}, port=$port")

                        // Verify it's a local service and port is in use (by ADB)
                        if (isLocalService(resolvedService) && isPortInUse(port)) {
                            Log.d(TAG, "Connection port detected: $port")
                            serviceName = resolvedService.serviceName
                            trySend(port)
                        } else {
                            Log.d(TAG, "Connection service rejected: port=$port, isLocal=${isLocalService(resolvedService)}, inUse=${isPortInUse(port)}")
                        }
                    }
                })
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Connection service lost: ${serviceInfo.serviceName}")
                if (serviceInfo.serviceName == serviceName) {
                    serviceName = null
                    trySend(-1)
                }
            }
        }

        nsdManager.discoverServices(TLS_CONNECT, NsdManager.PROTOCOL_DNS_SD, listener)

        awaitClose {
            if (registered) {
                try {
                    nsdManager.stopServiceDiscovery(listener)
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping connection discovery", e)
                }
            }
        }
    }

    fun detectPairingPort(): Flow<Int> = callbackFlow {
        var registered = false
        var serviceName: String? = null

        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "Pairing port discovery started: $serviceType")
                registered = true
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Pairing port discovery failed to start: $serviceType, error=$errorCode")
                trySend(-1)
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d(TAG, "Pairing port discovery stopped: $serviceType")
                registered = false
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.w(TAG, "Pairing port discovery failed to stop: $serviceType, error=$errorCode")
            }

            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Pairing service found: ${serviceInfo.serviceName}")
                nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(nsdServiceInfo: NsdServiceInfo, errorCode: Int) {
                        Log.w(TAG, "Failed to resolve pairing service: ${nsdServiceInfo.serviceName}, error=$errorCode")
                    }

                    override fun onServiceResolved(resolvedService: NsdServiceInfo) {
                        val port = resolvedService.port
                        Log.d(TAG, "Pairing service resolved: ${resolvedService.serviceName}, port=$port")

                        // Verify it's a local service and port is in use (by ADB)
                        if (isLocalService(resolvedService) && isPortInUse(port)) {
                            Log.d(TAG, "Pairing port detected: $port")
                            serviceName = resolvedService.serviceName
                            trySend(port)
                        } else {
                            Log.d(TAG, "Pairing service rejected: port=$port, isLocal=${isLocalService(resolvedService)}, inUse=${isPortInUse(port)}")
                        }
                    }
                })
            }

            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                Log.d(TAG, "Pairing service lost: ${serviceInfo.serviceName}")
                if (serviceInfo.serviceName == serviceName) {
                    serviceName = null
                    trySend(-1)
                }
            }
        }

        nsdManager.discoverServices(TLS_PAIRING, NsdManager.PROTOCOL_DNS_SD, listener)

        awaitClose {
            if (registered) {
                try {
                    nsdManager.stopServiceDiscovery(listener)
                } catch (e: Exception) {
                    Log.w(TAG, "Error stopping pairing discovery", e)
                }
            }
        }
    }


    private fun isLocalService(resolvedService: NsdServiceInfo): Boolean {
        val addr = resolvedService.host?.hostAddress ?: return false
        if (addr == "127.0.0.1" || addr == "::1" || addr == "0:0:0:0:0:0:0:1" || addr.startsWith("127.")) {
            return true
        }
        return try {
            NetworkInterface.getNetworkInterfaces().asSequence().any { networkInterface ->
                networkInterface.inetAddresses.asSequence().any { inetAddress ->
                    inetAddress.hostAddress == addr
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error checking if service is local", e)
            false
        }
    }


    private fun isPortInUse(port: Int): Boolean = try {
        ServerSocket().use {
            it.bind(InetSocketAddress("127.0.0.1", port), 1)
            Log.d(TAG, "Port $port is available (not in use)")
            false
        }
    } catch (e: IOException) {
        Log.d(TAG, "Port $port is in use")
        true
    }

    companion object {
        private const val TAG = "AdbPortDetector"
        private const val TLS_CONNECT = "_adb-tls-connect._tcp"
        private const val TLS_PAIRING = "_adb-tls-pairing._tcp"


        fun isSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R


        suspend fun awaitPairingPort(context: Context, timeoutMs: Long = 20_000L): Int? {
            if (!isSupported()) return null
            return withTimeoutOrNull(timeoutMs) {
                AdbPortDetector(context).detectPairingPort().first { it > 0 }
            }
        }
    }
}