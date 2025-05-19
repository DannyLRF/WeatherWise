package com.example.weatherwise.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherwise.data.repository.AuthRepository
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.MultiFactorAssertion
import com.google.firebase.auth.MultiFactorInfo
import com.google.firebase.auth.MultiFactorResolver
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _authUiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    private val _currentUser = MutableStateFlow(authRepository.getCurrentUser())
    val currentUser: StateFlow<FirebaseUser?> = _currentUser.asStateFlow()

    // 用於存儲 MFA 流程中的 MultiFactorResolver
    var mfaResolver: MultiFactorResolver? = null
    var mfaHints: List<MultiFactorInfo>? = null


    fun register(email: String, password: String) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            val result = authRepository.registerUser(email, password)
            _authUiState.value = result
            if (result is AuthUiState.Success) _currentUser.value = result.user
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            val result = authRepository.loginUser(email, password)
            if (result is AuthUiState.MfaRequired) {
                mfaResolver = result.resolver
                mfaHints = result.hints
            }
            _authUiState.value = result // 將結果（包括 MfaRequired）暴露給 UI
            // Success 狀態的 currentUser 更新將在 signInWithMfaAssertion 成功後進行
        }
    }

    fun completeMfaSignIn(assertion: MultiFactorAssertion) {
        val currentResolver = mfaResolver
        if (currentResolver == null) {
            _authUiState.value = AuthUiState.Error("MFA 流程錯誤：Resolver 未找到")
            return
        }
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            val result = authRepository.signInWithMfaAssertion(currentResolver, assertion)
            _authUiState.value = result
            if (result is AuthUiState.Success) {
                _currentUser.value = result.user
                mfaResolver = null // 清理 resolver
                mfaHints = null
            }
        }
    }

    fun logout() {
        authRepository.logoutUser()
        _currentUser.value = null
        _authUiState.value = AuthUiState.Idle
        mfaResolver = null // 清理 resolver
        mfaHints = null
    }

    fun resetAuthUiState() {
        if (_authUiState.value !is AuthUiState.MfaRequired) { // 避免在MFA流程中重置
            _authUiState.value = AuthUiState.Idle
        }
    }

    companion object {
        fun provideFactory(authRepository: AuthRepository): AuthViewModelFactory {
            return AuthViewModelFactory(authRepository)
        }
    }
}