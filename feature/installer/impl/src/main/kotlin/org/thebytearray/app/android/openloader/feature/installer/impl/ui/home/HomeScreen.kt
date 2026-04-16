package org.thebytearray.app.android.openloader.feature.installer.impl.ui.home

import android.content.pm.PackageManager
import android.widget.Toast
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import androidx.core.graphics.createBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import org.thebytearray.app.android.openloader.core.designsystem.icon.OpenLoaderIcons
import org.thebytearray.app.android.openloader.feature.installer.impl.R
import org.thebytearray.app.android.openloader.feature.installer.impl.InstallerViewModel
import org.thebytearray.app.android.openloader.feature.installer.impl.QueueStatus
import org.thebytearray.app.android.openloader.feature.installer.impl.QueuedApk

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
fun HomeScreen(
    viewModel: InstallerViewModel = hiltViewModel(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToSetup: () -> Unit = {},
) {
    val queue by viewModel.queue.collectAsStateWithLifecycle()
    val installing by viewModel.installing.collectAsStateWithLifecycle()
    val installMode by viewModel.installMode.collectAsStateWithLifecycle()
    val wirelessAdbConfigured by viewModel.wirelessAdbConfigured.collectAsStateWithLifecycle()
    val adbWirelessKeysReady by viewModel.adbWirelessKeysReady.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val alreadyConfiguredToastMessage = stringResource(R.string.home_setup_already_configured)
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    
    var showSetupDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }
    var isReady by remember { mutableStateOf(false) }
    var needsSetup by remember { mutableStateOf(false) }

    val pickLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNotEmpty()) {
            viewModel.addStagedUris(uris)
        }
    }

    LaunchedEffect(installMode, wirelessAdbConfigured, adbWirelessKeysReady) {
        isReady = viewModel.isInstallMethodReady()
        needsSetup = !isReady
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    isReady = viewModel.isInstallMethodReady()
                    needsSetup = !isReady
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val hasPendingItems = queue.any { it.status == QueueStatus.Pending }
    val hasCompletedItems = queue.any { it.status == QueueStatus.Success || it.status == QueueStatus.Failed }
    val allCompleted = queue.isNotEmpty() && queue.all { it.status == QueueStatus.Success || it.status == QueueStatus.Failed }
    val hasRetriableFailed = queue.any { viewModel.canRetryItem(it) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.home_title))
                        if (queue.isNotEmpty()) {
                            val pendingCount = queue.count { it.status == QueueStatus.Pending }
                            val completedCount = queue.count { it.status == QueueStatus.Success || it.status == QueueStatus.Failed }
                            Text(
                                when {
                                    installing -> "Installing..."
                                    allCompleted -> stringResource(R.string.home_queue_finished, completedCount)
                                    pendingCount > 0 -> "$pendingCount pending"
                                    else -> "${queue.size} APK${if (queue.size > 1) "s" else ""}"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToHistory) {
                        Icon(
                            OpenLoaderIcons.History,
                            contentDescription = stringResource(R.string.history_content_description),
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(OpenLoaderIcons.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        floatingActionButton = {
        }
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

            AnimatedVisibility(
                visible = needsSetup && queue.isNotEmpty() && !installing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                SetupWarningCard(
                    onConfigure = { showSetupDialog = true },
                    modifier = Modifier.padding(16.dp)
                )
            }

            if (queue.isEmpty()) {
                EmptyState(
                    onAddClick = { pickLauncher.launch(arrayOf("application/vnd.android.package-archive")) }
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    LazyColumn(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(
                            items = queue,
                            key = { it.localPath },
                        ) { item ->
                            val canRetry = remember(item.localPath, item.status) {
                                viewModel.canRetryItem(item)
                            }
                            ApkQueueItem(
                                item = item,
                                onRemove = {
                                    if (item.status != QueueStatus.Installing) {
                                        viewModel.removeFromQueue(item.localPath)
                                    }
                                },
                                onRetry = if (canRetry) {
                                    {
                                        viewModel.retryItem(item.localPath)
                                        scope.launch {
                                            val ready = viewModel.isInstallMethodReady()
                                            if (!ready) {
                                                showSetupDialog = true
                                            } else {
                                                viewModel.runInstallQueue()
                                            }
                                        }
                                    }
                                } else {
                                    null
                                },
                            )
                        }
                    }
                }

                if (queue.isNotEmpty()) {
                    Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    when {
                        installing -> {
                            OutlinedButton(
                                onClick = { viewModel.cancelInstallation() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = OpenLoaderIcons.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Cancel Installation")
                            }
                        }
                        allCompleted -> {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                if (hasRetriableFailed) {
                                    Button(
                                        onClick = {
                                            viewModel.retryAllStagedFailed()
                                            scope.launch {
                                                val ready = viewModel.isInstallMethodReady()
                                                if (!ready) {
                                                    showSetupDialog = true
                                                } else {
                                                    viewModel.runInstallQueue()
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Icon(
                                            imageVector = OpenLoaderIcons.Refresh,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.home_retry))
                                    }
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    FilledTonalButton(
                                        onClick = { viewModel.clearQueue() },
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Icon(
                                            imageVector = OpenLoaderIcons.Clear,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(stringResource(R.string.action_clear))
                                    }
                                    Button(
                                        onClick = {
                                            pickLauncher.launch(
                                                arrayOf("application/vnd.android.package-archive"),
                                            )
                                        },
                                        modifier = Modifier.weight(2f),
                                    ) {
                                        Icon(
                                            imageVector = OpenLoaderIcons.Add,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Install More")
                                    }
                                }
                            }
                        }
                        hasPendingItems -> {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { pickLauncher.launch(arrayOf("application/vnd.android.package-archive")) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = OpenLoaderIcons.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add")
                                }
                                Button(
                                    onClick = {
                                        if (needsSetup) {
                                            showSetupDialog = true
                                        } else {
                                            viewModel.runInstallQueue()
                                        }
                                    },
                                    enabled = !installing,
                                    modifier = Modifier.weight(2f)
                                ) {
                                    Text("Install ${queue.count { it.status == QueueStatus.Pending }} APK${if (queue.count { it.status == QueueStatus.Pending } > 1) "s" else ""}")
                                }
                            }
                        }
                    }
                }
                }
                }
            }
        }
    }

    if (showSetupDialog) {
        SetupRequiredDialog(
            onDismiss = { showSetupDialog = false },
            onConfigure = {
                showSetupDialog = false
                scope.launch {
                    val alreadyReady = viewModel.isInstallMethodReady()
                    if (alreadyReady) {
                        Toast.makeText(
                            context,
                            alreadyConfiguredToastMessage,
                            Toast.LENGTH_SHORT,
                        ).show()
                    } else {
                        onNavigateToSetup()
                    }
                }
            },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Queue") },
            text = { Text("Remove all ${queue.size} APK${if (queue.size > 1) "s" else ""} from the queue?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearQueue()
                        showClearDialog = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun SetupWarningCard(
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = OpenLoaderIcons.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_setup_required),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            TextButton(onClick = onConfigure) {
                Text(stringResource(R.string.home_configure_now))
            }
        }
    }
}

@Composable
private fun EmptyState(
    onAddClick: () -> Unit
) {
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
                stringResource(R.string.home_no_apks),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                stringResource(R.string.home_tap_to_add),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onAddClick) {
                Icon(OpenLoaderIcons.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add APK Files")
            }
        }
    }
}

@Composable
private fun ApkQueueItem(
    item: QueuedApk,
    onRemove: () -> Unit,
    onRetry: (() -> Unit)? = null,
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
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (icon != null) {
                    Image(
                        bitmap = icon.toBitmap().asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        alignment = Alignment.Center,
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
                if (packageName != null && packageName != label) {
                    Text(
                        text = packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = when (item.status) {
                        QueueStatus.Pending -> stringResource(R.string.status_pending)
                        QueueStatus.Installing -> stringResource(R.string.status_installing)
                        QueueStatus.Success -> stringResource(R.string.status_success)
                        QueueStatus.Failed -> item.message ?: stringResource(R.string.status_failed)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (item.status) {
                        QueueStatus.Pending -> MaterialTheme.colorScheme.onSurfaceVariant
                        QueueStatus.Installing -> MaterialTheme.colorScheme.primary
                        QueueStatus.Success -> MaterialTheme.colorScheme.primary
                        QueueStatus.Failed -> MaterialTheme.colorScheme.error
                    },
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            when (item.status) {
                QueueStatus.Pending -> {
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = OpenLoaderIcons.Close,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                QueueStatus.Installing -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
                QueueStatus.Success -> {
                    Icon(
                        imageVector = OpenLoaderIcons.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                QueueStatus.Failed -> {
                    if (onRetry != null) {
                        IconButton(onClick = onRetry) {
                            Icon(
                                imageVector = OpenLoaderIcons.Refresh,
                                contentDescription = stringResource(R.string.home_retry_content_description),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        Icon(
                            imageVector = OpenLoaderIcons.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SetupRequiredDialog(
    onDismiss: () -> Unit,
    onConfigure: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_setup_required)) },
        text = {
            Text("You need to configure the install method before installing APKs. Would you like to set it up now?")
        },
        confirmButton = {
            Button(onClick = onConfigure) {
                Text(stringResource(R.string.home_configure_now))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}