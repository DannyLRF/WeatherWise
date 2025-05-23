package com.example.weatherwise.ui.auth.mfa
// 導入您上傳的 Composable 函數 (請確保包名正確)
// MFA 相關的 UI 界面，我們稍後會用到您上傳的版本
// import com.example.weatherwise.ui.auth.MFASetupScreen
// import com.example.weatherwise.ui.auth.MFAVerificationScreen
import com.google.firebase.auth.PhoneMultiFactorGenerator
import android.app.Activity
import android.util.Log
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

    fun checkCurrentUserMfaStatus() {
        viewModelScope.launch {
            // 先重置輸入狀態
            resetInputState()

            val currentUser = firebaseAuth.currentUser
            if (currentUser != null) {
                val profile = loadUserMfaProfile(currentUser.uid)
                if (profile?.isPhoneMfaEnabled == true) {
                    isMfaSuccessfullySetup = true
                    this@PhoneMfaViewModel.phoneNumberInput = profile.phoneNumber ?: ""
                    infoMessage = "MFA enabled for ${profile.phoneNumber}"
                } else {
                    // 如果新用戶沒有 MFA，清除之前的設置狀態
                    isMfaSuccessfullySetup = false
                }
            } else {
                // 沒有用戶登錄，重置所有內容
                resetViewModelState()
            }
        }
    }

    // 用於 MFA 設置流程
    fun startPhoneNumberVerificationForSetup(activity: Activity?, fullPhoneNumber: String) {
        this.mfaLoginSession = null // 確保不是登錄流程
        initiatePhoneNumberVerification(activity, fullPhoneNumber)
    }

    // 用於登錄時的 MFA 驗證流程
    fun startPhoneNumberVerificationForLogin(activity: Activity?, session: MultiFactorSession, phoneHint: PhoneMultiFactorInfo) {
        this.mfaLoginSession = session
        this.phoneNumberInput = phoneHint.phoneNumber // 登錄時通常使用已知的提示號碼

        // 調用新增的 MFA 專用方法
        initiateMfaPhoneVerification(activity, session, phoneHint)
    }
    // 新增 MFA 專用的驗證方法
    private fun initiateMfaPhoneVerification(activity: Activity?, session: MultiFactorSession, phoneHint: PhoneMultiFactorInfo) {
        if (activity == null) {
            infoMessage = "can't retrieve Activity"
            return
        }

        isLoading = true
        infoMessage = "sending code..."
        isCodeSent = false

        authCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            // 回調實現保持不變
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(PHONE_MFA_TAG, "onVerificationCompleted:$credential")
                isLoading = false
                isCodeSent = true
                infoMessage = "code retrieved"
                smsCodeInput = credential.smsCode ?: ""
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.w(PHONE_MFA_TAG, "onVerificationFailed", e)
                isLoading = false
                infoMessage = "failed to verify: ${e.message}"
                isCodeSent = false
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                Log.d(PHONE_MFA_TAG, "onCodeSent:$verificationId")
                this@PhoneMfaViewModel.verificationId = verificationId
                isLoading = false
                infoMessage = "code sent"
                isCodeSent = true
            }
        }

        // 注意：這裡不使用 setPhoneNumber，而是使用 setMultiFactorHint
        val optionsBuilder = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setActivity(activity)
            .setCallbacks(authCallbacks)
            .setMultiFactorSession(session)
            .setMultiFactorHint(phoneHint)
            .setTimeout(60L, TimeUnit.SECONDS)

        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    private fun initiatePhoneNumberVerification(activity: Activity?, fullPhoneNumber: String) {
        if (activity == null) {
            infoMessage = "can't retrieve Activity"
            return
        }

        if (fullPhoneNumber.isBlank()) {
            infoMessage = "input valid number"
            return
        }

        isLoading = true
        infoMessage = "sending code..."
        isCodeSent = false

        authCallbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(PHONE_MFA_TAG, "onVerificationCompleted:$credential")
                isLoading = false
                isCodeSent = true

                if (mfaLoginSession != null) {
                    infoMessage = "code retrieved"
                    smsCodeInput = credential.smsCode ?: ""
                } else {
                    linkCredentialToUserForSetup(credential, "MFA enabled")
                }
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.w(PHONE_MFA_TAG, "onVerificationFailed", e)
                isLoading = false
                infoMessage = "failed to verify code: ${e.message}"
                isCodeSent = false
            }

            override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                Log.d(PHONE_MFA_TAG, "onCodeSent:$verificationId")
                this@PhoneMfaViewModel.verificationId = verificationId
                isLoading = false
                infoMessage = "code sent $fullPhoneNumber"
                isCodeSent = true
            }
        }

        val optionsBuilder = PhoneAuthOptions.newBuilder(firebaseAuth)
            .setPhoneNumber(fullPhoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(authCallbacks)

        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
//        if (session != null && phoneHint != null) {
//            optionsBuilder
//                .setMultiFactorSession(session)
//                .setMultiFactorHint(phoneHint) // This is the missing part
//        }
    }

    // 用於 MFA 設置流程
    fun verifySmsCodeAndEnableMfa() {
        val currentVerificationId = verificationId ?: run {
            infoMessage = "can't find id"
            return
        }
        if (smsCodeInput.isBlank() || smsCodeInput.length != 6) {
            infoMessage = "input code"
            return
        }
        isLoading = true
        infoMessage = "verifying..."
        val credential = PhoneAuthProvider.getCredential(currentVerificationId, smsCodeInput)
        linkCredentialToUserForSetup(credential, "MFA enabled！")
    }

    private fun linkCredentialToUserForSetup(credential: PhoneAuthCredential, successMessage: String) {
        val currentUser = firebaseAuth.currentUser ?: run {
            infoMessage = "user haven't login"
            isLoading = false
            return
        }
        val multiFactorAssertion = PhoneMultiFactorGenerator.getAssertion(credential)
        currentUser.multiFactor.enroll(multiFactorAssertion, "my phone number")
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
                    infoMessage = "failed to enable MFA: ${task.exception?.message}"
                }
            }
    }

    // 用於登錄流程，生成 MultiFactorAssertion
    fun getPhoneMultiFactorAssertionForLogin(): PhoneMultiFactorAssertion? {
        val currentVerificationId = verificationId ?: run {
            infoMessage = "failed to find id"
            return null
        }
        if (smsCodeInput.isBlank()) { // 登錄時可能自動獲取，也可能需要輸入
            infoMessage = "input the code"
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
            infoMessage = "failed to store mfa info: ${e.message}"
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
                infoMessage = "failed to find phone number"
                isLoading = false
                saveUserMfaProfile(UserPhoneMfaProfile(userId = currentUser.uid, isPhoneMfaEnabled = false, phoneNumber = null))
                isMfaSuccessfullySetup = false
                return@launch
            }
            currentUser.multiFactor.unenroll(phoneFactor).addOnCompleteListener { task ->
                isLoading = false
                if (task.isSuccessful) {
                    infoMessage = "MFA disabled"
                    isMfaSuccessfullySetup = false
                    phoneNumberInput = ""
                    smsCodeInput = ""
                    verificationId = null
                    isCodeSent = false
                    viewModelScope.launch {
                        saveUserMfaProfile(UserPhoneMfaProfile(userId = currentUser.uid, isPhoneMfaEnabled = false, phoneNumber = null))
                    }
                } else {
                    infoMessage = "failed to disable MFA ${task.exception?.message}"
                }
            }
        }
    }
    fun resetMessages() {
        infoMessage = null
    }

    // 添加新函數重置輸入相關狀態
    fun resetInputState() {
        phoneNumberInput = ""
        smsCodeInput = ""
        verificationId = null
        isCodeSent = false
        infoMessage = null
    }

    // 添加完整重置函數
    fun resetViewModelState() {
        resetInputState()
        isMfaSuccessfullySetup = false
    }
}