package com.byd.dilink.extras.hazard.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.byd.dilink.extras.hazard.ui.HazardListScreen
import com.byd.dilink.extras.hazard.ui.HazardSettingsScreen
import com.byd.dilink.extras.hazard.ui.LiveDriveScreen
import com.byd.dilink.extras.hazard.ui.RouteHazardsScreen

object HazardRoutes {
    const val LIVE_DRIVE = "live_drive"
    const val HAZARD_LIST = "hazard_list"
    const val ROUTE_HAZARDS = "route_hazards"
    const val SETTINGS = "settings"
}

@Composable
fun HazardNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = HazardRoutes.LIVE_DRIVE
    ) {
        composable(HazardRoutes.LIVE_DRIVE) {
            LiveDriveScreen(
                onNavigateToList = { navController.navigate(HazardRoutes.HAZARD_LIST) },
                onNavigateToRoute = { navController.navigate(HazardRoutes.ROUTE_HAZARDS) },
                onNavigateToSettings = { navController.navigate(HazardRoutes.SETTINGS) }
            )
        }
        composable(HazardRoutes.HAZARD_LIST) {
            HazardListScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(HazardRoutes.ROUTE_HAZARDS) {
            RouteHazardsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(HazardRoutes.SETTINGS) {
            HazardSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
