package com.example.weatherwise

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.weatherwise.ui.screens.LoginScreen
import com.example.weatherwise.RegisterScreen
import com.example.weatherwise.ui.theme.WeatherWiseTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                // Create a NavController
                val navController = rememberNavController()

                // Define the navigation graph
                NavHost(navController = navController, startDestination = Screen.Welcome.route) {
                    composable(Screen.Login.route) {
                        LoginScreen(
                            onLoginClick = { _, _ ->
                                // Handle login logic (e.g., navigate to a home screen)
                            },
                            onGoogleSignInClick = {
                                // Handle Google Sign-In
                            },
                            onForgotPasswordClick = {
                                // Handle Forgot Password
                            }
                        )
                    }
                    composable(Screen.Register.route) {
                        RegisterScreen(onRegisterClick = { _, _ ->
                            // Handle registration logic (e.g., navigate to login)
                        })
                    }
                }
            }
        }
    }
}
// Define Routes
sealed class Screen(val route: String) {
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Register : Screen("register")
    object MFA : Screen("mfa")
    object VerifyMFA : Screen("verifyMFA")

}


