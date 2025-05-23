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

    // Get userId for current user
    val currentUserId: String?
        get() = currentUser.value?.uid

    // Store MFA  MultiFactorResolver
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
            _authUiState.value = result

        }
    }

    fun completeMfaSignIn(assertion: MultiFactorAssertion) {
        val currentResolver = mfaResolver
        if (currentResolver == null) {
            _authUiState.value = AuthUiState.Error("MFA Process Error：Resolver not found")
            return
        }
        viewModelScope.launch {
            _authUiState.value = AuthUiState.Loading
            val result = authRepository.signInWithMfaAssertion(currentResolver, assertion)
            _authUiState.value = result
            if (result is AuthUiState.Success) {
                _currentUser.value = result.user
                mfaResolver = null // Clean resolver
                mfaHints = null
            }
        }
    }

    fun logout() {
        authRepository.logoutUser()
        _currentUser.value = null
        _authUiState.value = AuthUiState.Idle
        mfaResolver = null // Clean resolver
        mfaHints = null
    }

    fun resetAuthUiState() {
        if (_authUiState.value !is AuthUiState.MfaRequired) { // Avoid reset
            _authUiState.value = AuthUiState.Idle
        }
    }

    companion object {
        fun provideFactory(authRepository: AuthRepository): AuthViewModelFactory {
            return AuthViewModelFactory(authRepository)
        }
    }
}