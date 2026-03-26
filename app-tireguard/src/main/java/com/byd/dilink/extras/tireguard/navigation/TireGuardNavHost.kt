package com.byd.dilink.extras.tireguard.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.byd.dilink.extras.tireguard.ui.*

object TireGuardRoutes {
    const val OVERVIEW = "overview"
    const val LOG_TIRES = "log_tires"
    const val LOG_BATTERY = "log_battery"
    const val TIRE_HISTORY = "tire_history/{position}"
    const val BATTERY_HISTORY = "battery_history"
    const val ROTATION_TRACKER = "rotation_tracker"
    const val SETTINGS = "settings"

    fun tireHistory(position: String = "all") = "tire_history/$position"
}

@Composable
fun TireGuardNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = TireGuardRoutes.OVERVIEW) {
        composable(TireGuardRoutes.OVERVIEW) {
            OverviewScreen(
                onNavigateToLogTires = { navController.navigate(TireGuardRoutes.LOG_TIRES) },
                onNavigateToLogBattery = { navController.navigate(TireGuardRoutes.LOG_BATTERY) },
                onNavigateToTireHistory = { position ->
                    navController.navigate(TireGuardRoutes.tireHistory(position))
                },
                onNavigateToBatteryHistory = {
                    navController.navigate(TireGuardRoutes.BATTERY_HISTORY)
                },
                onNavigateToRotationTracker = {
                    navController.navigate(TireGuardRoutes.ROTATION_TRACKER)
                },
                onNavigateToSettings = {
                    navController.navigate(TireGuardRoutes.SETTINGS)
                }
            )
        }

        composable(TireGuardRoutes.LOG_TIRES) {
            LogTiresScreen(onBack = { navController.popBackStack() })
        }

        composable(TireGuardRoutes.LOG_BATTERY) {
            LogBatteryScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = TireGuardRoutes.TIRE_HISTORY,
            arguments = listOf(navArgument("position") {
                type = NavType.StringType
                defaultValue = "all"
            })
        ) { backStackEntry ->
            val position = backStackEntry.arguments?.getString("position") ?: "all"
            TireHistoryScreen(
                initialPosition = position,
                onBack = { navController.popBackStack() }
            )
        }

        composable(TireGuardRoutes.BATTERY_HISTORY) {
            BatteryHistoryScreen(onBack = { navController.popBackStack() })
        }

        composable(TireGuardRoutes.ROTATION_TRACKER) {
            RotationTrackerScreen(onBack = { navController.popBackStack() })
        }

        composable(TireGuardRoutes.SETTINGS) {
            TireSettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
