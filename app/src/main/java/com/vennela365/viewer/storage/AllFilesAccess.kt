package com.vennela365.viewer.storage

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.content.ContextCompat

/**
 * "All files access" gate.
 *
 * Vennela 365 is built to be restriction-less: when the user grants all-files
 * access it browses every folder on every volume directly, with no per-folder
 * picker. When the grant is absent the app still works, because every screen
 * falls back to the Storage Access Framework and to the app-visible media
 * directories. Nothing is ever silently unavailable.
 */
object AllFilesAccess {

    /** True when the platform lets us read arbitrary paths. */
    fun isGranted(context: Context): Boolean = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R ->
            runCatching { Environment.isExternalStorageManager() }.getOrDefault(false)
        else -> legacyReadGranted(context)
    }

    /** The legacy runtime grant used below Android 11. */
    fun legacyReadGranted(context: Context): Boolean {
        val read = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
        if (!read) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }

    /** Permissions to request at runtime on API levels below 30. */
    fun runtimePermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO
        )
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        else -> arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
    }

    /**
     * Opens the all-files-access settings page, falling back to the app details
     * screen when a device does not expose the dedicated page.
     */
    fun settingsIntent(context: Context): Intent {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val direct = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                .setData(Uri.fromParts("package", context.packageName, null))
            if (direct.resolveActivity(context.packageManager) != null) return direct
            val generic = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
            if (generic.resolveActivity(context.packageManager) != null) return generic
        }
        return Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", context.packageName, null))
    }

    /** True when the settings page actually launches; callers use the result. */
    fun launchSettings(context: Context): Boolean = runCatching {
        context.startActivity(settingsIntent(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)

    /**
     * Whether a path can be listed right now — all-files access, or the legacy
     * read grant for volumes the platform still exposes directly.
     */
    fun canBrowseDirectly(context: Context): Boolean =
        isGranted(context) || legacyReadGranted(context)

    /** Human label for the current state, used by the UI. */
    fun statusLabel(context: Context): String = when {
        isGranted(context) -> "All files access granted"
        legacyReadGranted(context) -> "Media access granted"
        else -> "Limited access"
    }
}
