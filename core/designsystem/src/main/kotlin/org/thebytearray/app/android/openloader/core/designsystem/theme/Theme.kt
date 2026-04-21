@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package org.thebytearray.app.android.openloader.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.expressiveLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.rememberDynamicColorScheme

@Composable
private fun expressiveLightSchemeFromSeed(seedColor: Color): ColorScheme {
    val base = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = false,
        isAmoled = false,
    )
    return expressiveLightColorScheme().copy(
        primary = base.primary,
        onPrimary = base.onPrimary,
        primaryContainer = base.primaryContainer,
        onPrimaryContainer = base.onPrimaryContainer,
        inversePrimary = base.inversePrimary,
        secondary = base.secondary,
        onSecondary = base.onSecondary,
        secondaryContainer = base.secondaryContainer,
        onSecondaryContainer = base.onSecondaryContainer,
        tertiary = base.tertiary,
        onTertiary = base.onTertiary,
        tertiaryContainer = base.tertiaryContainer,
        onTertiaryContainer = base.onTertiaryContainer,
        background = base.background,
        onBackground = base.onBackground,
        surface = base.surface,
        onSurface = base.onSurface,
        surfaceVariant = base.surfaceVariant,
        onSurfaceVariant = base.onSurfaceVariant,
        surfaceDim = base.surfaceDim,
        surfaceBright = base.surfaceBright,
        surfaceContainerLowest = base.surfaceContainerLowest,
        surfaceContainerLow = base.surfaceContainerLow,
        surfaceContainer = base.surfaceContainer,
        surfaceContainerHigh = base.surfaceContainerHigh,
        surfaceContainerHighest = base.surfaceContainerHighest,
        inverseSurface = base.inverseSurface,
        inverseOnSurface = base.inverseOnSurface,
        outline = base.outline,
        outlineVariant = base.outlineVariant,
        error = base.error,
        onError = base.onError,
        errorContainer = base.errorContainer,
        onErrorContainer = base.onErrorContainer,
        scrim = base.scrim,
    )
}

@Composable
private fun expressiveDarkSchemeFromSeed(seedColor: Color): ColorScheme {
    val base = rememberDynamicColorScheme(
        seedColor = seedColor,
        isDark = true,
        isAmoled = false,
    )
    return darkColorScheme(
        primary = base.primary,
        onPrimary = base.onPrimary,
        primaryContainer = base.primaryContainer,
        onPrimaryContainer = base.onPrimaryContainer,
        inversePrimary = base.inversePrimary,
        secondary = base.secondary,
        onSecondary = base.onSecondary,
        secondaryContainer = base.secondaryContainer,
        onSecondaryContainer = base.onSecondaryContainer,
        tertiary = base.tertiary,
        onTertiary = base.onTertiary,
        tertiaryContainer = base.tertiaryContainer,
        onTertiaryContainer = base.onTertiaryContainer,
        background = base.background,
        onBackground = base.onBackground,
        surface = base.surface,
        onSurface = base.onSurface,
        surfaceVariant = base.surfaceVariant,
        onSurfaceVariant = base.onSurfaceVariant,
        surfaceDim = base.surfaceDim,
        surfaceBright = base.surfaceBright,
        surfaceContainerLowest = base.surfaceContainerLowest,
        surfaceContainerLow = base.surfaceContainerLow,
        surfaceContainer = base.surfaceContainer,
        surfaceContainerHigh = base.surfaceContainerHigh,
        surfaceContainerHighest = base.surfaceContainerHighest,
        inverseSurface = base.inverseSurface,
        inverseOnSurface = base.inverseOnSurface,
        outline = base.outline,
        outlineVariant = base.outlineVariant,
        error = base.error,
        onError = base.onError,
        errorContainer = base.errorContainer,
        onErrorContainer = base.onErrorContainer,
        scrim = base.scrim,
    )
}

private fun ColorScheme.toAmoled(): ColorScheme = copy(
    background = PureBlack,
    surface = PureBlack,
    surfaceDim = PureBlack,
    surfaceContainerLowest = PureBlack,
    surfaceContainerLow = surfaceContainerLowest,
    surfaceContainer = surfaceContainerLow,
    surfaceContainerHigh = surfaceContainer,
    surfaceContainerHighest = surfaceContainerHigh,
)

@Composable
fun OpenLoaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    seedColor: Color = OpenLoaderSeedColor,
    isAmoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val baseScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> expressiveDarkSchemeFromSeed(seedColor)
        else -> expressiveLightSchemeFromSeed(seedColor)
    }

    val colorScheme = if (isAmoled) baseScheme.toAmoled() else baseScheme

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}
