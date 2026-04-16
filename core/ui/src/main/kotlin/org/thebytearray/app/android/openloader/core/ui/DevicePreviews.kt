package org.thebytearray.app.android.openloader.core.ui

import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview


@Preview(name = "phone", device = Devices.PHONE, showBackground = true)
@Preview(name = "phone_landscape", widthDp = 891, heightDp = 411, showBackground = true)
@Preview(name = "foldable", device = Devices.FOLDABLE, showBackground = true)
@Preview(name = "tablet", device = Devices.TABLET, showBackground = true)
@Preview(name = "desktop", device = Devices.DESKTOP, showBackground = true)
annotation class DevicePreviews
