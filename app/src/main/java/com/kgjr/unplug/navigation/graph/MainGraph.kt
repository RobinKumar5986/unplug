package com.kgjr.unplug.navigation.graph

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.kgjr.unplug.navigation.graph.subgraph.homeGraph
import com.kgjr.unplug.navigation.graph.subgraph.permissionGraph

@Composable
fun MainGraph(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        permissionGraph(navController)
        homeGraph(navController)
    }
}