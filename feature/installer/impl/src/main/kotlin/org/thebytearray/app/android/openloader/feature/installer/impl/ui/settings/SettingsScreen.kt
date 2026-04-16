package org.thebytearray.app.android.openloader.feature.installer.impl.ui.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.materialkolor.palettes.CorePalette
import org.thebytearray.app.android.openloader.core.designsystem.icon.OpenLoaderIcons
import org.thebytearray.app.android.openloader.core.model.InstallMode
import org.thebytearray.app.android.openloader.core.model.ThemeColor
import org.thebytearray.app.android.openloader.core.model.ThemeMode
import org.thebytearray.app.android.openloader.core.ui.toComposeColor
import org.thebytearray.app.android.openloader.feature.installer.impl.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val dynamicColor by viewModel.dynamicColor.collectAsStateWithLifecycle()
    val themeColor by viewModel.themeColor.collectAsStateWithLifecycle()
    var showThemeDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(OpenLoaderIcons.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_appearance))
            }

            item {
                val context = LocalContext.current
                val appearanceItemCount = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) 2 else 1
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SegmentedSettingsItem(
                        title = stringResource(R.string.settings_theme),
                        description = when (themeMode) {
                            ThemeMode.SYSTEM -> stringResource(R.string.theme_system)
                            ThemeMode.LIGHT -> stringResource(R.string.theme_light)
                            ThemeMode.DARK -> stringResource(R.string.theme_dark)
                            ThemeMode.AMOLED -> stringResource(R.string.theme_amoled)
                        },
                        icon = when (themeMode) {
                            ThemeMode.SYSTEM -> OpenLoaderIcons.SettingsBrightness
                            ThemeMode.LIGHT -> OpenLoaderIcons.LightMode
                            ThemeMode.DARK -> OpenLoaderIcons.DarkMode
                            ThemeMode.AMOLED -> OpenLoaderIcons.Contrast
                        },
                        index = 0,
                        count = appearanceItemCount,
                        onClick = { showThemeDialog = true }
                    )

                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        SegmentedSwitchItem(
                            title = stringResource(R.string.settings_dynamic_color),
                            description = stringResource(R.string.settings_dynamic_color_desc),
                            icon = OpenLoaderIcons.Palette,
                            isChecked = dynamicColor,
                            onCheckedChange = { viewModel.setDynamicColor(it) },
                            index = 1,
                            count = appearanceItemCount,
                            enabled = themeMode != ThemeMode.AMOLED
                        )
                    }
                }
            }

            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_color_palette))
            }

            item {
                ColorPaletteSelector(
                    selectedColor = themeColor,
                    isDynamicColor = dynamicColor,
                    onColorSelected = { color ->
                        viewModel.setThemeColor(color)
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                            viewModel.setDynamicColor(false)
                        }
                    }
                )
            }

            item {
                SettingsSectionHeader(title = stringResource(R.string.settings_install_method))
            }

            item {
                val installMode by viewModel.installMode.collectAsStateWithLifecycle()
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SegmentedSettingsItem(
                        title = stringResource(R.string.settings_default_method),
                        description = when (installMode) {
                            InstallMode.SHIZUKU -> stringResource(R.string.method_shizuku)
                            InstallMode.WIRELESS_ADB -> stringResource(R.string.method_wireless_adb)
                        },
                        icon = OpenLoaderIcons.SettingsOutlined,
                        index = 0,
                        count = 1,
                        onClick = {
                            viewModel.setInstallMode(
                                if (installMode == InstallMode.SHIZUKU)
                                    InstallMode.WIRELESS_ADB
                                else
                                    InstallMode.SHIZUKU
                            )
                        }
                    )
                }
            }
        }

        if (showThemeDialog) {
            ThemeSelectionDialog(
                currentTheme = themeMode,
                onThemeSelected = {
                    viewModel.setThemeMode(it)
                    showThemeDialog = false
                },
                onDismiss = { showThemeDialog = false }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SegmentedSettingsItem(
    title: String,
    description: String,
    icon: ImageVector,
    index: Int,
    count: Int,
    onClick: () -> Unit,
) {
    val shape = when {
        count == 1 -> RoundedCornerShape(16.dp)
        index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        index == count - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        else -> RoundedCornerShape(4.dp)
    }

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        headlineContent = { Text(title) },
        supportingContent = { Text(description) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SegmentedSwitchItem(
    title: String,
    description: String,
    icon: ImageVector,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    index: Int,
    count: Int,
    enabled: Boolean = true
) {
    val shape = when {
        count == 1 -> RoundedCornerShape(16.dp)
        index == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 4.dp, bottomEnd = 4.dp)
        index == count - 1 -> RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
        else -> RoundedCornerShape(4.dp)
    }

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(enabled = enabled) { onCheckedChange(!isChecked) },
        colors = ListItemDefaults.colors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        },
        headlineContent = {
            Text(
                text = title,
                color = if (enabled) Color.Unspecified else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
        },
        supportingContent = {
            Text(
                text = description,
                color = if (enabled) Color.Unspecified else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            )
        },
        trailingContent = {
            Switch(
                checked = isChecked,
                onCheckedChange = if (enabled) onCheckedChange else { _ -> },
                enabled = enabled
            )
        }
    )
}

@Composable
private fun ColorPaletteSelector(
    selectedColor: ThemeColor,
    isDynamicColor: Boolean,
    onColorSelected: (ThemeColor) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeColor.entries.take(4).forEach { color ->
                ColorPaletteItem(
                    color = color.toComposeColor().toArgb(),
                    isSelected = !isDynamicColor && selectedColor == color,
                    onSelected = { onColorSelected(color) }
                )
            }
        }
        Spacer(modifier = Modifier.size(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeColor.entries.drop(4).forEach { color ->
                ColorPaletteItem(
                    color = color.toComposeColor().toArgb(),
                    isSelected = !isDynamicColor && selectedColor == color,
                    onSelected = { onColorSelected(color) }
                )
            }
        }
    }
}

@Composable
private fun RowScope.ColorPaletteItem(
    color: Int,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    val corePalette = remember(color) { CorePalette.of(color) }
    val animatedCheckSize by animateDpAsState(
        targetValue = if (isSelected) 28.dp else 0.dp,
        label = "checkSize"
    )
    val animatedIconSize by animateDpAsState(
        targetValue = if (isSelected) 16.dp else 0.dp,
        label = "iconSize"
    )

    Surface(
        onClick = onSelected,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(corePalette.a1.tone(80)))
            ) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .size(24.dp)
                        .background(Color(corePalette.a2.tone(90)))
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(24.dp)
                        .background(Color(corePalette.a3.tone(60)))
                )
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(animatedCheckSize)
                            .align(Alignment.Center)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = OpenLoaderIcons.Check,
                            contentDescription = null,
                            modifier = Modifier.size(animatedIconSize),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThemeSelectionDialog(
    currentTheme: ThemeMode,
    onThemeSelected: (ThemeMode) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.settings_theme)) },
        text = {
            Column(modifier = Modifier.selectableGroup()) {
                ThemeOption(
                    title = stringResource(R.string.theme_system),
                    icon = OpenLoaderIcons.SettingsBrightness,
                    isSelected = currentTheme == ThemeMode.SYSTEM,
                    onClick = { onThemeSelected(ThemeMode.SYSTEM) }
                )
                ThemeOption(
                    title = stringResource(R.string.theme_light),
                    icon = OpenLoaderIcons.LightMode,
                    isSelected = currentTheme == ThemeMode.LIGHT,
                    onClick = { onThemeSelected(ThemeMode.LIGHT) }
                )
                ThemeOption(
                    title = stringResource(R.string.theme_dark),
                    icon = OpenLoaderIcons.DarkMode,
                    isSelected = currentTheme == ThemeMode.DARK,
                    onClick = { onThemeSelected(ThemeMode.DARK) }
                )
                ThemeOption(
                    title = stringResource(R.string.theme_amoled),
                    icon = OpenLoaderIcons.Contrast,
                    isSelected = currentTheme == ThemeMode.AMOLED,
                    onClick = { onThemeSelected(ThemeMode.AMOLED) }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}

@Composable
private fun ThemeOption(
    title: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onClick,
                role = Role.RadioButton
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        RadioButton(
            selected = isSelected,
            onClick = null
        )
    }
}