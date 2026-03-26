package com.byd.dilink.extras.prayer.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.byd.dilink.extras.prayer.ui.*

object PrayerRoutes {
    const val PRAYER_TIMES = "prayer_times"
    const val QIBLA = "qibla"
    const val TASBEEH = "tasbeeh"
    const val SETTINGS = "settings"
}

@Composable
fun PrayerNavHost() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = PrayerRoutes.PRAYER_TIMES) {
        composable(PrayerRoutes.PRAYER_TIMES) {
            PrayerTimesScreen(
                onNavigateToQibla = { navController.navigate(PrayerRoutes.QIBLA) },
                onNavigateToTasbeeh = { navController.navigate(PrayerRoutes.TASBEEH) },
                onNavigateToSettings = { navController.navigate(PrayerRoutes.SETTINGS) }
            )
        }
        composable(PrayerRoutes.QIBLA) {
            QiblaScreen(onBack = { navController.popBackStack() })
        }
        composable(PrayerRoutes.TASBEEH) {
            TasbeehScreen(onBack = { navController.popBackStack() })
        }
        composable(PrayerRoutes.SETTINGS) {
            PrayerSettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
