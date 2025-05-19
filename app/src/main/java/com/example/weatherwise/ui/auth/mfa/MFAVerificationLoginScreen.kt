package com.example.weatherwise.ui.auth.mfa

import android.app.Activity
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.weatherwise.ui.auth.AuthUiState
import com.example.weatherwise.ui.auth.AuthViewModel
import com.google.firebase.auth.PhoneMultiFactorInfo
import com.google.firebase.auth.PhoneMultiFactorGenerator



@Composable
fun MFAVerificationLoginScreen(
    phoneMfaViewModel: PhoneMfaViewModel,
    authViewModel: AuthViewModel,
    activity: Activity, // 如果需要重發驗證碼
    onVerificationSuccess: () -> Unit
) {
    val context = LocalContext.current
    // 監聽 AuthViewModel 的狀態，如果 MFA 登錄成功，則觸發回調
    LaunchedEffect(authViewModel.authUiState.value) {
        if (authViewModel.authUiState.value is AuthUiState.Success) {
            onVerificationSuccess()
        }
    }
    // 當 Composable 離開組合時，清除消息
    DisposableEffect(Unit) {
        onDispose {
            phoneMfaViewModel.resetMessages()
            authViewModel.resetAuthUiState() // 也重置 AuthViewModel 的狀態
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("MFA 驗證", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))

        // 您可以將 MFAVerificationScreen.kt 中的 Image 和 Text 佈局放在這裡
        // Image(painter = painterResource(id = R.drawable.lock), ...)
        Text("已向 ${phoneMfaViewModel.phoneNumberInput} 發送驗證碼。") // phoneNumberInput 應由登錄流程設置
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField( // 暫用 OutlinedTextField 替代 StyledTextField
            value = phoneMfaViewModel.smsCodeInput,
            onValueChange = { phoneMfaViewModel.smsCodeInput = it },
            label = { Text("輸入6位數驗證碼") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                val assertion = phoneMfaViewModel.getPhoneMultiFactorAssertionForLogin()
                if (assertion != null) {
                    authViewModel.completeMfaSignIn(assertion)
                } else {
                    // infoMessage 應該已在 getPhoneMultiFactorAssertionForLogin 中設置
                    // Toast.makeText(context, phoneMfaViewModel.infoMessage ?: "獲取斷言失敗", Toast.LENGTH_SHORT).show()
                }
            },
            enabled = !phoneMfaViewModel.isLoading && phoneMfaViewModel.smsCodeInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("驗證並登錄")
        }
        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                // 重新發送驗證碼的邏輯
                // 需要確保 authViewModel.mfaResolver 和 authViewModel.mfaHints 不為空
                val resolver = authViewModel.mfaResolver
                val phoneHint = authViewModel.mfaHints?.find { it.factorId == PhoneMultiFactorGenerator.FACTOR_ID } as? PhoneMultiFactorInfo
                if (resolver != null && phoneHint?.phoneNumber != null) {
                    phoneMfaViewModel.startPhoneNumberVerificationForLogin(activity, resolver.session,
                        phoneHint
                    )
                } else {
                    Toast.makeText(context, "無法重新發送驗證碼，請重新登錄", Toast.LENGTH_LONG).show()
                }
            },
            enabled = !phoneMfaViewModel.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("重新發送驗證碼")
        }

        if (phoneMfaViewModel.isLoading || authViewModel.authUiState.value is AuthUiState.Loading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
        }

        phoneMfaViewModel.infoMessage?.let { message ->
            Text(message, color = if (message.contains("失敗")) MaterialTheme.colorScheme.error else LocalContentColor.current)
        }
        // 也顯示來自 AuthViewModel 的錯誤信息
        val authStateValue = authViewModel.authUiState.value
        if (authStateValue is AuthUiState.Error) {
            Text(authStateValue.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

    }
}