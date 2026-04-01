package com.kgjr.unplug.helper

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

object PermissionHelper {
    private const val TAG = "PermissionHelper"

    // ── Check individual permissions ─────────────────────────────────────────

    fun hasAccessibilityPermission(context: Context): Boolean {
        val service = "${context.packageName}/${com.kgjr.unplug.service.UnplugAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        val granted = enabledServices.contains(service)
        Log.d(TAG, "Accessibility granted=$granted (enabled=$enabledServices)")
        return granted
    }

    fun hasUsageStatsPermission(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        val granted = mode == AppOpsManager.MODE_ALLOWED
        Log.d(TAG, "UsageStats granted=$granted")
        return granted
    }

    fun hasOverlayPermission(context: Context): Boolean {
        val granted = Settings.canDrawOverlays(context)
        Log.d(TAG, "Overlay granted=$granted")
        return granted
    }

    fun hasAllPermissions(context: Context): Boolean =
        hasAccessibilityPermission(context) &&
        hasUsageStatsPermission(context) &&
        hasOverlayPermission(context)

    // ── Open settings screens ─────────────────────────────────────────────────

    fun openAccessibilitySettings(context: Context) {
        Log.d(TAG, "Opening Accessibility Settings")
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun openUsageStatsSettings(context: Context) {
        Log.d(TAG, "Opening Usage Access Settings")
        context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun openOverlaySettings(context: Context) {
        Log.d(TAG, "Opening Overlay Settings")
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        )
    }
}