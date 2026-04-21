@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalResources
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
import org.thebytearray.app.android.openloader.core.designsystem.component.OlFilledTonalIconTextButton
import org.thebytearray.app.android.openloader.core.designsystem.component.OlHeroAction
import org.thebytearray.app.android.openloader.core.designsystem.component.OlHeroHeader
import org.thebytearray.app.android.openloader.core.designsystem.component.OlIconTextButton
import org.thebytearray.app.android.openloader.core.designsystem.component.OlOutlinedIconTextButton
import org.thebytearray.app.android.openloader.core.designsystem.icon.OpenLoaderIcons
import org.thebytearray.app.android.openloader.core.designsystem.theme.Dimens
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
    val resources = LocalResources.current
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
            val result = viewModel.addStagedUris(uris)
            if (result.skippedDuplicates > 0) {
                val msg = if (result.skippedDuplicates == 1) {
                    resources.getString(R.string.home_duplicate_skipped, 1)
                } else {
                    resources.getString(R.string.home_duplicate_skipped_plural, result.skippedDuplicates)
                }
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            }
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
    val hasRetriableFailed = queue.any { viewModel.canRetryItem(it) }
    val allCompleted = queue.isNotEmpty() && queue.all { it.status == QueueStatus.Success || it.status == QueueStatus.Failed }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OlHeroHeader(
                title = stringResource(R.string.home_title),
                subtitle = if (queue.isNotEmpty()) {
                    val pendingCount = queue.count { it.status == QueueStatus.Pending }
                    val completedCount = queue.count { it.status == QueueStatus.Success || it.status == QueueStatus.Failed }
                    when {
                        installing -> "Installing..."
                        allCompleted -> stringResource(R.string.home_queue_finished, completedCount)
                        pendingCount > 0 -> "$pendingCount pending"
                        else -> "${queue.size} APK${if (queue.size > 1) "s" else ""}"
                    }
                } else null,
                actions = listOf(
                    OlHeroAction(
                        icon = OpenLoaderIcons.History,
                        contentDescription = stringResource(R.string.history_content_description),
                        onClick = onNavigateToHistory,
                    ),
                    OlHeroAction(
                        icon = OpenLoaderIcons.Settings,
                        contentDescription = "Settings",
                        onClick = onNavigateToSettings,
                    ),
                ),
            )

            if (installing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dimens.paddingLarge)
                        .height(4.dp),
                )
                Spacer(modifier = Modifier.height(Dimens.paddingSmall))
            }

            AnimatedVisibility(
                visible = needsSetup && queue.isNotEmpty() && !installing,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                SetupWarningCard(
                    onConfigure = {
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
                    modifier = Modifier.padding(
                        horizontal = Dimens.paddingLarge,
                        vertical = Dimens.paddingSmall,
                    ),
                )
            }

            if (queue.isEmpty()) {
                EmptyState(
                    onAddClick = { pickLauncher.launch(arrayOf("application/vnd.android.package-archive")) },
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = Dimens.paddingLarge),
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = Dimens.paddingSmall),
                            verticalArrangement = Arrangement.spacedBy(Dimens.paddingSmall),
                            modifier = Modifier.fillMaxSize(),
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

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(Dimens.paddingLarge),
                        verticalArrangement = Arrangement.spacedBy(Dimens.paddingSmall),
                    ) {
                        when {
                            installing -> {
                                OlOutlinedIconTextButton(
                                    text = "Cancel Installation",
                                    icon = OpenLoaderIcons.Close,
                                    onClick = { viewModel.cancelInstallation() },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            allCompleted -> {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(Dimens.paddingSmall),
                                ) {
                                    if (hasRetriableFailed) {
                                        OlIconTextButton(
                                            text = stringResource(R.string.home_retry),
                                            icon = OpenLoaderIcons.Refresh,
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
                                        )
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(Dimens.paddingSmall),
                                    ) {
                                        OlFilledTonalIconTextButton(
                                            text = stringResource(R.string.action_clear),
                                            icon = OpenLoaderIcons.Clear,
                                            onClick = { viewModel.clearQueue() },
                                            modifier = Modifier.weight(1f),
                                        )
                                        OlIconTextButton(
                                            text = "Install More",
                                            icon = OpenLoaderIcons.Add,
                                            onClick = {
                                                pickLauncher.launch(
                                                    arrayOf("application/vnd.android.package-archive"),
                                                )
                                            },
                                            modifier = Modifier.weight(2f),
                                        )
                                    }
                                }
                            }
                            hasPendingItems -> {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(Dimens.paddingSmall),
                                ) {
                                    OlFilledTonalIconTextButton(
                                        text = "Add",
                                        icon = OpenLoaderIcons.Add,
                                        onClick = {
                                            pickLauncher.launch(arrayOf("application/vnd.android.package-archive"))
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                    val pendingCount = queue.count { it.status == QueueStatus.Pending }
                                    OlIconTextButton(
                                        text = "Install $pendingCount APK${if (pendingCount > 1) "s" else ""}",
                                        onClick = {
                                            if (needsSetup) {
                                                showSetupDialog = true
                                            } else {
                                                viewModel.runInstallQueue()
                                            }
                                        },
                                        enabled = !installing,
                                        modifier = Modifier.weight(2f),
                                    )
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
                OlIconTextButton(
                    text = "Clear",
                    onClick = {
                        viewModel.clearQueue()
                        showClearDialog = false
                    },
                )
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("Cancel")
                }
            },
            shape = MaterialTheme.shapes.extraLarge,
        )
    }
}

@Composable
private fun SetupWarningCard(
    onConfigure: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
        ),
        shape = MaterialTheme.shapes.large,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.paddingLarge, vertical = Dimens.paddingMedium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.error,
                        shape = CircleShape,
                    ),
            ) {
                Icon(
                    imageVector = OpenLoaderIcons.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(Dimens.iconSizeLarge),
                )
            }
            Spacer(modifier = Modifier.width(Dimens.paddingMedium))
            Text(
                text = stringResource(R.string.home_setup_required),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onConfigure) {
                Text(
                    text = stringResource(R.string.home_configure_now),
                )
            }
        }
    }
}

@Composable
private fun EmptyState(onAddClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(Dimens.paddingHuge),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.paddingMedium),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(72.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape,
                    ),
            ) {
                Icon(
                    imageVector = OpenLoaderIcons.Android,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(36.dp),
                )
            }
            Text(
                text = stringResource(R.string.home_no_apks),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.home_tap_to_add),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(Dimens.paddingExtraSmall))
            OlIconTextButton(
                text = "Add APK Files",
                icon = OpenLoaderIcons.Add,
                onClick = onAddClick,
            )
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

    val shape = MaterialTheme.shapes.large
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.paddingLarge, vertical = Dimens.paddingMedium),
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
                        modifier = Modifier.size(Dimens.iconSizeLarge),
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
                    style = MaterialTheme.typography.bodySmall,
                    color = when (item.status) {
                        QueueStatus.Pending -> MaterialTheme.colorScheme.onSurfaceVariant
                        QueueStatus.Installing -> MaterialTheme.colorScheme.primary
                        QueueStatus.Success -> MaterialTheme.colorScheme.primary
                        QueueStatus.Failed -> MaterialTheme.colorScheme.error
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.width(Dimens.paddingSmall))

            when (item.status) {
                QueueStatus.Pending -> {
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = OpenLoaderIcons.Close,
                            contentDescription = "Remove",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                QueueStatus.Installing -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(Dimens.iconSizeLarge),
                        strokeWidth = 2.dp,
                    )
                }
                QueueStatus.Success -> {
                    Icon(
                        imageVector = OpenLoaderIcons.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
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
    onConfigure: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.home_setup_required)) },
        text = {
            Text("You need to configure the install method before installing APKs. Would you like to set it up now?")
        },
        confirmButton = {
            OlIconTextButton(
                text = stringResource(R.string.home_configure_now),
                onClick = onConfigure,
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        },
        shape = MaterialTheme.shapes.extraLarge,
    )
}
