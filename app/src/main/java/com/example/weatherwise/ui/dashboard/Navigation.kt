package com.example.weatherwise // Or your correct package

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.weatherwise.ui.auth.AuthViewModel
import com.example.weatherwise.ui.auth.mfa.PhoneMfaViewModel

// Make sure all necessary screen composables are imported
// e.g., import com.example.weatherwise.WeatherMainPage, com.example.weatherwise.CityPage, etc.
// import com.example.weatherwise.SettingsScreen
// import com.example.weatherwise.FiveDayForecastPage
// import com.example.weatherwise.AboutScreen


@Composable
fun WeatherAppNavigation(
    authViewModel: AuthViewModel,
    navigateToMfaSetup: () -> Unit,
    phoneMfaViewModel: PhoneMfaViewModel  // 新增這個參數
) {
    val navController = rememberNavController()
    NavHost(navController, startDestination = "main_weather_dashboard") {
        composable("main_weather_dashboard") {
            WeatherMainPage(navController = navController)
        }
        // 其他 composable 保持不變...

        composable("settings") {
            SettingsScreen(
                navController = navController,
                authViewModel = authViewModel,
                navigateToMfaSetup = navigateToMfaSetup,
                phoneMfaViewModel = phoneMfaViewModel  // 傳遞給 SettingsScreen
            )
        }
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
                apiKey = "3a936acc8bb109dcb94017abbc0ec0fb", // Reminder: Secure API key
                onBack = { navController.popBackStack() }
            )
        }
        // This route was "about_app/{page_type}" and SettingsScreen navigated to "about_app/$routeSegment"
        // If $routeSegment can be "terms_of_service", then page_type is appropriate.
        composable("about_app/{page_type}") { backStackEntry ->
            val pageType = backStackEntry.arguments?.getString("page_type") ?: "unknown"
            // Ensure title transformation matches what AboutScreen expects or how it processes it
            val title = pageType.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            AboutScreen(navController = navController, title = title)
        }
    }
}