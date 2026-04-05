package com.kgjr.unplug.navigation.graph.subgraph

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.kgjr.unplug.navigation.destiantions.NavigationDestinations
import com.kgjr.unplug.ui.screens.PermissionScreen

fun NavGraphBuilder.permissionGraph(navController: NavController) {
    navigation(
        route = NavigationDestinations.permissionMain,
        startDestination = NavigationDestinations.permissionScreen
    ) {
        composable(NavigationDestinations.permissionScreen) {
            PermissionScreen(
                onPermissionsGranted = {
                    navController.navigate(NavigationDestinations.homeMain) {
                        popUpTo(NavigationDestinations.permissionMain) { inclusive = true }
                    }
                }
            )
        }
    }
}