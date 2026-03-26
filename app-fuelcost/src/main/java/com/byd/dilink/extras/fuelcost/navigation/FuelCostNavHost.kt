package com.byd.dilink.extras.fuelcost.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.byd.dilink.extras.fuelcost.ui.*

object FuelCostRoutes {
    const val DASHBOARD = "dashboard"
    const val LOG_FUEL = "log_fuel"
    const val LOG_CHARGE = "log_charge"
    const val STATISTICS = "statistics"
    const val SETTINGS = "settings"
}

@Composable
fun FuelCostNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = FuelCostRoutes.DASHBOARD
    ) {
        composable(FuelCostRoutes.DASHBOARD) {
            DashboardScreen(
                onNavigateToLogFuel = { navController.navigate(FuelCostRoutes.LOG_FUEL) },
                onNavigateToLogCharge = { navController.navigate(FuelCostRoutes.LOG_CHARGE) },
                onNavigateToStatistics = { navController.navigate(FuelCostRoutes.STATISTICS) },
                onNavigateToSettings = { navController.navigate(FuelCostRoutes.SETTINGS) }
            )
        }
        composable(FuelCostRoutes.LOG_FUEL) {
            LogFuelScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(FuelCostRoutes.LOG_CHARGE) {
            LogChargeScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(FuelCostRoutes.STATISTICS) {
            StatisticsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(FuelCostRoutes.SETTINGS) {
            FuelSettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
