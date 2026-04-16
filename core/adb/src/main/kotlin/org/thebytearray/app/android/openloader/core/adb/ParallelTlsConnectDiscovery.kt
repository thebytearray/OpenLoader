package org.thebytearray.app.android.openloader.core.adb

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import java.net.NetworkInterface

/**
 * Caches `_adb-tls-connect._tcp` host → port while pairing runs (same idea as
 * aShellYou [WifiAdbRepositoryImpl.startParallelConnectDiscovery]).
 */
internal class ParallelTlsConnectDiscovery(private val context: Context) {

    private val lock = Any()
    private val cached = mutableMapOf<String, Int>()
    private var nsdManager: NsdManager? = null
    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun start() {
        stop()
        val mgr = context.getSystemService(Context.NSD_SERVICE) as? NsdManager ?: return
        nsdManager = mgr
        val listener = object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                Log.d(TAG, "Parallel connect discovery started: $serviceType")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e(TAG, "Parallel connect discovery start failed: $errorCode")
            }

            override fun onDiscoveryStopped(serviceType: String) {}

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}

            override fun onServiceFound(info: NsdServiceInfo) {
                @Suppress("DEPRECATION")
                mgr.resolveService(info, object : NsdManager.ResolveListener {
                    override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                        Log.w(TAG, "Parallel: resolve failed $errorCode")
                    }

                    override fun onServiceResolved(resolved: NsdServiceInfo) {
                        val ip = resolved.host?.hostAddress ?: return
                        val port = resolved.port
                        Log.d(TAG, "Parallel: cached connect $ip -> $port")
                        synchronized(lock) { cached[ip] = port }
                    }
                })
            }

            override fun onServiceLost(info: NsdServiceInfo) {}
        }
        discoveryListener = listener
        try {
            mgr.discoverServices(TLS_CONNECT, NsdManager.PROTOCOL_DNS_SD, listener)
        } catch (e: Exception) {
            Log.e(TAG, "Parallel connect discovery error", e)
        }
    }

    fun stop() {
        val mgr = nsdManager
        val listener = discoveryListener
        discoveryListener = null
        nsdManager = null
        if (mgr != null && listener != null) {
            try {
                mgr.stopServiceDiscovery(listener)
            } catch (e: Exception) {
                Log.w(TAG, "stop parallel connect discovery", e)
            }
        }
    }

    fun getCachedPortForHost(host: String): Int? = synchronized(lock) { cached[host] }

    /**
     * Wireless debugging may advertise the device LAN IP instead of 127.0.0.1.
     */
    fun getAnyCachedLocalConnectPort(): Int? = synchronized(lock) {
        cached.entries.firstOrNull { (ip, _) -> isLocalIpAddress(ip) }?.value
    }

    fun clearCache() {
        synchronized(lock) { cached.clear() }
    }

    private fun isLocalIpAddress(addr: String): Boolean {
        if (addr == "127.0.0.1" || addr == "::1" || addr == "0:0:0:0:0:0:0:1" || addr.startsWith("127.")) {
            return true
        }
        return try {
            NetworkInterface.getNetworkInterfaces().asSequence().any { ni ->
                ni.inetAddresses.asSequence().any { it.hostAddress == addr }
            }
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val TAG = "ParallelTlsConnect"
        private const val TLS_CONNECT = "_adb-tls-connect._tcp"
    }
}
