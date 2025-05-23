package com.example.weatherwise // Or your correct package

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
import com.example.weatherwise.ui.navigation.Screen

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
    phoneMfaViewModel: PhoneMfaViewModel,
    navigateToLogin: () -> Unit // 新增回调
) {
    val navController = rememberNavController()
    val context = LocalContext.current

    NavHost(navController, startDestination = "main_weather_dashboard") {
        composable("main_weather_dashboard") {
            // userId 可以为 null (游客)
            WeatherMainPage(navController = navController, userId = authViewModel.currentUserId)
        }

        composable(
            route = "main_page/{lat}/{lon}",
            arguments = listOf(
                navArgument("lat") { type = NavType.StringType },
                navArgument("lon") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val lat = backStackEntry.arguments?.getString("lat")?.toDoubleOrNull() ?: 0.0
            val lon = backStackEntry.arguments?.getString("lon")?.toDoubleOrNull() ?: 0.0
            // userId 可以为 null (游客)
            WeatherMainPage(lat = lat, lon = lon, navController = navController, userId = authViewModel.currentUserId)
        }

        composable(Screen.Login.name) { // 添加 Login 路由以便从导航图内部跳转
            // MainActivity 的 when(currentScreen) 会处理实际的 LoginScreen 显示
            // 这里仅用于导航目标，实际UI由 MainActivity 控制
            LaunchedEffect(Unit) {
                navigateToLogin()
            }
        }

        composable("settings") {
            if (authViewModel.currentUserId == null) {
                Toast.makeText(context, "请登录后访问设置", Toast.LENGTH_SHORT).show()
                LaunchedEffect(Unit) {
                    // navController.navigate(Screen.Login.name) { // 使用枚举确保路由名称一致
                    //     popUpTo("settings") { inclusive = true }
                    // }
                    navigateToLogin() // 使用回调切换到登录界面
                    // 在 MainActivity 中， currentScreen 会被设置为 Screen.Login
                    // 此处 popUpTo 可能不再需要，因为 MainActivity 会重建 NavHost
                }
            } else {
                SettingsScreen(
                    navController = navController,
                    authViewModel = authViewModel,
                    navigateToMfaSetup = navigateToMfaSetup,
                    phoneMfaViewModel = phoneMfaViewModel
                )
            }
        }

        // 修改 city 路由，使其不直接依赖路径中的 userId，而是在 Composable 内部检查 AuthViewModel
        composable("city") { // 简化路由
            val currentUserId = authViewModel.currentUserId
            if (currentUserId == null) {
                Toast.makeText(context, "请登录后访问城市列表", Toast.LENGTH_SHORT).show()
                LaunchedEffect(Unit) {
                    // navController.navigate(Screen.Login.name) {
                    //    popUpTo("city") { inclusive = true }
                    // }
                    navigateToLogin()
                }
            } else {
                // 确保 CityScreen 接收一个非空的 userId
                CityScreen(navController = navController, userId = currentUserId)
            }
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
                apiKey = "79a25643aff43a2dbd3f03165be96f1a", // 示例 API Key
                onBack = { navController.popBackStack() }
            )
        }

        composable("about_app/{page_type}") { backStackEntry ->
            val pageType = backStackEntry.arguments?.getString("page_type") ?: "unknown"
            val title = pageType.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            AboutScreen(navController = navController, title = title)
        }
    }
}