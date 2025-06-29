package com.example.weatherwise.ui.auth.mfa

// 導入您上傳的 Composable 函數 (請確保包名正確)
// MFA 相關的 UI 界面，我們稍後會用到您上傳的版本
// import com.example.weatherwise.ui.auth.MFASetupScreen
// import com.example.weatherwise.ui.auth.MFAVerificationScreen
import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth

// import com.example.weatherwise.R // 確保 R 文件被正確導入，如果您的 UI 中使用了 R.drawable 等

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneMfaSetupScreen(
    phoneMfaViewModel: PhoneMfaViewModel,
    activity: Activity,
    onMfaSetupComplete: () -> Unit
) {
    val currentUser = FirebaseAuth.getInstance().currentUser
    LocalContext.current

    // 添加 LaunchedEffect 在屏幕顯示時檢查當前用戶狀態
    LaunchedEffect(Unit) {
        phoneMfaViewModel.checkCurrentUserMfaStatus()
    }

    // 監聽 isMfaSuccessfullySetup 的變化，以便在設置/禁用成功後執行回調
    LaunchedEffect(phoneMfaViewModel.isMfaSuccessfullySetup, phoneMfaViewModel.infoMessage) {
        if (phoneMfaViewModel.infoMessage?.contains("enabled") == true ||
            phoneMfaViewModel.infoMessage?.contains("disabled") == true) {
            // 只有在明確成功或禁用時才觸發完成回調
            // 避免在僅加載狀態時就跳轉
            if(phoneMfaViewModel.isMfaSuccessfullySetup || phoneMfaViewModel.infoMessage?.contains("disabled") == true) {
                onMfaSetupComplete()
            }
        }
    }
    // 當 Composable 離開組合時，清除消息，避免下次進入時顯示舊消息
    DisposableEffect(Unit) {
        onDispose {
            phoneMfaViewModel.resetInputState()
        }
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MFA setup") },
                navigationIcon = {
                    IconButton(onClick = { onMfaSetupComplete() }) { // 返回按鈕也觸發完成回調
                        Icon(                       // ← 這裡要顯式寫 imageVector 參數
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues) // 使用 Scaffold 的 padding
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 您可以將 MFASetupScreen.kt 中的 Image 和 Text 佈局放在這裡
            // Image(painter = painterResource(id = R.drawable.lock), ...)
            // Text(stringResource(R.string.mfa_setup), ...)

            if (currentUser == null) {
                Text("login to setup MFA。")
                return@Column
            }

            if (phoneMfaViewModel.isMfaSuccessfullySetup) {
                Text("MFA enabled for ${phoneMfaViewModel.phoneNumberInput}") // 使用 phoneNumberInput
                Button(onClick = { phoneMfaViewModel.disablePhoneMfa() }) {
                    Text("disabled MFA")
                }
            } else {
                // 使用您 MFASetupScreen.kt 中的 StyledTextField
                OutlinedTextField( // 暫用 OutlinedTextField 替代 StyledTextField
                    value = phoneMfaViewModel.phoneNumberInput,
                    onValueChange = { phoneMfaViewModel.phoneNumberInput = it },
                    label = { Text("phone number (e.g +16505551234)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !phoneMfaViewModel.isCodeSent
                )

                Button(
                    onClick = { phoneMfaViewModel.startPhoneNumberVerificationForSetup(activity, phoneMfaViewModel.phoneNumberInput) },
                    enabled = !phoneMfaViewModel.isLoading && !phoneMfaViewModel.isCodeSent && phoneMfaViewModel.phoneNumberInput.isNotBlank()
                ) {
                    Text("1. send verification code")
                }

                if (phoneMfaViewModel.isCodeSent) {
                    OutlinedTextField( // 暫用 OutlinedTextField 替代 StyledTextField
                        value = phoneMfaViewModel.smsCodeInput,
                        onValueChange = { phoneMfaViewModel.smsCodeInput = it },
                        label = { Text("2. verification code") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { phoneMfaViewModel.verifySmsCodeAndEnableMfa() },
                        enabled = !phoneMfaViewModel.isLoading && phoneMfaViewModel.smsCodeInput.isNotBlank()
                    ) {
                        Text("3. verify and enable MFA")
                    }
                }
            }

            if (phoneMfaViewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
            }

            phoneMfaViewModel.infoMessage?.let { message ->
                Text(message, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp))
            }
        }
    }
}