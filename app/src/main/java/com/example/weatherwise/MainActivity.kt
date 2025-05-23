package com.example.weatherwise

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
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


class MainActivity : ComponentActivity() {
    @SuppressLint("StateFlowValueCalledInComposition")
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
                    mutableStateOf(Screen.WeatherHome) // 修改这里
                }

                var navigatedToLoginFromProtectedRoute by remember { mutableStateOf(false) }


                val navigateToLoginAction = { fromProtectedRoute: Boolean ->
                    navigatedToLoginFromProtectedRoute = fromProtectedRoute
                    currentScreen = Screen.Login
                }

                LaunchedEffect(authState) {
                    when (val state = authState) {
                        is AuthUiState.Success -> {
                            if (state.user != null) {
                                currentScreen = Screen.WeatherHome
                                navigatedToLoginFromProtectedRoute = false // 登录成功后重置标志
                                Toast.makeText(context, "successful", Toast.LENGTH_SHORT).show()
                                authViewModel.resetAuthUiState()
                                phoneMfaViewModel.resetInputState()
                            }
                        }
                        is AuthUiState.Error -> { //确保Error时重置标志
                            Toast.makeText(context, "error: ${state.message}", Toast.LENGTH_LONG).show()
                            authViewModel.resetAuthUiState()
                            phoneMfaViewModel.resetMessages()
                            // 如果是从受保护路由来的，并且登录失败，用户可能想返回，此时 navigatedToLoginFromProtectedRoute 保持
                        }
                        is AuthUiState.MfaRequired -> {
                            navigatedToLoginFromProtectedRoute = true
                            val phoneHint = state.hints.find { it.factorId == PhoneMultiFactorGenerator.FACTOR_ID } as? PhoneMultiFactorInfo
                            if (phoneHint != null) {
                                activity?.let { nonNullActivity ->
                                    phoneMfaViewModel.startPhoneNumberVerificationForLogin(
                                        nonNullActivity,
                                        state.resolver.session,
                                        phoneHint
                                    )
                                    currentScreen = Screen.MfaVerifyLogin
                                } ?: Toast.makeText(context, "can't retrieve Activity instance", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "can't find available MFA", Toast.LENGTH_LONG).show()
                                authViewModel.logout() // 登出以清理状态
                            }
                        }
                        is AuthUiState.Loading -> { /* Global loading indicator could be here */ }
                        AuthUiState.Idle -> { /* Initial or reset state */ }
                    }
                }

                LaunchedEffect(currentUser) {
                    if (currentUser == null) {
                        if (currentScreen != Screen.Login &&
                            currentScreen != Screen.Register &&
                            currentScreen != Screen.MfaVerifyLogin &&
                            currentScreen != Screen.MfaSetup) {
                            // 如果用户登出且当前不在认证屏幕，则保持在 WeatherHome (游客)
                            // 或者，如果需要强制跳登录，则 currentScreen = Screen.Login
                            // 根据当前需求，保持 WeatherHome 游客模式
                            if (!navigatedToLoginFromProtectedRoute) { // 仅当不是因为强制跳转时才去WeatherHome
                                currentScreen = Screen.WeatherHome
                            } else {
                                // 如果是因为访问受保护页面而到这里的（比如登出后），则应该去Login
                                currentScreen = Screen.Login
                            }
                        }
                    } else { // currentUser is not null
                        currentScreen = Screen.WeatherHome
                        navigatedToLoginFromProtectedRoute = false // 已登录，重置标志
                    }
                }


                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (currentScreen) {
                        Screen.Login -> {
                            BackHandler(enabled = navigatedToLoginFromProtectedRoute) {
                                // 如果是从受保护路由重定向来的，按返回键则回到 WeatherHome
                                currentScreen = Screen.WeatherHome
                                navigatedToLoginFromProtectedRoute = false // 重置标志
                            }
                            LoginScreen(
                                onLoginClick = { email, password ->
                                    authViewModel.login(email, password)
                                },
                                onGoogleSignInClick = {
                                    currentScreen = Screen.Register
                                    // navigatedToLoginFromProtectedRoute = false; // 转到注册页，不算直接从登录返回
                                    Toast.makeText(context, "Google 登錄待實現", Toast.LENGTH_SHORT).show()
                                },
                                onForgotPasswordClick = { Toast.makeText(context, "忘記密碼功能待實現", Toast.LENGTH_SHORT).show() }
                            )
                        }
                        Screen.Register -> {
                            BackHandler { // 从注册页总是可以返回到登录页（如果适用）或主页
                                if (navigatedToLoginFromProtectedRoute) { // 如果之前是从受保护页导向登录再到注册
                                    currentScreen = Screen.Login // 返回到登录页
                                } else {
                                    currentScreen = Screen.WeatherHome // 否则返回主页
                                }
                            }
                            RegisterScreen(
                                onRegisterClick = { email, password ->
                                    authViewModel.register(email, password)
                                    // 注册成功后 AuthState.Success 会处理导航
                                }
                            )
                        }
                        Screen.WeatherHome -> {
                            WeatherAppNavigation(
                                authViewModel = authViewModel,
                                navigateToMfaSetup = {
                                    if (authViewModel.currentUser.value != null) {
                                        phoneMfaViewModel.resetMessages()
                                        navigatedToLoginFromProtectedRoute = true // 去MFA设置也算保护流程
                                        currentScreen = Screen.MfaSetup
                                    } else {
                                        Toast.makeText(context, "请先登录再设置 MFA", Toast.LENGTH_SHORT).show()
                                        navigateToLoginAction(true)
                                    }
                                },
                                phoneMfaViewModel = phoneMfaViewModel,
                                navigateToLogin = { navigateToLoginAction(true) } // 传递 true
                            )
                        }
                        Screen.MfaSetup -> {
                            BackHandler { // 从MFA设置页返回
                                currentScreen = Screen.WeatherHome
                                navigatedToLoginFromProtectedRoute = false
                            }
                            if (authViewModel.currentUser.value != null) {
                                activity?.let { nonNullActivity ->
                                    com.example.weatherwise.ui.auth.mfa.PhoneMfaSetupScreen(
                                        phoneMfaViewModel = phoneMfaViewModel,
                                        activity = nonNullActivity,
                                        onMfaSetupComplete = {
                                            currentScreen = Screen.WeatherHome
                                            navigatedToLoginFromProtectedRoute = false
                                            Toast.makeText(context, "MFA 設置已更新", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } ?: Text("无法获取 Activity 实例，请稍后再试")
                            } else {
                                LaunchedEffect(Unit) { navigateToLoginAction(true) }
                                Text("请先登录。正在跳转到登录页面...")
                            }
                        }
                        Screen.MfaVerifyLogin -> {
                            BackHandler { // 从MFA验证页返回到登录页
                                authViewModel.logout() // 清理MFA状态
                                currentScreen = Screen.Login
                                navigatedToLoginFromProtectedRoute = true // 保持是在登录流程中
                            }
                            activity?.let { nonNullActivity ->
                                com.example.weatherwise.ui.auth.mfa.MFAVerificationLoginScreen(
                                    phoneMfaViewModel = phoneMfaViewModel,
                                    authViewModel = authViewModel,
                                    activity = nonNullActivity,
                                    onVerificationSuccess = {
                                        // AuthState.Success effect will handle navigation
                                    }
                                )
                            } ?: Text("无法获取 Activity 实例，请稍后再试")
                        }
                    }
                }
            }
        }
    }
}