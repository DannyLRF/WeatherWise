package com.example.weatherwise.ui.auth

import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.weatherwise.AboutScreen
import com.example.weatherwise.CityPage
import com.example.weatherwise.FiveDayForecastPage
import com.example.weatherwise.SettingsScreen
import com.example.weatherwise.WeatherMainPage
import com.example.weatherwise.ui.auth.mfa.MFAVerificationLoginScreen
import com.example.weatherwise.ui.auth.mfa.PhoneMfaSetupScreen
import com.example.weatherwise.ui.auth.mfa.PhoneMfaViewModel
import com.example.weatherwise.ui.screens.LoginScreen
import com.google.firebase.auth.PhoneMultiFactorGenerator
import com.google.firebase.auth.PhoneMultiFactorInfo


@Composable
fun AppNavHost(
    authViewModel: AuthViewModel,
    phoneMfaViewModel: PhoneMfaViewModel,
    startDestination: String = "login"
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val authState by authViewModel.authUiState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    // Side effect to respond to auth state
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthUiState.Success -> {
                if (state.user != null) {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                    authViewModel.resetAuthUiState()
                    phoneMfaViewModel.resetMessages()
                }
            }

            is AuthUiState.Error -> {
                Toast.makeText(context, "錯誤: ${state.message}", Toast.LENGTH_LONG).show()
                authViewModel.resetAuthUiState()
                phoneMfaViewModel.resetMessages()
            }

            is AuthUiState.MfaRequired -> {
                val phoneHint = state.hints.find { it.factorId == PhoneMultiFactorGenerator.FACTOR_ID } as? PhoneMultiFactorInfo
                if (phoneHint != null && activity != null) {
                    phoneMfaViewModel.startPhoneNumberVerificationForLogin(activity, state.resolver.session, phoneHint)
                    navController.navigate("mfa_verify_login")
                } else {
                    Toast.makeText(context, "未找到電話 MFA 因素", Toast.LENGTH_SHORT).show()
                    authViewModel.logout()
                }
            }

            else -> {}
        }
    }

    NavHost(navController = navController, startDestination = startDestination) {
        composable("login") {
            LoginScreen(
                onLoginClick = { email, password -> authViewModel.login(email, password) },
                onGoogleSignInClick = { navController.navigate("register") },
                onForgotPasswordClick = {
                    Toast.makeText(context, "忘記密碼功能待實現", Toast.LENGTH_SHORT).show()
                }
            )
        }

        composable("register") {
            RegisterScreen(
                onRegisterClick = { email, password ->
                    authViewModel.register(email, password)
                }
            )
        }

        composable("home") {
            WeatherMainPage(navController)  // 假设进入天气主界面

        }

        composable("mfa_setup") {
            if (activity != null) {
                PhoneMfaSetupScreen(
                    phoneMfaViewModel = phoneMfaViewModel,
                    activity = activity,
                    onMfaSetupComplete = {
                        navController.navigate("home") {
                            popUpTo("mfa_setup") { inclusive = true }
                        }
                    }
                )
            }
        }

        composable("mfa_verify_login") {
            if (activity != null) {
                MFAVerificationLoginScreen(
                    phoneMfaViewModel = phoneMfaViewModel,
                    authViewModel = authViewModel,
                    activity = activity,
                    onVerificationSuccess = {
                        navController.navigate("home") {
                            popUpTo("mfa_verify_login") { inclusive = true }
                        }
                    }
                )
            }
        }

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