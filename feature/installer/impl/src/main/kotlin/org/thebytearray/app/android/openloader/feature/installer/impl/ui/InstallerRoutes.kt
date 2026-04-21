@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

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
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.thebytearray.app.android.openloader.core.designsystem.component.OlFilledTonalIconTextButton
import org.thebytearray.app.android.openloader.core.designsystem.component.OlIconTextButton
import org.thebytearray.app.android.openloader.core.designsystem.component.OlOutlinedIconTextButton
import org.thebytearray.app.android.openloader.core.designsystem.component.OlTopAppBar
import org.thebytearray.app.android.openloader.core.designsystem.icon.OpenLoaderIcons
import org.thebytearray.app.android.openloader.core.designsystem.theme.Dimens
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

@Composable
private fun SelectableModeCard(
    title: String,
    description: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    badgeContainerColor: Color,
    badgeContentColor: Color,
    onClick: () -> Unit,
) {
    val shape = MaterialTheme.shapes.extraLarge
    val container = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceContainer
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape = shape,
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = container),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(Dimens.paddingLarge),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(Dimens.iconBadgeSize)
                    .background(color = badgeContainerColor, shape = CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = badgeContentColor,
                    modifier = Modifier.size(Dimens.iconSizeLarge),
                )
            }
            Spacer(modifier = Modifier.width(Dimens.paddingLarge))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            RadioButton(
                selected = selected,
                onClick = onClick,
            )
        }
    }
}

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
            OlTopAppBar(title = "OpenLoader")
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = Dimens.paddingLarge)
                .padding(bottom = Dimens.paddingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimens.paddingMedium),
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

            Spacer(modifier = Modifier.height(Dimens.paddingSmall))

            SelectableModeCard(
                title = "Shizuku",
                description = "Uses a local privileged service",
                icon = OpenLoaderIcons.Android,
                selected = mode == InstallMode.SHIZUKU,
                badgeContainerColor = MaterialTheme.colorScheme.primary,
                badgeContentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = { viewModel.setInstallMode(InstallMode.SHIZUKU) },
            )

            SelectableModeCard(
                title = "Wireless ADB",
                description = "Pair via Wi-Fi (Android 11+)",
                icon = OpenLoaderIcons.Wifi,
                selected = mode == InstallMode.WIRELESS_ADB,
                badgeContainerColor = MaterialTheme.colorScheme.tertiary,
                badgeContentColor = MaterialTheme.colorScheme.onTertiary,
                onClick = { viewModel.setInstallMode(InstallMode.WIRELESS_ADB) },
            )

            Spacer(modifier = Modifier.weight(1f))

            OlIconTextButton(
                text = "Continue",
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
            )
        }
    }
}

@Composable
private fun InfoCard(
    modifier: Modifier = Modifier,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val shape = MaterialTheme.shapes.extraLarge
    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                shape = shape,
            ),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Column(
            modifier = Modifier.padding(Dimens.paddingLarge),
            verticalArrangement = Arrangement.spacedBy(Dimens.paddingSmall),
            content = content,
        )
    }
}

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
            OlTopAppBar(
                title = "Shizuku",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(OpenLoaderIcons.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = Dimens.paddingLarge)
                .padding(bottom = Dimens.paddingLarge)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.paddingMedium),
        ) {
            if (!viewModel.isShizukuInstalled()) {
                InfoCard {
                    Text(
                        text = "Shizuku not installed",
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )
                    Text(
                        text = "Install Shizuku, start it with wireless debugging or root, then return here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OlIconTextButton(
                        text = "Open Shizuku download page",
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "https://shizuku.rikka.app/download/".toUri(),
                                ),
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = Dimens.paddingSmall),
                    )
                }
            } else {
                InfoCard {
                    Text(
                        text = "Shizuku is installed",
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )
                    Text(
                        text = if (permissionOk) "Permission granted. You can continue."
                        else "Grant this app permission when prompted.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (!permissionOk) {
                        OlIconTextButton(
                            text = "Request Shizuku permission",
                            onClick = {
                                if (Shizuku.isPreV11()) {
                                    return@OlIconTextButton
                                }
                                if (!Shizuku.pingBinder()) {
                                    Toast.makeText(
                                        context,
                                        "Start Shizuku and wait until it is running, then try again.",
                                        Toast.LENGTH_SHORT,
                                    ).show()
                                    return@OlIconTextButton
                                }
                                if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                                    granted = true
                                } else {
                                    Shizuku.requestPermission(SHIZUKU_PERM_REQ)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = Dimens.paddingSmall),
                        )
                    }
                }
            }
            OlIconTextButton(
                text = "Continue to install",
                onClick = onContinue,
                enabled = viewModel.isShizukuInstalled() && permissionOk,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

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
            OlTopAppBar(
                title = "Wireless ADB",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(OpenLoaderIcons.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = Dimens.paddingLarge)
                .padding(bottom = Dimens.paddingLarge)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.paddingMedium),
        ) {
            if (!canUseWireless) {
                InfoCard(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                    Text(
                        text = "Unsupported Android version",
                        style = MaterialTheme.typography.titleLargeEmphasized,
                    )
                    Text(
                        text = "Wireless debugging requires Android 11 (API 30) or newer.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            InfoCard {
                Text(
                    text = "Connection Status",
                    style = MaterialTheme.typography.titleLargeEmphasized,
                )
                Text(
                    text = adbHint ?: "Checking...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (canUseWireless) {
                    Text(
                        text = "Pairing registers this device; the connection uses a separate port. " +
                            "After pairing, return to this screen or tap Test Connection. " +
                            "Accept the wireless debugging authorization on the device if prompted.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (onStartNotificationPairing != null && canUseWireless) {
                val shape = MaterialTheme.shapes.extraLarge
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    shape = shape,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.paddingLarge),
                        verticalArrangement = Arrangement.spacedBy(Dimens.paddingMedium),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = CircleShape,
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = OpenLoaderIcons.Notifications,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimary,
                            )
                        }
                        Text(
                            text = "Quick Pair",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "1. Enable Wireless Debugging in Developer Options\n" +
                                "2. Tap Pair with Code\n" +
                                "3. Tap the button below\n" +
                                "4. Enter only the pairing code in the notification",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f),
                        )
                        OlIconTextButton(
                            text = "Start Notification Pairing",
                            icon = OpenLoaderIcons.Notifications,
                            onClick = onStartNotificationPairing,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }

            OlFilledTonalIconTextButton(
                text = "Test Connection",
                onClick = {
                    scope.launch {
                        viewModel.testAdb()
                        viewModel.refreshAdbStatus()
                    }
                },
                enabled = canUseWireless,
                modifier = Modifier.fillMaxWidth(),
            )

            OlIconTextButton(
                text = "Continue to install",
                onClick = onContinue,
                enabled = canUseWireless,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

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
            val result = viewModel.addStagedUris(uris)
            if (result.skippedDuplicates > 0) {
                val msg = if (result.skippedDuplicates == 1) {
                    context.getString(R.string.home_duplicate_skipped, 1)
                } else {
                    context.getString(
                        R.string.home_duplicate_skipped_plural,
                        result.skippedDuplicates,
                    )
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
        }
    }

    Scaffold(
        topBar = {
            OlTopAppBar(
                title = "Install queue",
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
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(
                    imageVector = OpenLoaderIcons.FileDownload,
                    contentDescription = "Add APK files",
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (installing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.paddingLarge)
                        .height(4.dp),
                )
            }

            if (queue.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(Dimens.paddingHuge),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(Dimens.paddingLarge),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(96.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    shape = CircleShape,
                                ),
                        ) {
                            Icon(
                                imageVector = OpenLoaderIcons.Android,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(48.dp),
                            )
                        }
                        Text(
                            "No APKs selected",
                            style = MaterialTheme.typography.titleLargeEmphasized,
                        )
                        Text(
                            "Tap + to add APK files",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = Dimens.paddingLarge),
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = Dimens.paddingSmall),
                        verticalArrangement = Arrangement.spacedBy(Dimens.paddingSmall),
                        modifier = Modifier.weight(1f),
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
                        OlOutlinedIconTextButton(
                            text = stringResource(R.string.cancel_installation),
                            icon = OpenLoaderIcons.Close,
                            onClick = { viewModel.cancelInstallation() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimens.paddingLarge),
                        )
                    } else {
                        val pendingCount = queue.count { it.status == QueueStatus.Pending }
                        OlIconTextButton(
                            text = "Install $pendingCount APK${if (pendingCount > 1) "s" else ""}",
                            onClick = { viewModel.runInstallQueue() },
                            enabled = pendingCount > 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = Dimens.paddingLarge),
                        )
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

    val shape = MaterialTheme.shapes.extraLarge

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
                    .clip(shape)
                    .background(color)
                    .padding(horizontal = Dimens.paddingLarge),
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
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                    shape = shape,
                ),
            shape = shape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
            ),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(Dimens.paddingLarge),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
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

                Spacer(modifier = Modifier.width(Dimens.paddingMedium))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium,
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

                Spacer(modifier = Modifier.width(Dimens.paddingSmall))

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
                color.copy(alpha = 0.12f),
                RoundedCornerShape(Dimens.paddingLarge),
            )
            .padding(horizontal = Dimens.paddingSmall, vertical = 4.dp),
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
            style = MaterialTheme.typography.labelMedium,
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
