package org.thebytearray.app.android.openloader.core.adb

import android.content.Context
import android.content.pm.PackageManager


object ApkArchiveMetadata {
    fun read(context: Context, apkPath: String): Pair<String?, String?> {
        return try {
            val pm = context.packageManager
            val info = pm.getPackageArchiveInfo(apkPath, PackageManager.GET_META_DATA)
                ?: return null to null
            info.applicationInfo?.apply {
                sourceDir = apkPath
                publicSourceDir = apkPath
            }
            val pkg = info.packageName
            val label = info.applicationInfo?.loadLabel(pm)?.toString()
            pkg to label
        } catch (_: Exception) {
            null to null
        }
    }
}
