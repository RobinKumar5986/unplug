package com.kgjr.unplug.utils

import android.app.AppOpsManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.util.Log

object PermissionHelper {
    private const val TAG = "PermissionHelper"

    fun hasAccessibilityPermission(context: Context): Boolean {
        val service = "${context.packageName}/${com.kgjr.unplug.service.UnplugAccessibilityService::class.java.canonicalName}"
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: ""
        return enabledServices.contains(service).also {
            Log.d(TAG, "Accessibility granted=$it")
        }
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
        return (mode == AppOpsManager.MODE_ALLOWED).also {
            Log.d(TAG, "UsageStats granted=$it")
        }
    }

    fun hasOverlayPermission(context: Context): Boolean =
        Settings.canDrawOverlays(context).also {
            Log.d(TAG, "Overlay granted=$it")
        }

    fun hasBatteryOptimizationIgnored(context: Context): Boolean {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName).also {
            Log.d(TAG, "BatteryOptimizationIgnored=$it")
        }
    }

    fun hasAllPermissions(context: Context): Boolean =
        hasAccessibilityPermission(context) &&
                hasUsageStatsPermission(context) &&
                hasOverlayPermission(context) &&
                hasBatteryOptimizationIgnored(context)


    fun openAccessibilitySettings(context: Context) {
        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun openUsageStatsSettings(context: Context) {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            intent.data = null
            context.startActivity(intent)
        }
    }

    fun openOverlaySettings(context: Context) {
        val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.fromParts("package", context.packageName, null)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            intent.data = null
            context.startActivity(intent)
        }
    }

    fun openBatteryOptimizationSettings(context: Context) {
        // Try direct exemption request first — takes user straight to the dialog
        try {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback: open the general battery optimization list
            Log.w(TAG, "Direct battery exemption dialog unavailable, opening settings list")
            context.startActivity(
                Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
    }
}