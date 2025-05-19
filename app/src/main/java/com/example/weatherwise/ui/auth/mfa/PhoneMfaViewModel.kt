package com.example.weatherwise.ui.auth.mfa
// 導入您上傳的 Composable 函數 (請確保包名正確)
// MFA 相關的 UI 界面，我們稍後會用到您上傳的版本
// import com.example.weatherwise.ui.auth.MFASetupScreen
// import com.example.weatherwise.ui.auth.MFAVerificationScreen
import com.google.firebase.auth.PhoneMultiFactorGenerator
import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.weatherwise.data.model.auth.UserPhoneMfaProfile
import com.example.weatherwise.util.PHONE_MFA_TAG
import com.google.firebase.FirebaseException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class PhoneMfaViewModel : ViewModel() {
    var phoneNumberInput by mutableStateOf("") // 用於 MFA 設置時輸入電話號碼
    var smsCodeInput by mutableStateOf("")    // 用於 MFA 設置或登錄時輸入 SMS 驗證碼
    var verificationId by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(false)
        private set
    var infoMessage by mutableStateOf<String?>(null)
        private set
    var isMfaSuccessfullySetup by mutableStateOf(false) // 用於MFA設置流程
        private set
    var isCodeSent by mutableStateOf(false)
        private set

    private val firestore = FirebaseFirestore.getInstance()
    private val firebaseAuth = FirebaseAuth.getInstance()
    private lateinit var authCallbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks

    // 用於登錄時的 MultiFactorSession
    private var mfaLoginSession: MultiFactorSession? = null

    init {
        checkCurrentUserMfaStatus()
    }

    private fun checkCurrentUserMfaStatus() {
        viewModelScope.launch {
            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                val profile = loadUserMfaProfile(currentUser.uid)
                if (profile?.isPhoneMfaEnabled == true) {
                    isMfaSuccessfullySetup = true // 標記已設置
                    this@PhoneMfaViewModel.phoneNumberInput = profile.phoneNumber ?: "" // 顯示已綁定號碼
                    infoMessage = "電話 MFA 已為 ${profile.phoneNumber} 啟用。"
                }
            }
        }
    }

    // 用於 MFA 設置流程
    fun startPhoneNumberVerificationForSetup(activity: Activity, fullPhoneNumber: String) {
        this.mfaLoginSession = null // 確保不是登錄流程
        initiatePhoneNumberVerification(activity, fullPhoneNumber, null)
    }

    // 用於登錄時的 MFA 驗證流程
    fun startPhoneNumberVerificationForLogin(activity: Activity, session: MultiFactorSession, hintPhoneNumber: String) {
        this.mfaLoginSession = session
        this.phoneNumberInput = hintPhoneNumber // 登錄時通常使用已知的提示號碼
        initiatePhoneNumberVerification(activity, hintPhoneNumber, session)
    }

    private fun initiatePhoneNumberVerification(activity: Activity, fullPhoneNumber: String, session: MultiFactorSession?) {
        if (fullPhoneNumber.isBlank()) {
            infoMessage = "請輸入有效的電話號碼。"
            return
        }
        isLoading = true
        infoMessage = "正在發送驗證碼..."
        isCodeSent = false

        authCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(PHONE_MFA_TAG, "onVerificationCompleted:$credential")
                isLoading = false
                isCodeSent = true
                if (mfaLoginSession != null) { // 登錄流程中的自動完成
                    // 不需要用戶輸入 code，直接用 credential 生成 assertion
                    // 這部分邏輯會在 AuthViewModel 中處理
                    infoMessage = "驗證碼自動獲取成功。"
                    smsCodeInput = credential.smsCode ?: "" // 嘗試獲取 code 以便 UI 更新 (可選)
                    // 通知 AuthViewModel 可以嘗試完成登錄
                } else { // MFA 設置流程中的自動完成
                    linkCredentialToUserForSetup(credential, "驗證碼自動驗證成功！MFA 已啟用。")
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.w(PHONE_MFA_TAG, "onVerificationFailed", e)
                isLoading = false
                infoMessage = "電話號碼驗證失敗: ${e.message}"
                isCodeSent = false
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                Log.d(PHONE_MFA_TAG, "onCodeSent:$verificationId")
                this@PhoneMfaViewModel.verificationId = verificationId
                isLoading = false
                infoMessage = "驗證碼已發送到 $fullPhoneNumber"
                isCodeSent = true
            }
        }

        val optionsBuilder = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(fullPhoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(authCallbacks)

        session?.let {
            optionsBuilder.setMultiFactorSession(it) // 如果是登錄流程，設置 session
        }

        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    // 用於 MFA 設置流程
    fun verifySmsCodeAndEnableMfa() {
        val currentVerificationId = verificationId ?: run {
            infoMessage = "錯誤：驗證 ID 未找到。"
            return
        }
        if (smsCodeInput.isBlank() || smsCodeInput.length != 6) {
            infoMessage = "請輸入6位數驗證碼。"
            return
        }
        isLoading = true
        infoMessage = "正在驗證..."
        val credential = PhoneAuthProvider.getCredential(currentVerificationId, smsCodeInput)
        linkCredentialToUserForSetup(credential, "MFA 已成功啟用！")
    }

    private fun linkCredentialToUserForSetup(credential: PhoneAuthCredential, successMessage: String) {
        val currentUser = firebaseAuth.currentUser ?: run {
            infoMessage = "錯誤：用戶未登錄。"
            isLoading = false
            return
        }
        val multiFactorAssertion = PhoneMultiFactorGenerator.getAssertion(credential)
        currentUser.multiFactor.enroll(multiFactorAssertion, "我的電話號碼")
            .addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    Log.d(PHONE_MFA_TAG, "MFA enabled successfully.")
                    infoMessage = successMessage
                    isMfaSuccessfullySetup = true
                    viewModelScope.launch {
                        saveUserMfaProfile(
                            UserPhoneMfaProfile(
                                userId = currentUser.uid,
                                isPhoneMfaEnabled = true,
                                phoneNumber = credential.smsCode?.let { currentUser.phoneNumber } ?: phoneNumberInput // 更新存儲的號碼
                            )
                        )
                    }
                } else {
                    Log.w(PHONE_MFA_TAG, "Error enrolling MFA", task.exception)
                    infoMessage = "啟用 MFA 失敗: ${task.exception?.message}"
                }
            }
    }

    // 用於登錄流程，生成 MultiFactorAssertion
    fun getPhoneMultiFactorAssertionForLogin(): PhoneMultiFactorAssertion? {
        val currentVerificationId = verificationId ?: run {
            infoMessage = "錯誤：驗證 ID 未找到 (登錄流程)。"
            return null
        }
        if (smsCodeInput.isBlank()) { // 登錄時可能自動獲取，也可能需要輸入
            infoMessage = "請輸入驗證碼 (登錄流程)。"
            // 如果是自動完成，smsCodeInput 可能為空，但 credential 已在 onVerificationCompleted 中處理
            // 這裡假設如果需要手動輸入，則 smsCodeInput 不會為空
            // 實際上，如果 onVerificationCompleted 提供了 credential，應直接使用它
            return null
        }
        val credential = PhoneAuthProvider.getCredential(currentVerificationId, smsCodeInput)
        return PhoneMultiFactorGenerator.getAssertion(credential)
    }


    private suspend fun saveUserMfaProfile(userMfaProfile: UserPhoneMfaProfile) {
        try {
            firestore.collection("user_mfa_profiles").document(userMfaProfile.userId).set(userMfaProfile).await()
            Log.d(PHONE_MFA_TAG, "User MFA profile saved.")
        } catch (e: Exception) {
            infoMessage = "儲存MFA設定失敗: ${e.message}"
            Log.e(PHONE_MFA_TAG, "Error saving MFA profile", e)
        }
    }

    suspend fun loadUserMfaProfile(userId: String): UserPhoneMfaProfile? {
        return try {
            firestore.collection("user_mfa_profiles").document(userId).get().await()
                .toObject(UserPhoneMfaProfile::class.java)
        } catch (e: Exception) {
            Log.e(PHONE_MFA_TAG, "Error loading MFA profile", e)
            null
        }
    }

    fun disablePhoneMfa() {
        viewModelScope.launch {
            val currentUser = firebaseAuth.currentUser ?: return@launch
            isLoading = true
            val phoneFactor = currentUser.multiFactor.enrolledFactors.find { it.factorId == PhoneMultiFactorGenerator.FACTOR_ID }
            if (phoneFactor == null) {
                infoMessage = "未找到已啟用的電話 MFA。"
                isLoading = false
                saveUserMfaProfile(UserPhoneMfaProfile(userId = currentUser.uid, isPhoneMfaEnabled = false, phoneNumber = null))
                isMfaSuccessfullySetup = false
                return@launch
            }
            currentUser.multiFactor.unenroll(phoneFactor).addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    infoMessage = "電話 MFA 已禁用。"
                    isMfaSuccessfullySetup = false
                    phoneNumberInput = ""
                    smsCodeInput = ""
                    verificationId = null
                    isCodeSent = false
                    viewModelScope.launch {
                        saveUserMfaProfile(UserPhoneMfaProfile(userId = currentUser.uid, isPhoneMfaEnabled = false, phoneNumber = null))
                    }
                } else {
                    infoMessage = "禁用 MFA 失敗: ${task.exception?.message}"
                }
            }
        }
    }
    fun resetMessages() { // 新增：用於清除提示信息
        infoMessage = null
    }
}