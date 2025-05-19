package com.example.weatherwise.ui.auth

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactorInfo
import com.google.firebase.auth.MultiFactorResolver

sealed class AuthUiState {
    object Idle : AuthUiState() // 初始狀態
    object Loading : AuthUiState() // 加載中
    data class Success(val user: FirebaseUser?) : AuthUiState() // 成功狀態
    data class Error(val message: String) : AuthUiState() // 錯誤狀態
    // 新增：MFA 驗證請求狀態
    data class MfaRequired(val resolver: MultiFactorResolver, val hints: List<MultiFactorInfo>) : AuthUiState()
}