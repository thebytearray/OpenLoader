package org.thebytearray.app.android.openloader.core.ui

import androidx.compose.ui.graphics.Color
import org.thebytearray.app.android.openloader.core.model.ThemeColor

fun ThemeColor.toComposeColor(): Color = Color(seedArgb.toInt())
