package org.thebytearray.app.android.openloader.core.designsystem.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.rememberDynamicColorScheme

@Composable
private fun createAmoledColorScheme(seedColor: Color): ColorScheme {
    val baseScheme = rememberDynamicColorScheme(
        seedColor = seedColor, isDark = true, isAmoled = true
    )

    return darkColorScheme(
        primary = baseScheme.primary,
        onPrimary = baseScheme.onPrimary,
        primaryContainer = baseScheme.primaryContainer,
        onPrimaryContainer = baseScheme.onPrimaryContainer,
        secondary = baseScheme.secondary,
        onSecondary = baseScheme.onSecondary,
        secondaryContainer = baseScheme.secondaryContainer,
        onSecondaryContainer = baseScheme.onSecondaryContainer,
        tertiary = baseScheme.tertiary,
        onTertiary = baseScheme.onTertiary,
        tertiaryContainer = baseScheme.tertiaryContainer,
        onTertiaryContainer = baseScheme.onTertiaryContainer,
        error = baseScheme.error,
        onError = baseScheme.onError,
        errorContainer = baseScheme.errorContainer,
        onErrorContainer = baseScheme.onErrorContainer,
        background = PureBlack,
        onBackground = baseScheme.onBackground,
        surface = PureBlack,
        onSurface = baseScheme.onSurface,
        surfaceVariant = Color(0xFF1A1A1A),
        onSurfaceVariant = baseScheme.onSurfaceVariant,
        outline = baseScheme.outline,
        outlineVariant = baseScheme.outlineVariant,
        scrim = baseScheme.scrim,
        inverseSurface = baseScheme.inverseSurface,
        inverseOnSurface = baseScheme.inverseOnSurface,
        inversePrimary = baseScheme.inversePrimary,
        surfaceDim = PureBlack,
        surfaceBright = PureBlack,
        surfaceContainerLowest = PureBlack,
        surfaceContainerLow = PureBlack,
        surfaceContainer = PureBlack,
        surfaceContainerHigh = PureBlack,
        surfaceContainerHighest = PureBlack,
    )
}

@Composable
fun OpenLoaderTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    seedColor: Color = OpenLoaderSeedColor,
    isAmoled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme = when {
        isAmoled -> {
            createAmoledColorScheme(seedColor = seedColor)
        }

        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        else -> {
            rememberDynamicColorScheme(
                seedColor = seedColor, isDark = darkTheme, isAmoled = false
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content,
    )
}