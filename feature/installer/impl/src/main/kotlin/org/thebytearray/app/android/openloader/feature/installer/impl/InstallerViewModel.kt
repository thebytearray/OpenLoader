package org.thebytearray.app.android.openloader.feature.installer.impl

import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.thebytearray.app.android.openloader.core.adb.AdbConnectionRepository
import org.thebytearray.app.android.openloader.core.adb.WirelessAdbClient
import org.thebytearray.app.android.openloader.core.domain.di.AdbBackend
import org.thebytearray.app.android.openloader.core.domain.di.ShizukuBackend
import org.thebytearray.app.android.openloader.core.domain.install.InstallBackend
import org.thebytearray.app.android.openloader.core.model.InstallMode
import org.thebytearray.app.android.openloader.core.adb.ApkArchiveMetadata
import org.thebytearray.app.android.openloader.core.datastore.history.InstallHistoryRepository
import org.thebytearray.app.android.openloader.feature.installer.impl.data.ApkStaging
import org.thebytearray.app.android.openloader.feature.installer.impl.data.InstallModeRepository
import android.content.Context
import rikka.shizuku.Shizuku
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

data class QueuedApk(
    val displayName: String,
    val localPath: String,
    val packageName: String? = null,
    val appLabel: String? = null,
    val status: QueueStatus = QueueStatus.Pending,
    val message: String? = null,
)

enum class QueueStatus { Pending, Installing, Success, Failed }

@HiltViewModel
class InstallerViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val installModeRepository: InstallModeRepository,
    private val installHistoryRepository: InstallHistoryRepository,
    private val apkStaging: ApkStaging,
    @param:ShizukuBackend private val shizukuBackend: InstallBackend,
    @param:AdbBackend private val adbBackend: InstallBackend,
) : ViewModel() {

    val installMode = installModeRepository.installMode.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        InstallMode.SHIZUKU,
    )

    val wirelessAdbConfigured: StateFlow<Boolean> = installModeRepository.wirelessAdbConfigured.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        false,
    )

    /**
     * True when TLS connect host/port exist in the ADB keys DataStore (pairing completed successfully).
     * Used so UI recomputes when pairing writes keys even if user-pref "configured" was not set yet.
     */
    val adbWirelessKeysReady: StateFlow<Boolean> = combine(
        AdbConnectionRepository.getInstance(context).isPaired,
        AdbConnectionRepository.getInstance(context).lastPort,
    ) { paired, port -> paired && port != null }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    private val _queue = MutableStateFlow<List<QueuedApk>>(emptyList())
    val queue: StateFlow<List<QueuedApk>> = _queue.asStateFlow()

    private val _installing = MutableStateFlow(false)
    val installing: StateFlow<Boolean> = _installing.asStateFlow()

    private val _adbMessage = MutableStateFlow<String?>(null)
    val adbMessage: StateFlow<String?> = _adbMessage.asStateFlow()

    val installHistory = installHistoryRepository.history.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList(),
    )

    private var installJob: kotlinx.coroutines.Job? = null
    private val installCanceledByUser = AtomicBoolean(false)

    fun setInstallMode(mode: InstallMode) {
        viewModelScope.launch {
            installModeRepository.setInstallMode(mode)
        }
    }

    fun refreshAdbStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            _adbMessage.value = null
            val status = WirelessAdbClient.getConnectionStatusAsync(context, forceCheck = true, quickCheck = true)
            if (status == WirelessAdbClient.ConnectionStatus.CONNECTED) {
                installModeRepository.setWirelessAdbConfigured(true)
            }
            val hint = when (status) {
                WirelessAdbClient.ConnectionStatus.CONNECTED -> "ADB connected."
                WirelessAdbClient.ConnectionStatus.NEEDS_PAIRING -> "Pair or enable wireless debugging."
                WirelessAdbClient.ConnectionStatus.NOT_CONNECTED -> "Not connected."
                WirelessAdbClient.ConnectionStatus.ERROR -> "Error checking ADB."
            }
            withContext(Dispatchers.Main.immediate) {
                _adbMessage.value = hint
            }
        }
    }

    fun markWirelessAdbSetupFinished() {
        viewModelScope.launch {
            installModeRepository.setWirelessAdbConfigured(true)
        }
    }


    suspend fun isInstallMethodReady(forceAdbProbe: Boolean = false): Boolean = when (installMode.value) {
        InstallMode.SHIZUKU -> isShizukuInstalled() && shizukuPermissionGranted()
        InstallMode.WIRELESS_ADB -> {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                false
            } else {
                withContext(Dispatchers.IO) {
                    WirelessAdbClient.getConnectionStatusAsync(
                        context,
                        forceCheck = forceAdbProbe,
                        quickCheck = true,
                    ) == WirelessAdbClient.ConnectionStatus.CONNECTED
                }
            }
        }
    }

    suspend fun pairAdb(pairingCode: String, port: Int): Result<Unit> {
        val parts = pairingCode.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val code = parts.getOrNull(0) ?: return Result.failure(IllegalArgumentException("Invalid pairing code"))
        val pairingPort = parts.getOrNull(1)?.toIntOrNull() ?: port
        val result = WirelessAdbClient.pair(context, code, pairingPort)
        if (result.isSuccess) {
            installModeRepository.setWirelessAdbConfigured(true)
        }
        return result.map { }
    }

    suspend fun testAdb(): Result<Boolean> {
        val result = WirelessAdbClient.testConnection(context)
        if (result.isSuccess) {
            installModeRepository.setWirelessAdbConfigured(true)
        }
        return result
    }

    fun isShizukuInstalled(): Boolean = try {
        context.packageManager.getPackageInfo(SHIZUKU_PACKAGE, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    fun shizukuPermissionGranted(): Boolean =
        Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED

    /**
     * Result of staging a batch of URIs. [added] is the count actually appended
     * to the queue; [skippedDuplicates] is the count ignored because another
     * queued entry already has the same [QueuedApk.packageName].
     */
    data class AddStagedResult(val added: Int, val skippedDuplicates: Int)

    fun addStagedUris(uris: List<android.net.Uri>): AddStagedResult {
        val existingPackages = _queue.value.mapNotNull { it.packageName }.toHashSet()
        val seenInBatch = hashSetOf<String>()
        val newItems = mutableListOf<QueuedApk>()
        var skipped = 0

        uris.forEach { uri ->
            val file = apkStaging.stageUri(uri)
            val name = uri.lastPathSegment ?: file.name
            val (pkg, label) = ApkArchiveMetadata.read(context, file.absolutePath)

            val isDuplicate = pkg != null && (pkg in existingPackages || pkg in seenInBatch)
            if (isDuplicate) {
                java.io.File(file.absolutePath).delete()
                skipped++
                return@forEach
            }
            if (pkg != null) seenInBatch += pkg

            newItems += QueuedApk(
                displayName = name,
                localPath = file.absolutePath,
                packageName = pkg,
                appLabel = label,
            )
        }
        if (newItems.isNotEmpty()) {
            _queue.value += newItems
        }
        return AddStagedResult(added = newItems.size, skippedDuplicates = skipped)
    }

    fun clearQueue() {
        _queue.value.forEach {
            java.io.File(it.localPath).delete()
        }
        _queue.value = emptyList()
    }

    fun removeFromQueue(localPath: String) {
        val item = _queue.value.find { it.localPath == localPath }
        if (item != null) {
            if (item.status != QueueStatus.Installing) {
                java.io.File(localPath).delete()
            }
            _queue.value = _queue.value.filter { it.localPath != localPath }
        }
    }

    fun canRetryItem(item: QueuedApk): Boolean =
        item.status == QueueStatus.Failed && java.io.File(item.localPath).exists()

    fun retryItem(localPath: String) {
        val f = java.io.File(localPath)
        _queue.value = _queue.value.map { item ->
            if (item.localPath == localPath && item.status == QueueStatus.Failed && f.exists()) {
                item.copy(status = QueueStatus.Pending, message = null)
            } else {
                item
            }
        }
    }

    fun retryAllStagedFailed() {
        _queue.value = _queue.value.map { item ->
            if (item.status == QueueStatus.Failed && java.io.File(item.localPath).exists()) {
                item.copy(status = QueueStatus.Pending, message = null)
            } else {
                item
            }
        }
    }

    fun cancelInstallation() {
        installCanceledByUser.set(true)
        markInstallingItemsCanceled()
        installJob?.cancel()
        _installing.value = false
    }

    private fun markInstallingItemsCanceled() {
        val canceled = context.getString(R.string.install_canceled)
        _queue.value = _queue.value.map { item ->
            if (item.status == QueueStatus.Installing) {
                item.copy(status = QueueStatus.Failed, message = canceled)
            } else {
                item
            }
        }
    }

    fun runInstallQueue() {
        if (installJob?.isActive == true) return
        installJob = viewModelScope.launch {
            try {
                if (!isInstallMethodReady()) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            context,
                            context.getString(R.string.install_method_not_ready),
                            Toast.LENGTH_LONG,
                        ).show()
                    }
                    return@launch
                }
                _installing.value = true
                try {
                    val mode = installMode.first()
                    val backend = backendFor(mode)
                    val current = _queue.value.toMutableList()
                    for (i in current.indices) {
                        if (!isActive) break

                        val item = current[i]

                        if (!_queue.value.any { it.localPath == item.localPath }) {
                            continue
                        }

                        val file = java.io.File(item.localPath)
                        if (!file.exists()) {
                            current[i] = item.copy(
                                status = QueueStatus.Failed,
                                message = "File not found (may have been removed)",
                            )
                            _queue.value = current.toList()
                            continue
                        }

                        val parsed = ApkArchiveMetadata.read(context, item.localPath)
                        val packageName = item.packageName ?: parsed.first
                        val appLabel = item.appLabel ?: parsed.second
                        val enriched = item.copy(packageName = packageName, appLabel = appLabel)

                        current[i] = enriched.copy(status = QueueStatus.Installing)
                        _queue.value = current.toList()

                        installCanceledByUser.set(false)
                        ensureActive()
                        val result = backend.installStagedFile(file)

                        if (!isActive) {
                            markInstallingItemsCanceled()
                            break
                        }

                        if (!_queue.value.any { it.localPath == item.localPath }) {
                            continue
                        }

                        val canceledStr = context.getString(R.string.install_canceled)
                        val prior = _queue.value.find { it.localPath == item.localPath } ?: continue
                        val queueSaysCanceled =
                            prior.status == QueueStatus.Failed && prior.message == canceledStr
                        val userCanceled = installCanceledByUser.get() || queueSaysCanceled
                        if (userCanceled) {
                            installCanceledByUser.set(false)
                            val osInstalledAnyway =
                                result.isSuccess &&
                                    !packageName.isNullOrBlank() &&
                                    isPackageInstalled(packageName)
                            val msg = if (osInstalledAnyway) {
                                context.getString(R.string.install_canceled_but_installed)
                            } else {
                                canceledStr
                            }
                            val done = enriched.copy(
                                status = QueueStatus.Failed,
                                message = msg,
                                packageName = packageName,
                                appLabel = appLabel,
                            )
                            current[i] = done
                            _queue.value = current.toList()
                            installHistoryRepository.append(
                                packageName = packageName.orEmpty(),
                                appLabel = appLabel ?: enriched.displayName,
                                success = false,
                                resultMessage = msg,
                            )
                            if (osInstalledAnyway) {
                                file.delete()
                            }
                            continue
                        }

                        val msg = result.exceptionOrNull()?.message ?: result.getOrNull()
                        val done = enriched.copy(
                            status = if (result.isSuccess) QueueStatus.Success else QueueStatus.Failed,
                            message = msg,
                            packageName = packageName,
                            appLabel = appLabel,
                        )
                        current[i] = done
                        _queue.value = current.toList()
                        installHistoryRepository.append(
                            packageName = packageName.orEmpty(),
                            appLabel = appLabel ?: enriched.displayName,
                            success = result.isSuccess,
                            resultMessage = msg,
                        )
                        if (result.isSuccess) {
                            file.delete()
                        }
                    }
                } catch (e: CancellationException) {
                    installCanceledByUser.set(false)
                    markInstallingItemsCanceled()
                    throw e
                }
            } finally {
                _installing.value = false
                installJob = null
            }
        }
    }

    fun clearInstallHistory() {
        viewModelScope.launch {
            installHistoryRepository.clearAll()
        }
    }

    private fun backendFor(mode: InstallMode): InstallBackend = when (mode) {
        InstallMode.SHIZUKU -> shizukuBackend
        InstallMode.WIRELESS_ADB -> adbBackend
    }

    private fun isPackageInstalled(packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }

    companion object {
        const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    }
}
