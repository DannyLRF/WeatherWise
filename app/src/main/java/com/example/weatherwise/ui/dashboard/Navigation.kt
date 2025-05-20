package com.example.weatherwise

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.weatherwise.SettingsScreen

@Composable
fun WeatherAppNav() {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "main") {
        composable("main") { WeatherMainPage(navController) }
        composable("city") { CityPage() }
        composable("settings") { SettingsScreen(navController = navController) }
        composable(
            route = "five_day_forecast/{lat}/{lon}",
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType },
                navArgument("lon") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
            val lon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull() ?: 0.0
            FiveDayForecastPage(
                lat = lat,
                lon = lon,
                apiKey = "3a936acc8bb109dcb94017abbc0ec0fb",
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = "about/{pageTitle}",
            arguments = listOf(navArgument("pageTitle") { type = NavType.StringType })
        ) { backStackEntry ->
            val title = backStackEntry.arguments?.getString("pageTitle") ?: "About"
            AboutScreen(navController = navController, title = title)
        }
    }
}