package com.example.weatherwise.ui.auth

import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactorInfo
import com.google.firebase.auth.MultiFactorResolver

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState() // Loading
    data class Success(val user: FirebaseUser?) : AuthUiState() // Success
    data class Error(val message: String) : AuthUiState() // Error
    // MFA
    data class MfaRequired(val resolver: MultiFactorResolver, val hints: List<MultiFactorInfo>) : AuthUiState()
}