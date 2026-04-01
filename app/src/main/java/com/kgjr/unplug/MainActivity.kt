package com.kgjr.unplug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import com.kgjr.unplug.screen.BlockScreen
import com.kgjr.unplug.ui.screens.PermissionScreen
import com.kgjr.unplug.ui.theme.UnplugTheme
import com.kgjr.unplug.utils.PermissionHelper

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            UnplugTheme {
                var allPermissionsGranted by rememberSaveable {
                    mutableStateOf(PermissionHelper.hasAllPermissions(this@MainActivity))
                }

                if (allPermissionsGranted) {
                    BlockScreen()
                } else {
                    PermissionScreen(
                        onPermissionsGranted = {
                            allPermissionsGranted = PermissionHelper.hasAllPermissions(this@MainActivity)
                        }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Re-check permissions when returning from settings
        setContent {
            UnplugTheme {
                var allPermissionsGranted by rememberSaveable {
                    mutableStateOf(PermissionHelper.hasAllPermissions(this@MainActivity))
                }

                if (allPermissionsGranted) {
                    BlockScreen()
                } else {
                    PermissionScreen(
                        onPermissionsGranted = {
                            allPermissionsGranted = PermissionHelper.hasAllPermissions(this@MainActivity)
                        }
                    )
                }
            }
        }
    }
}