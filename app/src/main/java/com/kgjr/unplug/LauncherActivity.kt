package com.kgjr.unplug

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.kgjr.unplug.navigation.destiantions.NavigationDestinations
import com.kgjr.unplug.utils.PermissionHelper

class LauncherActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val destination = if (PermissionHelper.hasAllPermissions(this)) {
            NavigationDestinations.homeMain
        } else {
            NavigationDestinations.permissionMain
        }

        startActivity(
            Intent(this, MainActivity::class.java)
                .putExtra("destination", destination)
        )
        finish()
    }
}