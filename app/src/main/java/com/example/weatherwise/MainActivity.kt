package com.example.weatherwise

// 導入您上傳的 Composable 函數 (請確保包名正確)
// MFA 相關的 UI 界面，我們稍後會用到您上傳的版本
// import com.example.weatherwise.ui.auth.MFASetupScreen
// import com.example.weatherwise.ui.auth.MFAVerificationScreen

import androidx.activity.compose.LocalActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.weatherwise.data.repository.AuthRepository
import com.example.weatherwise.ui.auth.AuthUiState
import com.example.weatherwise.ui.auth.AuthViewModel
import com.example.weatherwise.ui.auth.RegisterScreen
import com.example.weatherwise.ui.auth.mfa.PhoneMfaViewModel
import com.example.weatherwise.ui.navigation.Screen
import com.example.weatherwise.ui.screens.LoginScreen
import com.example.weatherwise.ui.theme.WeatherWiseTheme
import com.google.firebase.auth.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val firebaseAuth = FirebaseAuth.getInstance()
        val authRepository = AuthRepository(firebaseAuth)
        val authViewModelFactory = AuthViewModel.provideFactory(authRepository)
        // PhoneMfaViewModel 不需要 Factory，因為它沒有構造函數參數

        setContent {
            WeatherWiseTheme(darkTheme = true) {
                val authViewModel: AuthViewModel = viewModel(factory = authViewModelFactory)
                val phoneMfaViewModel: PhoneMfaViewModel = viewModel() // 直接獲取實例

                val currentUser by authViewModel.currentUser.collectAsState()
                val authState by authViewModel.authUiState.collectAsState()
                val context = LocalContext.current
                val activity = LocalActivity.current // 使用 LocalActivity 獲取當前 Activity

                var currentScreen by remember { mutableStateOf(if (currentUser == null) Screen.Login else Screen.WeatherHome) }

                // 處理認證狀態的副作用
                LaunchedEffect(authState) {
                    when (val state = authState) {
                        is AuthUiState.Success -> {
                            if (state.user != null) {
                                currentScreen = Screen.WeatherHome
                                Toast.makeText(context, "操作成功", Toast.LENGTH_SHORT).show()
                                authViewModel.resetAuthUiState()
                                phoneMfaViewModel.resetMessages() // 重置 MFA ViewModel 的消息
                            }
                        }
                        is AuthUiState.Error -> {
                            Toast.makeText(context, "錯誤: ${state.message}", Toast.LENGTH_LONG).show()
                            authViewModel.resetAuthUiState()
                            phoneMfaViewModel.resetMessages()
                        }
                        is AuthUiState.MfaRequired -> {
                            // 當需要 MFA 時，導航到 MFA 驗證界面
                            // 我們需要將 resolver 和 hints 傳遞給 MFA 驗證流程
                            val phoneHint = state.hints.find { it.factorId == PhoneMultiFactorGenerator.FACTOR_ID } as? PhoneMultiFactorInfo
                            if (phoneHint != null) {
                                activity?.let { nonNullActivity ->
                                    phoneMfaViewModel.startPhoneNumberVerificationForLogin(
                                        nonNullActivity,
                                        state.resolver.session,
                                        phoneHint
                                    )
                                    currentScreen = Screen.MfaVerifyLogin
                                } ?: run {
                                    // 處理 activity 為空的情況
                                    Toast.makeText(context, "無法獲取 Activity 實例", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(context, "未找到可用的電話 MFA 因素", Toast.LENGTH_LONG).show()
                                authViewModel.logout() // 如果無法進行MFA，則登出
                            }
                            // 不需要重置 authUiState，因為我們還在 MFA 流程中
                        }
                        is AuthUiState.Loading -> { /* 可以顯示全局加載指示器 */ }
                        AuthUiState.Idle -> { /* 初始狀態或重置後的狀態 */ }
                    }
                }

                LaunchedEffect(currentUser) {
                    currentScreen = if (currentUser == null) {
                        if (currentScreen != Screen.Register && currentScreen != Screen.MfaVerifyLogin) Screen.Login else currentScreen
                    } else {
                        // 如果是從 MFA 驗證成功過來的，currentUser 會更新，此時應跳到主頁
                        if (currentScreen == Screen.MfaVerifyLogin) Screen.WeatherHome else currentScreen
                    }
                }

                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    when (currentScreen) {
                        Screen.Login -> {
                            LoginScreen(
                                onLoginClick = { email, password ->
                                    authViewModel.login(email, password)
                                },
                                onGoogleSignInClick = { currentScreen = Screen.Register },
                                onForgotPasswordClick = { Toast.makeText(context, "忘記密碼功能待實現", Toast.LENGTH_SHORT).show() }
                            )
                        }
                        Screen.Register -> {
                            RegisterScreen(
                                onRegisterClick = { email, password ->
                                    authViewModel.register(email, password)
                                    // 註冊成功後，如果直接登錄，LaunchedEffect(authState) 會處理跳轉
                                    // 如果註冊後需要郵箱驗證或其他步驟，這裡的邏輯可能需要調整
                                }
                            )
                        }
                        Screen.WeatherHome -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("天氣主頁 (用戶: ${currentUser?.email ?: "未知"})", style = MaterialTheme.typography.headlineMedium)
                                Spacer(modifier = Modifier.height(20.dp))
                                Button(onClick = { authViewModel.logout() }) {
                                    Text("登出")
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(onClick = {
                                    phoneMfaViewModel.resetMessages() // 清除舊消息
                                    currentScreen = Screen.MfaSetup
                                }) {
                                    Text("設定/管理 電話 MFA")
                                }
                            }
                        }
                        Screen.MfaSetup -> {
                            // 這裡使用 PhoneMfaSetupScreen，它內部使用 PhoneMfaViewModel
                            activity?.let { nonNullActivity ->
                                com.example.weatherwise.ui.auth.mfa.PhoneMfaSetupScreen(
                                    phoneMfaViewModel = phoneMfaViewModel,
                                    activity = nonNullActivity,
                                    onMfaSetupComplete = { // 添加一個回調，以便成功設置後返回主頁
                                        currentScreen = Screen.WeatherHome
                                        Toast.makeText(context, "MFA 設置已更新", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            } ?: run {
                                Text("無法獲取 Activity 實例，請稍後再試")
                            }
                        }
                        Screen.MfaVerifyLogin -> {
                            // 這裡使用 MFAVerificationScreen，它需要與 PhoneMfaViewModel 和 AuthViewModel 配合
                            activity?.let { nonNullActivity ->
                                com.example.weatherwise.ui.auth.mfa.MFAVerificationLoginScreen(
                                    phoneMfaViewModel = phoneMfaViewModel,
                                    authViewModel = authViewModel,
                                    activity = nonNullActivity,
                                    onVerificationSuccess = {
                                        currentScreen = Screen.WeatherHome // 驗證成功後跳轉
                                    }
                                )
                            } ?: run {
                                Text("無法獲取 Activity 實例，請稍後再試")
                            }
                        }
                    }
                }
            }
        }
    }
}