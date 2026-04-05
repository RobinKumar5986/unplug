package com.kgjr.unplug.navigation.graph.subgraph

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import com.kgjr.unplug.navigation.destiantions.NavigationDestinations
import com.kgjr.unplug.screen.BlockScreen

fun NavGraphBuilder.homeGraph(navController: NavController) {
    navigation(
        route = NavigationDestinations.homeMain,
        startDestination = NavigationDestinations.homeScreen
    ) {
        composable(NavigationDestinations.homeScreen) {
            BlockScreen()
        }
    }
}