package com.example.ui.viewmodels

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.auth.AuthResult
import com.example.data.auth.AuthService
import com.example.data.auth.UserSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(context: Context) : ViewModel() {
    private val authService = AuthService.getInstance(context)

    // UI States
    private val _sessionState = MutableStateFlow<UserSession?>(authService.getCurrentSession())
    val sessionState: StateFlow<UserSession?> = _sessionState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _successMessage = MutableStateFlow<String?>(null)
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    private val _inviteCode = MutableStateFlow<String?>(null)
    val inviteCode: StateFlow<String?> = _inviteCode.asStateFlow()

    private val _otpSent = MutableStateFlow(false)
    val otpSent: StateFlow<Boolean> = _otpSent.asStateFlow()

    private val _lastSimulatedOtp = MutableStateFlow<String?>(null)
    val lastSimulatedOtp: StateFlow<String?> = _lastSimulatedOtp.asStateFlow()

    private val _deliveryStatus = MutableStateFlow<String?>(null)
    val deliveryStatus: StateFlow<String?> = _deliveryStatus.asStateFlow()

    fun checkSession() {
        val current = authService.getCurrentSession()
        _sessionState.value = current
    }

    fun login(emailOrPhone: String, credential: String, role: String, onSuccess: (UserSession) -> Unit) {
        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null

        viewModelScope.launch {
            authService.login(emailOrPhone, credential, role) { result ->
                _isLoading.value = false
                when (result) {
                    is AuthResult.Success -> {
                        _sessionState.value = result.session
                        onSuccess(result.session)
                    }
                    is AuthResult.Error -> {
                        _errorMessage.value = result.message
                    }
                    is AuthResult.VerificationRequired -> {
                        _errorMessage.value = "Account verification is required to complete login."
                    }
                }
            }
        }
    }

    fun signup(
        displayName: String,
        email: String,
        phone: String,
        password: String,
        role: String,
        onSuccess: (UserSession) -> Unit
    ) {
        _isLoading.value = true
        _errorMessage.value = null
        _successMessage.value = null

        viewModelScope.launch {
            authService.signup(displayName, email, phone, password, role) { result ->
                _isLoading.value = false
                when (result) {
                    is AuthResult.Success -> {
                        _sessionState.value = result.session
                        _successMessage.value = "Registration successful! Welcome, ${result.session.displayName}"
                        onSuccess(result.session)
                    }
                    is AuthResult.Error -> {
                        _errorMessage.value = result.message
                    }
                    is AuthResult.VerificationRequired -> {
                        _errorMessage.value = "Verification required before proceeding."
                    }
                }
            }
        }
    }

    fun logout(onComplete: () -> Unit) {
        _isLoading.value = true
        viewModelScope.launch {
            authService.clearSession()
            _sessionState.value = null
            _isLoading.value = false
            onComplete()
        }
    }

    fun sendCustomerOtp(phone: String, onComplete: (Boolean) -> Unit) {
        if (phone.trim().length < 10) {
            _errorMessage.value = "Enter a valid 10-digit phone number"
            onComplete(false)
            return
        }
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            authService.generateAndSendOtp(phone, "LOGIN") { success, codeOrError ->
                _isLoading.value = false
                if (success) {
                    _otpSent.value = true
                    _lastSimulatedOtp.value = codeOrError
                    _deliveryStatus.value = "Security code dispatched to $phone (SMS & notification)"
                    _successMessage.value = "A 6-digit verification code has been dispatched to $phone. Please check your SMS or notifications."
                    onComplete(true)
                } else {
                    _deliveryStatus.value = null
                    _errorMessage.value = codeOrError
                    onComplete(false)
                }
            }
        }
    }

    fun verifyCustomerOtpAndLogin(phone: String, name: String, otp: String, onSuccess: (UserSession) -> Unit) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            authService.verifyCustomerOtpAndLogin(phone, name, otp) { result ->
                _isLoading.value = false
                when (result) {
                    is AuthResult.Success -> {
                        _sessionState.value = result.session
                        _lastSimulatedOtp.value = null
                        onSuccess(result.session)
                    }
                    is AuthResult.Error -> {
                        _errorMessage.value = result.message
                    }
                    is AuthResult.VerificationRequired -> {}
                }
            }
        }
    }

    fun requestPasswordResetOtp(target: String, onComplete: (Boolean) -> Unit) {
        if (target.trim().length < 6) {
            _errorMessage.value = "Enter a valid email or 10-digit phone number"
            onComplete(false)
            return
        }
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            authService.generateAndSendOtp(target, "RESET_PASSWORD") { success, codeOrError ->
                _isLoading.value = false
                if (success) {
                    _lastSimulatedOtp.value = codeOrError
                    val destination = if (target.contains("@")) "your Email inbox ($target)" else "your SMS messages ($target)"
                    _deliveryStatus.value = if (target.contains("@")) "Dispatched via Email to $target" else "Dispatched via SMS to $target"
                    _successMessage.value = "A 6-digit reset code has been dispatched to $destination."
                    onComplete(true)
                } else {
                    _deliveryStatus.value = null
                    _errorMessage.value = codeOrError
                    onComplete(false)
                }
            }
        }
    }

    fun sendSignupOtp(target: String, onComplete: (Boolean) -> Unit) {
        if (target.trim().length < 6) {
            _errorMessage.value = "Enter a valid email or 10-digit phone number"
            onComplete(false)
            return
        }
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            authService.generateAndSendOtp(target, "SIGNUP") { success, codeOrError ->
                _isLoading.value = false
                if (success) {
                    _lastSimulatedOtp.value = codeOrError
                    val destination = if (target.contains("@")) "your Email inbox ($target)" else "your SMS messages ($target)"
                    _deliveryStatus.value = if (target.contains("@")) "Dispatched via Email to $target" else "Dispatched via SMS to $target"
                    _successMessage.value = "A 6-digit verification code has been dispatched to $destination."
                    onComplete(true)
                } else {
                    _deliveryStatus.value = null
                    _errorMessage.value = codeOrError
                    onComplete(false)
                }
            }
        }
    }

    fun verifySignupOtp(target: String, otp: String, onComplete: (Boolean, String) -> Unit) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            authService.verifyOtp(target, "SIGNUP", otp) { success, msg ->
                _isLoading.value = false
                if (success) {
                    _lastSimulatedOtp.value = null
                } else {
                    _errorMessage.value = msg
                }
                onComplete(success, msg)
            }
        }
    }

    fun resetPasswordWithOtp(target: String, otp: String, newPass: String, onComplete: (Boolean) -> Unit) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            authService.resetPasswordWithOtp(target, otp, newPass) { success, msg ->
                _isLoading.value = false
                if (success) {
                    _lastSimulatedOtp.value = null
                    _successMessage.value = msg
                    onComplete(true)
                } else {
                    _errorMessage.value = msg
                    onComplete(false)
                }
            }
        }
    }

    fun createWorkerInvitation(
        email: String,
        name: String,
        phone: String,
        designation: String,
        salary: Double,
        onComplete: (Boolean, String) -> Unit
    ) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            authService.createWorker(email, name, phone, designation, salary) { success, codeOrMessage ->
                _isLoading.value = false
                if (success) {
                    _inviteCode.value = codeOrMessage
                    onComplete(true, codeOrMessage)
                } else {
                    _errorMessage.value = codeOrMessage
                    onComplete(false, codeOrMessage)
                }
            }
        }
    }

    fun verifyWorkerInvitationAndRegister(
        email: String,
        code: String,
        passwordPin: String,
        onSuccess: (UserSession) -> Unit
    ) {
        _isLoading.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            authService.acceptInvitationAndSetupPassword(email, code, passwordPin) { result ->
                _isLoading.value = false
                when (result) {
                    is AuthResult.Success -> {
                        _sessionState.value = result.session
                        onSuccess(result.session)
                    }
                    is AuthResult.Error -> {
                        _errorMessage.value = result.message
                    }
                    is AuthResult.VerificationRequired -> {
                        _errorMessage.value = "Verification code accepted but activation is pending."
                    }
                }
            }
        }
    }

    fun resetError() {
        _errorMessage.value = null
    }

    fun clearErrors() {
        _errorMessage.value = null
        _successMessage.value = null
    }

    fun resetSuccessMessage() {
        _successMessage.value = null
    }

    fun dismissOtpNotification() {
        _lastSimulatedOtp.value = null
    }

    fun openSmsAppForTarget(phone: String, context: Context) {
        val otp = _lastSimulatedOtp.value ?: ""
        val msg = if (otp.isNotBlank()) "ReLec verification code: $otp" else "ReLec security verification request"
        com.example.util.VerificationNotificationHelper.sendSmsViaIntent(context, phone, msg)
    }
}

class AuthViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(context.applicationContext) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
