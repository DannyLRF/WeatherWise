package com.example.weatherwise.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await
import com.example.weatherwise.ui.auth.AuthUiState
import com.google.firebase.auth.FirebaseAuthMultiFactorException
import com.google.firebase.auth.MultiFactorAssertion
import com.google.firebase.auth.MultiFactorResolver


class AuthRepository(private val firebaseAuth: FirebaseAuth) {

    suspend fun registerUser(email: String, password: String): AuthUiState {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            AuthUiState.Success(result.user)
        } catch (e: Exception) {
            AuthUiState.Error(e.message ?: "註冊失敗，未知錯誤")
        }
    }

    suspend fun loginUser(email: String, password: String): AuthUiState {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            AuthUiState.Success(result.user)
        } catch (e: FirebaseAuthMultiFactorException) {
            // 捕獲 MFA 異常
            Log.d("AuthRepository", "MFA Required for user: $email")
            AuthUiState.MfaRequired(e.resolver, e.resolver.hints)
        }
        catch (e: Exception) {
            AuthUiState.Error(e.message ?: "登錄失敗，未知錯誤")
        }
    }

    // 使用 MultiFactorAssertion 完成 MFA 登錄
    suspend fun signInWithMfaAssertion(resolver: MultiFactorResolver, assertion: MultiFactorAssertion): AuthUiState {
        return try {
            val result = resolver.resolveSignIn(assertion).await()
            AuthUiState.Success(result.user)
        } catch (e: Exception) {
            AuthUiState.Error(e.message ?: "MFA 登錄失敗")
        }
    }


    fun getCurrentUser(): FirebaseUser? = firebaseAuth.currentUser
    fun logoutUser() = firebaseAuth.signOut()
}