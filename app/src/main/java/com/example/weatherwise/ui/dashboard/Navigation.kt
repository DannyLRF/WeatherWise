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
    phoneMfaViewModel: PhoneMfaViewModel
) {
    val navController = rememberNavController()

    // Define the app's navigation graph
    NavHost(navController, startDestination = "main_weather_dashboard") // Initial screen when app starts
    {
        // Main weather dashboard page
        composable("main_weather_dashboard") {
            WeatherMainPage(navController = navController)
        }

        // City selection page
        composable(
            route = "main_page/{lat}/{lon}",
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType },
                navArgument("lon") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
            val lon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull() ?: 0.0
            WeatherMainPage(lat = lat, lon = lon, navController = navController)
        }

        // Settings screen, receives ViewModels and MFA setup callback
        composable("settings") {
            SettingsScreen(
                navController = navController,
                authViewModel = authViewModel,
                navigateToMfaSetup = navigateToMfaSetup,
                phoneMfaViewModel = phoneMfaViewModel  // passing to SettingsScreen
            )
        }

        // City selection screen
        composable("city") {
            CityScreen(navController = navController)
        }



        // 5-day forecast page with latitude and longitude parameters
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
        // About pages like Terms of Service or Privacy Policy
        composable("about_app/{page_type}") { backStackEntry ->
            val pageType = backStackEntry.arguments?.getString("page_type") ?: "unknown"

            // Ensure title transformation matches what AboutScreen expects or how it processes it
            // Format the title from the URL-friendly string
            val title = pageType.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            AboutScreen(navController = navController, title = title)
        }
    }
}