package org.thebytearray.app.android.openloader.feature.installer.impl.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.thebytearray.app.android.openloader.core.designsystem.icon.OpenLoaderIcons
import org.thebytearray.app.android.openloader.core.model.InstallMode
import org.thebytearray.app.android.openloader.feature.installer.impl.InstallerViewModel
import org.thebytearray.app.android.openloader.feature.installer.impl.R
import org.thebytearray.app.android.openloader.feature.installer.impl.QueueStatus
import org.thebytearray.app.android.openloader.feature.installer.impl.QueuedApk
import rikka.shizuku.Shizuku
import androidx.core.graphics.createBitmap

private const val SHIZUKU_PERM_REQ = 1001

private fun getApkIcon(context: android.content.Context, localPath: String): Drawable? {
    return try {
        val pm = context.packageManager
        val info = pm.getPackageArchiveInfo(localPath, PackageManager.GET_META_DATA)
        info?.applicationInfo?.loadIcon(pm)
    } catch (_: Exception) {
        null
    }
}

private fun getApkLabel(context: android.content.Context, localPath: String): String? {
    return try {
        val pm = context.packageManager
        val info = pm.getPackageArchiveInfo(localPath, PackageManager.GET_META_DATA)
        info?.applicationInfo?.loadLabel(pm)?.toString()
    } catch (_: Exception) {
        null
    }
}

private fun getApkPackageName(context: android.content.Context, localPath: String): String? {
    return try {
        val pm = context.packageManager
        val info = pm.getPackageArchiveInfo(localPath, PackageManager.GET_META_DATA)
        info?.packageName
    } catch (_: Exception) {
        null
    }
}

private fun getInstalledAppDrawable(context: android.content.Context, packageName: String): Drawable? {
    return try {
        context.packageManager.getApplicationIcon(packageName)
    } catch (_: Exception) {
        null
    }
}

private fun resolveQueueIcon(context: android.content.Context, item: QueuedApk): Drawable? {
    if (java.io.File(item.localPath).exists()) {
        return getApkIcon(context, item.localPath)
    }
    val pkg = item.packageName ?: return null
    return getInstalledAppDrawable(context, pkg)
}

private fun resolveQueuePackageName(context: android.content.Context, item: QueuedApk): String? {
    if (item.packageName != null) return item.packageName
    if (java.io.File(item.localPath).exists()) {
        return getApkPackageName(context, item.localPath)
    }
    return null
}

private fun resolveQueueLabel(context: android.content.Context, item: QueuedApk): String {
    val fromFile = if (java.io.File(item.localPath).exists()) {
        getApkLabel(context, item.localPath)
    } else {
        null
    }
    return item.appLabel ?: fromFile ?: item.packageName ?: item.displayName
}

private fun Drawable.toBitmap(): Bitmap {
    val width = if (intrinsicWidth > 0) intrinsicWidth else 48
    val height = if (intrinsicHeight > 0) intrinsicHeight else 48
    val bitmap = createBitmap(width, height)
    val canvas = android.graphics.Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSelectRoute(
    viewModel: InstallerViewModel,
    onShizuku: () -> Unit,
    onAdb: () -> Unit,
    /** When Shizuku is installed and permission is already granted; e.g. pop back to home. */
    onShizukuAlreadyReady: () -> Unit,
) {
    val context = LocalContext.current
    val mode by viewModel.installMode.collectAsStateWithLifecycle(InstallMode.SHIZUKU)
    val shizukuAlreadyGrantedMessage = stringResource(R.string.mode_select_shizuku_already_granted)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "OpenLoader",
                        fontWeight = FontWeight.Bold,
                    )
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "Select install method",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
            )

            Text(
                "Choose how you want to install APKs on this device",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setInstallMode(InstallMode.SHIZUKU) },
                colors = CardDefaults.cardColors(
                    containerColor = if (mode == InstallMode.SHIZUKU)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = OpenLoaderIcons.Android,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Shizuku",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Uses a local privileged service",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    RadioButton(
                        selected = mode == InstallMode.SHIZUKU,
                        onClick = { viewModel.setInstallMode(InstallMode.SHIZUKU) },
                    )
                }
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { viewModel.setInstallMode(InstallMode.WIRELESS_ADB) },
                colors = CardDefaults.cardColors(
                    containerColor = if (mode == InstallMode.WIRELESS_ADB)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
                shape = RoundedCornerShape(16.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f),
                                RoundedCornerShape(12.dp),
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = OpenLoaderIcons.Wifi,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(28.dp),
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Wireless ADB",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "Pair via Wi-Fi (Android 11+)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    RadioButton(
                        selected = mode == InstallMode.WIRELESS_ADB,
                        onClick = { viewModel.setInstallMode(InstallMode.WIRELESS_ADB) },
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    when (mode) {
                        InstallMode.SHIZUKU -> {
                            if (viewModel.isShizukuInstalled() && viewModel.shizukuPermissionGranted()) {
                                Toast.makeText(
                                    context,
                                    shizukuAlreadyGrantedMessage,
                                    Toast.LENGTH_SHORT,
                                ).show()
                                onShizukuAlreadyReady()
                            } else {
                                onShizuku()
                            }
                        }
                        InstallMode.WIRELESS_ADB -> onAdb()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continue")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShizukuSetupRoute(
    viewModel: InstallerViewModel,
    onContinue: () -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var granted by remember { mutableStateOf(viewModel.shizukuPermissionGranted()) }
    val latestGranted by rememberUpdatedState(granted)
    val permissionOk = latestGranted || viewModel.shizukuPermissionGranted()

    DisposableEffect(Unit) {
        val listener =
            Shizuku.OnRequestPermissionResultListener { _, result ->
                granted = result == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        Shizuku.addRequestPermissionResultListener(listener)
        onDispose { Shizuku.removeRequestPermissionResultListener(listener) }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                granted = viewModel.shizukuPermissionGranted()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        granted = viewModel.shizukuPermissionGranted()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Shizuku") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(OpenLoaderIcons.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!viewModel.isShizukuInstalled()) {
                Text("Install Shizuku, start it with wireless debugging or root, then return here.")
                Button(
                    onClick = {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://shizuku.rikka.app/download/".toUri(),
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Open Shizuku download page")
                }
            } else {
                Text("Shizuku is installed. Grant this app permission when prompted.")
                if (!permissionOk) {
                    Button(
                        onClick = {
                            if (Shizuku.isPreV11()) {
                                return@Button
                            }
                            if (!Shizuku.pingBinder()) {
                                Toast.makeText(
                                    context,
                                    "Start Shizuku and wait until it is running, then try again.",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                return@Button
                            }
                            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                                granted = true
                            } else {
                                Shizuku.requestPermission(SHIZUKU_PERM_REQ)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Request Shizuku permission")
                    }
                }
                Text(
                    if (permissionOk) "Permission granted. You can continue."
                    else "Permission not granted yet.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Button(
                onClick = onContinue,
                enabled = viewModel.isShizukuInstalled() && permissionOk,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continue to install")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdbSetupRoute(
    viewModel: InstallerViewModel,
    onContinue: () -> Unit,
    onBack: () -> Unit,
    onStartNotificationPairing: (() -> Unit)? = null,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val adbHint by viewModel.adbMessage.collectAsStateWithLifecycle(null)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.refreshAdbStatus()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshAdbStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val canUseWireless = Build.VERSION.SDK_INT >= Build.VERSION_CODES.R

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wireless ADB") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(OpenLoaderIcons.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!canUseWireless) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        "Wireless debugging requires Android 11 (API 30) or newer.",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Connection Status",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        adbHint ?: "Checking...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (canUseWireless) {
                        Text(
                            "Pairing registers this device; the connection uses a separate port. " +
                                "After pairing, return to this screen or tap Test Connection. " +
                                "Accept the wireless debugging authorization on the device if prompted.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (onStartNotificationPairing != null && canUseWireless) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Icon(
                            imageVector = OpenLoaderIcons.Notifications,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Quick Pair",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "1. Enable Wireless Debugging in Developer Options\n" +
                            "2. Tap Pair with Code\n" +
                            "3. Tap the button below\n" +
                            "4. Enter only the pairing code in the notification",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Button(
                            onClick = onStartNotificationPairing,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = OpenLoaderIcons.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Start Notification Pairing")
                        }
                    }
                }
            }

            Button(
                onClick = {
                    scope.launch {
                        viewModel.testAdb()
                        viewModel.refreshAdbStatus()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = canUseWireless
            ) {
                Text("Test Connection")
            }

            Button(
                onClick = onContinue,
                modifier = Modifier.fillMaxWidth(),
                enabled = canUseWireless
            ) {
                Text("Continue to install")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallQueueRoute(
    viewModel: InstallerViewModel,
    onBack: () -> Unit,
) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val installing by viewModel.installing.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addStagedUris(uris)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Install queue")
                        if (queue.isNotEmpty()) {
                            Text(
                                "${queue.size} APK${if (queue.size > 1) "s" else ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(OpenLoaderIcons.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (queue.isNotEmpty() && !installing) {
                        TextButton(onClick = { viewModel.clearQueue() }) {
                            Text("Clear all")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { pickLauncher.launch(arrayOf("application/vnd.android.package-archive")) },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ) {
                Icon(
                    imageVector = OpenLoaderIcons.FileDownload,
                    contentDescription = "Add APK files",
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            if (installing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                )
            }

            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Icon(
                            imageVector = OpenLoaderIcons.Android,
                            contentDescription = null,
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        )
                        Text(
                            "No APKs selected",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Tap + to add APK files",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(
                            items = queue,
                            key = { it.localPath },
                        ) { item ->
                            ApkQueueItem(
                                item = item,
                                onRemove = {
                                    viewModel.removeFromQueue(item.localPath)
                                },
                            )
                        }
                    }

                    if (installing) {
                        OutlinedButton(
                            onClick = { viewModel.cancelInstallation() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                        ) {
                            Icon(
                                imageVector = OpenLoaderIcons.Close,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.cancel_installation))
                        }
                    } else {
                        Button(
                            onClick = { viewModel.runInstallQueue() },
                            enabled = queue.any { it.status == QueueStatus.Pending },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                        ) {
                            Text(
                                "Install ${queue.count { it.status == QueueStatus.Pending }} APK" +
                                    if (queue.count { it.status == QueueStatus.Pending } > 1) "s" else "",
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApkQueueItem(
    item: QueuedApk,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    val icon = remember(item.localPath, item.packageName, item.appLabel, item.status) {
        resolveQueueIcon(context, item)
    }
    val packageName = remember(item.localPath, item.packageName) {
        resolveQueuePackageName(context, item)
    }
    val label = remember(item.localPath, item.packageName, item.appLabel, item.displayName) {
        resolveQueueLabel(context, item)
    }

    val dismissState = rememberSwipeToDismissBoxState()

    LaunchedEffect(dismissState) {
        snapshotFlow { dismissState.currentValue }
            .distinctUntilChanged()
            .collect { value ->
                if (value == SwipeToDismissBoxValue.EndToStart || value == SwipeToDismissBoxValue.StartToEnd) {
                    onRemove()
                }
            }
    }

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    SwipeToDismissBoxValue.EndToStart, SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.errorContainer
                    else -> Color.Transparent
                },
                label = "dismiss-bg",
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = OpenLoaderIcons.Close,
                    contentDescription = "Remove",
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        },
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    if (icon != null) {
                        ImageBitmap(
                            bitmap = icon.toBitmap(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                        )
                    } else {
                        Icon(
                            imageVector = OpenLoaderIcons.Android,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = packageName?.takeIf { it != label } ?: item.displayName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                StatusBadge(status = item.status, message = item.message)
            }
        }
    }
}

@Composable
private fun StatusBadge(
    status: QueueStatus,
    message: String?,
) {
    val (icon, color, text) = when (status) {
        QueueStatus.Pending -> Triple(
            OpenLoaderIcons.Pending,
            MaterialTheme.colorScheme.tertiary,
            "Pending",
        )

        QueueStatus.Installing -> Triple(
            null,
            MaterialTheme.colorScheme.primary,
            "Installing",
        )

        QueueStatus.Success -> Triple(
            OpenLoaderIcons.CheckCircle,
            MaterialTheme.colorScheme.primary,
            "Success",
        )

        QueueStatus.Failed -> Triple(
            OpenLoaderIcons.Error,
            MaterialTheme.colorScheme.error,
            message ?: "Failed",
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .background(
                color.copy(alpha = 0.1f),
                RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        if (status == QueueStatus.Installing) {
            CircularProgressIndicator(
                modifier = Modifier.size(12.dp),
                strokeWidth = 2.dp,
                color = color,
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = color,
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            maxLines = 1,
        )
    }
}

@Composable
private fun ImageBitmap(
    bitmap: Bitmap,
    contentDescription: String?,
    modifier: Modifier = Modifier,
) {
    Image(
        bitmap = bitmap.asImageBitmap(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = ContentScale.Crop,
    )
}