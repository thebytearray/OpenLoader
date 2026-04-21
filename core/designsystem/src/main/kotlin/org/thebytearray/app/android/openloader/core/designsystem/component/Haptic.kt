package org.thebytearray.app.android.openloader.core.designsystem.component

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

@Composable
fun withHaptic(
    type: HapticFeedbackType = HapticFeedbackType.TextHandleMove,
    onClick: () -> Unit,
): () -> Unit {
    val haptic = LocalHapticFeedback.current
    return {
        haptic.performHapticFeedback(type)
        onClick()
    }
}
