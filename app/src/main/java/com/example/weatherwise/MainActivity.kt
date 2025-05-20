package com.example.weatherwise

// 導入您上傳的 Composable 函數 (請確保包名正確)
// MFA 相關的 UI 界面，我們稍後會用到您上傳的版本
// import com.example.weatherwise.ui.auth.MFASetupScreen
// import com.example.weatherwise.ui.auth.MFAVerificationScreen

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.LocalActivity // Keep this import
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weatherwise.data.repository.AuthRepository
import com.example.weatherwise.ui.auth.AuthUiState
import com.example.weatherwise.ui.auth.AuthViewModel
import com.example.weatherwise.ui.auth.RegisterScreen
import com.example.weatherwise.ui.auth.mfa.PhoneMfaViewModel
import com.example.weatherwise.ui.navigation.Screen // Your existing Screen enum
import com.example.weatherwise.ui.screens.LoginScreen // Your existing LoginScreen
import com.example.weatherwise.ui.theme.WeatherWiseTheme
import com.google.firebase.auth.*
// Import the updated WeatherAppNavigation
// Make sure the package name matches where you placed WeatherAppNavigation
// e.g., import com.example.weatherwise.ui.dashboard.WeatherAppNavigation or just com.example.weatherwise.WeatherAppNavigation


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val firebaseAuth = FirebaseAuth.getInstance()
        val authRepository = AuthRepository(firebaseAuth)
        val authViewModelFactory = AuthViewModel.provideFactory(authRepository)

        setContent {
            WeatherWiseTheme(darkTheme = true) {
                val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)
                val phoneMfaViewModel: PhoneMfaViewModel = viewModel()

                val currentUser by authViewModel.currentUser.collectAsState()
                val authState by authViewModel.authUiState.collectAsState()
                val context = LocalContext.current
                val activity = LocalActivity.current

                // currentScreen determines the top-level view (Auth vs. Main App)
                var currentScreen by remember {
                    mutableStateOf(if (currentUser == null) Screen.Login else Screen.WeatherHome)
                }

                LaunchedEffect(authState) {
                    when (val state = authState) {
                        is AuthUiState.Success -> {
                            if (state.user != null) {
                                // On successful login/MFA, go to WeatherHome (which now hosts WeatherAppNavigation)
                                currentScreen = Screen.WeatherHome
                                Toast.makeText(context, "操作成功", Toast.LENGTH_SHORT).show()
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
                            if (phoneHint != null) {
                                activity?.let { nonNullActivity ->
                                    phoneMfaViewModel.startPhoneNumberVerificationForLogin(
                                        nonNullActivity,
                                        state.resolver.session,
                                        phoneHint
                                    )
                                    currentScreen = Screen.MfaVerifyLogin
                                } ?: Toast.makeText(context, "無法獲取 Activity 實例", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "未找到可用的電話 MFA 因素", Toast.LENGTH_LONG).show()
                                authViewModel.logout()
                            }
                        }
                        is AuthUiState.Loading -> { /* Global loading indicator could be here */ }
                        AuthUiState.Idle -> { /* Initial or reset state */ }
                    }
                }

                LaunchedEffect(currentUser) {
                    currentScreen = if (currentUser == null) {
                        // If logged out, or never logged in, show Login or Register screen
                        if (currentScreen != Screen.Register && currentScreen != Screen.MfaVerifyLogin) {
                            Screen.Login
                        } else {
                            currentScreen // Remain on Register or MfaVerifyLogin if already there
                        }
                    } else {
                        // If user exists (logged in), show the main app content
                        Screen.WeatherHome
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (currentScreen) {
                        Screen.Login -> {
                            LoginScreen(
                                onLoginClick = { email, password ->
                                    authViewModel.login(email, password)
                                },
                                onGoogleSignInClick = { currentScreen = Screen.Register /* or actual Google Sign-In logic */ },
                                onForgotPasswordClick = { Toast.makeText(context, "忘記密碼功能待實現", Toast.LENGTH_SHORT).show() }
                            )
                        }
                        Screen.Register -> {
                            RegisterScreen(
                                onRegisterClick = { email, password ->
                                    authViewModel.register(email, password)
                                }
                            )
                        }
                        Screen.WeatherHome -> {
                            // WeatherHome now hosts the main application navigation graph
                            WeatherAppNavigation(
                                authViewModel = authViewModel,
                                navigateToMfaSetup = {
                                    phoneMfaViewModel.resetMessages() // Reset messages before navigating
                                    currentScreen = Screen.MfaSetup
                                }
                            )
                        }
                        Screen.MfaSetup -> {
                            activity?.let { nonNullActivity ->
                                com.example.weatherwise.ui.auth.mfa.PhoneMfaSetupScreen(
                                    phoneMfaViewModel = phoneMfaViewModel,
                                    activity = nonNullActivity,
                                    onMfaSetupComplete = {
                                        currentScreen = Screen.WeatherHome // Return to main app content
                                        Toast.makeText(context, "MFA 設置已更新", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } ?: Text("無法獲取 Activity 實例，請稍後再試")
                        }
                        Screen.MfaVerifyLogin -> {
                            activity?.let { nonNullActivity ->
                                com.example.weatherwise.ui.auth.mfa.MFAVerificationLoginScreen(
                                    phoneMfaViewModel = phoneMfaViewModel,
                                    authViewModel = authViewModel,
                                    activity = nonNullActivity,
                                    onVerificationSuccess = {
                                        // AuthState.Success effect will handle navigation to WeatherHome
                                    }
                                )
                            } ?: Text("無法獲取 Activity 實例，請稍後再試")
                        }
                        // Handle other screens if any, or add a default case
                    }
                }
            }
        }
    }
}