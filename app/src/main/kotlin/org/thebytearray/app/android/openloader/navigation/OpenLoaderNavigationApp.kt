package org.thebytearray.app.android.openloader.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import org.thebytearray.app.android.openloader.core.navigation.Navigator
import org.thebytearray.app.android.openloader.core.navigation.rememberNavigationState
import org.thebytearray.app.android.openloader.core.navigation.toEntries
import org.thebytearray.app.android.openloader.feature.installer.api.navigation.HomeNavKey
import org.thebytearray.app.android.openloader.feature.installer.impl.navigation.installerMainEntries
import org.thebytearray.app.android.openloader.feature.installer.impl.navigation.installerSetupEntries

@Composable
fun OpenLoaderNavigationApp(modifier: Modifier = Modifier) {
    val navigationState = rememberNavigationState(
        startKey = HomeNavKey,
        topLevelKeys = setOf(HomeNavKey),
    )
    val navigator = remember(navigationState) {
        Navigator(navigationState)
    }
    val entryProvider = entryProvider {
        installerMainEntries(navigator)
        installerSetupEntries(navigator)
        adbSetupAppEntry(navigator)
    }

    NavDisplay(
        entries = navigationState.toEntries(entryProvider),
        onBack = { navigator.goBack() },
        modifier = modifier,
    )
}
