package org.thebytearray.app.android.openloader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.thebytearray.app.android.openloader.core.datastore.UserPreferencesDataSource
import org.thebytearray.app.android.openloader.core.designsystem.theme.OpenLoaderTheme
import org.thebytearray.app.android.openloader.core.model.ThemeMode
import org.thebytearray.app.android.openloader.core.ui.toComposeColor
import org.thebytearray.app.android.openloader.navigation.OpenLoaderNavigationApp
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var userPreferencesDataSource: UserPreferencesDataSource

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val initialThemeMode = runBlocking { userPreferencesDataSource.themeMode.first() }
        val initialDynamicColor = runBlocking { userPreferencesDataSource.dynamicColor.first() }
        val initialThemeColor = runBlocking { userPreferencesDataSource.themeColor.first() }

        setContent {
            var themeMode by remember { mutableStateOf(initialThemeMode) }
            var dynamicColor by remember { mutableStateOf(initialDynamicColor) }
            var themeColor by remember { mutableStateOf(initialThemeColor) }
            
            LaunchedEffect(Unit) {
                userPreferencesDataSource.themeMode.collect { themeMode = it }
            }
            LaunchedEffect(Unit) {
                userPreferencesDataSource.dynamicColor.collect { dynamicColor = it }
            }
            LaunchedEffect(Unit) {
                userPreferencesDataSource.themeColor.collect { themeColor = it }
            }
            
            val isDarkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK, ThemeMode.AMOLED -> true
            }
            
            OpenLoaderTheme(
                darkTheme = isDarkTheme,
                dynamicColor = dynamicColor,
                seedColor = themeColor.toComposeColor(),
                isAmoled = themeMode == ThemeMode.AMOLED
            ) {
                OpenLoaderNavigationApp()
            }
        }
    }
}