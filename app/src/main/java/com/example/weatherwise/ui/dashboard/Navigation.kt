
package com.example.weatherwise // Or your correct package

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
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

/**
 * Defines the app's navigation graph using Jetpack Navigation Compose.
 * Sets up routes for weather dashboard, city selection, settings, 5-day forecast, and about pages.
 *
 * @param authViewModel The authentication ViewModel for managing login/user state.
 * @param navigateToMfaSetup A callback to trigger MFA setup flow.
 * @param phoneMfaViewModel The ViewModel for managing phone MFA verification.
 */
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
        // Route: Main weather dashboard (default start screen)
        composable("main_weather_dashboard") {
            val context = LocalContext.current
            val userId = authViewModel.currentUserId

            if (userId != null) {
                DashboardPage(navController = navController, userId = userId)
            } else {
                // ✅ 弹出错误提示
                Toast.makeText(context, "Please log in to continue", Toast.LENGTH_SHORT).show()

                // ✅ 跳转到登录页
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo("main_weather_dashboard") { inclusive = true } // 可选：清除栈
                    }
                }
            }
        }

        // // Route: City selection page with parameters (lat, lon)
        composable(
            route = "main_page/{lat}/{lon}",
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType },
                navArgument("lon") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
            val lon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull() ?: 0.0
            val userId = authViewModel.currentUserId

            Log.d("Navigation", "Navigated to main_page with lat=$lat, lon=$lon")
            Log.d("Navigation", "Current userId = $userId")

            if (userId != null) {

                Log.d("Navigation", "Navigating to WeatherMainPage with userId=$userId")

                CityWeatherPage(lat = lat, lon = lon, navController = navController, userId = userId)
            } else {

                Log.w("Navigation", "userId is null, redirecting to login")

                Toast.makeText(LocalContext.current, "Please log in to view weather data.", Toast.LENGTH_SHORT).show()
                LaunchedEffect(Unit) {
                    navController.navigate("login") {
                        popUpTo("main_page/$lat/$lon") { inclusive = true } // 可选：移除非法页面
                    }
                }
            }
        }

        // // Route: Settings screen, receives ViewModels and MFA setup callback
        composable("settings") {
            SettingsScreen(
                navController = navController,
                authViewModel = authViewModel,
                navigateToMfaSetup = navigateToMfaSetup,
                phoneMfaViewModel = phoneMfaViewModel  // passing to SettingsScreen
            )
        }

        // Route: City selection screen
        composable(
            route = "city/{userId}",
            arguments = listOf(navArgument("userId") { type = NavType.StringType })
        ) { backStackEntry ->
            val userId = backStackEntry.arguments?.getString("userId") ?: ""
            CityScreen(navController = navController, userId = userId)
        }

        // Route: 5-day forecast page, navigated to with lat/lon parameters
        composable(
            route = "five_day_forecast/{lat}/{lon}",
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType },
                navArgument("lon") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
            val lon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull() ?: 0.0

            // Launch the 5-day forecast page with back button handler
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