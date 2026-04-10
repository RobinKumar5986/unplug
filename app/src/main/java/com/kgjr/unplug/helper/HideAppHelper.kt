package com.kgjr.unplug.helper

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

object HideAppHelper {
    fun hideAppIconPermanently(context: Context) {
        context.packageManager.setComponentEnabledSetting(
            ComponentName(context, "com.kgjr.unplug.LauncherAlias"),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )
    }
}