@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package org.thebytearray.app.android.openloader.feature.installer.impl.ui.about

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import org.thebytearray.app.android.openloader.core.designsystem.component.OlTopAppBar
import org.thebytearray.app.android.openloader.core.designsystem.component.SineWaveShape
import org.thebytearray.app.android.openloader.core.designsystem.component.WaveEdge
import org.thebytearray.app.android.openloader.core.designsystem.component.withHaptic
import org.thebytearray.app.android.openloader.core.designsystem.icon.OpenLoaderIcons
import org.thebytearray.app.android.openloader.feature.installer.impl.R

private const val GITHUB_URL = "https://github.com/thebytearray/OpenLoader"
private const val LICENSE_URL = "https://github.com/thebytearray/OpenLoader/blob/master/LICENSE"
private const val RELEASES_URL = "https://github.com/thebytearray/OpenLoader/releases"
private const val DEV_GITHUB_URL = "https://github.com/codewithtamim"
private const val ORG_URL = "https://github.com/thebytearray"
private const val DEV_EMAIL = "mailto:tamim@thebytearray.org"

@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit = {},
) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
        } catch (_: PackageManager.NameNotFoundException) {
            "1.0.0"
        }
    }

    val openUrl: (String) -> Unit = { url ->
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }
    }

    Scaffold(
        topBar = {
            OlTopAppBar(
                title = stringResource(R.string.about_title),
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = OpenLoaderIcons.ArrowBack,
                            contentDescription = stringResource(R.string.action_back),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            item { HeroSection(versionName = versionName, openUrl = openUrl) }

            item { DeveloperSection(openUrl = openUrl) }

            item {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(25.dp),
                )
            }
        }
    }
}

@Composable
private fun HeroSection(
    versionName: String,
    openUrl: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        ) {
            Spacer(
                modifier = Modifier
                    .requiredSize(120.dp)
                    .clip(MaterialShapes.Cookie9Sided.toShape())
                    .clickable(onClick = withHaptic {})
                    .background(MaterialTheme.colorScheme.primaryContainer),
            )
            Icon(
                imageVector = OpenLoaderIcons.Android,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(60.dp),
            )
        }

        Text(
            text = stringResource(R.string.home_title),
            fontWeight = FontWeight.Black,
            style = MaterialTheme.typography.displaySmallEmphasized.copy(
                letterSpacing = 0.025.em,
            ),
            textAlign = TextAlign.Center,
        )

        FlowRow(
            itemVerticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(
                space = 15.dp,
                alignment = Alignment.CenterHorizontally,
            ),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            AppHandlesChip(
                icon = OpenLoaderIcons.Code,
                title = stringResource(R.string.about_github),
                description = stringResource(R.string.about_github_desc),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                onClick = { openUrl(GITHUB_URL) },
            )

            AppHandlesChip(
                icon = OpenLoaderIcons.History,
                title = "v$versionName",
                description = stringResource(R.string.about_version_desc),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                onClick = { openUrl(RELEASES_URL) },
            )

            AppHandlesChip(
                icon = OpenLoaderIcons.Verified,
                title = stringResource(R.string.about_license),
                description = stringResource(R.string.about_license_desc),
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                onClick = { openUrl(LICENSE_URL) },
            )
        }
    }
}

@Composable
private fun DeveloperSection(
    openUrl: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                SineWaveShape(
                    amplitude = 10f,
                    frequency = 5f,
                    edge = WaveEdge.Both,
                ),
            )
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        Text(
            text = stringResource(R.string.about_developer),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 25.dp)
                .align(Alignment.Start),
        )

        ProfileAvatar(
            initial = "T",
            size = 150.dp,
        )

        Text(
            text = stringResource(R.string.about_developer_name),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Text(
            text = stringResource(R.string.about_developer_role),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(
            text = stringResource(R.string.about_developer_bio),
            style = MaterialTheme.typography.bodySmall,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        ContactHandlesCard(
            modifier = Modifier.padding(
                start = 15.dp,
                end = 15.dp,
                bottom = 25.dp,
            ),
            openUrl = openUrl,
        )
    }
}

@Composable
private fun ContactHandlesCard(
    modifier: Modifier = Modifier,
    openUrl: (String) -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 20.dp, vertical = 10.dp)
                .height(IntrinsicSize.Min),
        ) {
            ContactBox(
                modifier = Modifier.weight(1f),
                icon = OpenLoaderIcons.Email,
                label = stringResource(R.string.about_contact_email),
                onClick = { openUrl(DEV_EMAIL) },
            )

            ContactDivider()

            ContactBox(
                modifier = Modifier.weight(1f),
                icon = OpenLoaderIcons.Code,
                label = stringResource(R.string.about_contact_github),
                onClick = { openUrl(DEV_GITHUB_URL) },
            )

            ContactDivider()

            ContactBox(
                modifier = Modifier.weight(1f),
                icon = OpenLoaderIcons.Business,
                label = stringResource(R.string.about_contact_org),
                onClick = { openUrl(ORG_URL) },
            )
        }
    }
}

@Composable
private fun ContactDivider() {
    VerticalDivider(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.padding(horizontal = 5.dp),
    )
}

@Composable
private fun ContactBox(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp),
        modifier = modifier
            .clip(MaterialTheme.shapes.extraLarge)
            .clickable(onClick = withHaptic { onClick() })
            .padding(vertical = 8.dp),
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProfileAvatar(
    initial: String,
    size: Dp,
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(MaterialShapes.Cookie9Sided.toShape())
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = MaterialShapes.Cookie9Sided.toShape(),
            )
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = initial.uppercase(),
            style = MaterialTheme.typography.displayMediumEmphasized,
            fontWeight = FontWeight.Black,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun AppHandlesChip(
    icon: ImageVector,
    title: String,
    description: String,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    onClick: () -> Unit = {},
) {
    FilledTonalButton(
        shapes = ButtonDefaults.shapes(),
        colors = ButtonDefaults.filledTonalButtonColors(containerColor, contentColor),
        onClick = withHaptic(HapticFeedbackType.VirtualKey) { onClick() },
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
        )

        Spacer(modifier = Modifier.width(10.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMediumEmphasized,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmallEmphasized,
                color = contentColor.copy(alpha = 0.85f),
            )
        }
    }
}
