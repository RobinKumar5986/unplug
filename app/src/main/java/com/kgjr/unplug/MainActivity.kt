package com.kgjr.unplug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.kgjr.unplug.navigation.destiantions.NavigationDestinations
import com.kgjr.unplug.navigation.graph.MainGraph

import com.kgjr.unplug.ui.theme.UnplugTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val destination = intent.getStringExtra("destination")
            ?: NavigationDestinations.permissionMain

        setContent {
            UnplugTheme {
                val navController = rememberNavController()
                MainGraph(navController = navController, startDestination = destination)
            }
        }
    }
}