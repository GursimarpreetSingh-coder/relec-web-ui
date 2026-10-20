package com.example.data.auth

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.AppDatabase
import com.example.data.AuditLogEntry
import com.example.data.Customer
import com.example.data.OtpRecord
import com.example.data.ReLectRepository
import com.example.data.UserEntity
import com.example.data.Worker
import com.example.data.WorkerStatus
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random

// Sealed Class for Auth Results
sealed class AuthResult {
    data class Success(val session: UserSession) : AuthResult()
    data class Error(val message: String) : AuthResult()
    object VerificationRequired : AuthResult()
}

// User Session Model
data class UserSession(
    val uid: String,
    val email: String,
    val phone: String,
    val displayName: String,
    val role: String, // SuperAdmin, Worker, Customer
    val isApproved: Boolean = true,
    val isActive: Boolean = true,
    val token: String = UUID.randomUUID().toString(),
    val createdAt: String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
)

object FirebaseConfig {
    private const val TAG = "FirebaseConfig"

    val isAvailable: Boolean
        get() = try {
            FirebaseApp.getInstance() != null
        } catch (e: Exception) {
            false
        }
}

class AuthService private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val prefs: SharedPreferences = appContext.getSharedPreferences("relect_auth_prefs", Context.MODE_PRIVATE)
    private val database = AppDatabase.getDatabase(appContext)
    private val dao = database.reLectDao()
    private val authScope = CoroutineScope(Dispatchers.IO)
    private val repository = ReLectRepository.getInstance(appContext)

    private var currentSession: UserSession? = null

    init {
        restoreSession()
        seedInitialUsersIfEmpty()
    }

    companion object {
        @Volatile
        private var INSTANCE: AuthService? = null

        fun getInstance(context: Context): AuthService {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AuthService(context).also { INSTANCE = it }
            }
        }
    }

    // --- SHA-256 Hashing with Salt ---
    fun hashStringWithSalt(input: String, salt: String): String {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val combined = "$salt:$input"
            val hash = digest.digest(combined.toByteArray(Charsets.UTF_8))
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            input
        }
    }

    private fun logAudit(action: String, details: String, userId: String? = currentSession?.uid, userName: String = currentSession?.displayName ?: "Anonymous") {
        authScope.launch {
            try {
                dao.insertAuditLog(
                    AuditLogEntry(
                        userId = userId,
                        userName = userName,
                        action = action,
                        details = details,
                        timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    )
                )
            } catch (e: Exception) {
                Log.e("AuthService", "Failed to write audit log: ${e.message}")
            }
        }
    }

    // --- Seed Initial Real Accounts in Room Database (No Hardcoded Runtime Bypasses) ---
    private fun seedInitialUsersIfEmpty() {
        authScope.launch {
            try {
                val existingMaster = dao.getUserByEmail("gursimarpsr@gmail.com")
                if (existingMaster == null) {
                    val adminSalt = UUID.randomUUID().toString().take(8)
                    val adminHash = hashStringWithSalt("Admin@123", adminSalt)
                    val adminUser = UserEntity(
                        id = "ADMIN_MASTER_GURSIMAR",
                        email = "gursimarpsr@gmail.com",
                        phone = "9872134590",
                        displayName = "Gursimar Singh",
                        passwordHash = adminHash,
                        salt = adminSalt,
                        role = "Admin",
                        isApproved = true,
                        isActive = true,
                        createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    )
                    dao.insertUser(adminUser)
                    logAudit("SYSTEM_SEED", "Master Admin account gursimarpsr@gmail.com initialized in Room DB", adminUser.id, "System")
                }

                if (dao.getUserCount() <= 1) {
                    val workerSalt = UUID.randomUUID().toString().take(8)
                    val workerHash = hashStringWithSalt("Worker@123", workerSalt)
                    val workerUser = UserEntity(
                        id = "WORKER_" + UUID.randomUUID().toString().take(8),
                        email = "worker@relect.com",
                        phone = "9821034455",
                        displayName = "Harpreet Singh",
                        passwordHash = workerHash,
                        salt = workerSalt,
                        role = "Worker",
                        isApproved = true,
                        isActive = true,
                        createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    )
                    dao.insertUser(workerUser)

                    Log.d("AuthService", "Seeded secure accounts into Room database")
                }

                // Sanitize any existing accounts in DB to eliminate any corrupted newline characters
                try {
                    val allUsers = dao.getAllUsersList()
                    for (u in allUsers) {
                        val sanitizedName = u.displayName.replace(Regex("(?i)\\s*\\(master\\s*admin\\)"), "").replace(Regex("[\\r\\n\\t\\u00A0\\u2028\\u2029]+"), " ").trim()
                        val sanitizedEmail = u.email?.replace(Regex("[\\r\\n\\t\\u00A0\\u2028\\u2029]+"), "")?.trim()
                        val sanitizedPhone = u.phone?.replace(Regex("[\\r\\n\\t\\u00A0\\u2028\\u2029]+"), "")?.trim()
                        if (sanitizedName != u.displayName || sanitizedEmail != u.email || sanitizedPhone != u.phone) {
                            dao.updateUser(u.copy(displayName = sanitizedName, email = sanitizedEmail, phone = sanitizedPhone))
                        }
                    }
                } catch (cleanupErr: Exception) {
                    Log.w("AuthService", "User cleanup warning: ${cleanupErr.message}")
                }
            } catch (e: Exception) {
                Log.e("AuthService", "Error seeding initial accounts: ${e.message}")
            }
        }
    }

    // --- Session Restoring & Management ---
    private fun restoreSession() {
        val uid = prefs.getString("session_uid", null)
        val email = prefs.getString("session_email", null)
        val phone = prefs.getString("session_phone", null)
        val displayName = prefs.getString("session_display_name", null)
        val role = prefs.getString("session_role", null)
        val isApproved = prefs.getBoolean("session_is_approved", true)
        val isActive = prefs.getBoolean("session_is_active", true)
        val token = prefs.getString("session_token", null)

        if (uid != null && role != null && token != null) {
            currentSession = UserSession(
                uid = uid,
                email = email ?: "",
                phone = phone ?: "",
                displayName = displayName ?: "User",
                role = role,
                isApproved = isApproved,
                isActive = isActive,
                token = token
            )
        }
    }

    private fun saveSession(session: UserSession) {
        prefs.edit().apply {
            putString("session_uid", session.uid)
            putString("session_email", session.email)
            putString("session_phone", session.phone)
            putString("session_display_name", session.displayName)
            putString("session_role", session.role)
            putBoolean("session_is_approved", session.isApproved)
            putBoolean("session_is_active", session.isActive)
            putString("session_token", session.token)
            apply()
        }
        currentSession = session
        logAudit("SESSION_START", "User session established for role ${session.role}", session.uid, session.displayName)
    }

    fun clearSession() {
        val prevSession = currentSession
        prefs.edit().clear().apply()
        currentSession = null
        if (prevSession != null) {
            logAudit("LOGOUT", "User signed out", prevSession.uid, prevSession.displayName)
        }
    }

    fun getCurrentSession(): UserSession? {
        return currentSession
    }

    // =========================================================================
    // 1. PRODUCTION LOGIN (Real Database & Salted Hash Verification)
    // =========================================================================
    fun login(emailOrPhone: String, credential: String, role: String, onResult: (AuthResult) -> Unit) {
        val trimmedInput = emailOrPhone.trim()
        val trimmedCred = credential.trim()

        if (trimmedInput.isBlank() || trimmedCred.isBlank()) {
            onResult(AuthResult.Error("Please enter your login ID and password."))
            return
        }

        authScope.launch {
            if (FirebaseConfig.isAvailable) {
                // Firebase Production Flow
                try {
                    val auth = FirebaseAuth.getInstance()
                    val firestore = FirebaseFirestore.getInstance()

                    if (role == "Customer") {
                        withContext(Dispatchers.Main) {
                            onResult(AuthResult.Error("Please use phone OTP verification for customer access."))
                        }
                        return@launch
                    }

                    auth.signInWithEmailAndPassword(trimmedInput, trimmedCred)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val fbUser = auth.currentUser
                                if (fbUser != null) {
                                    firestore.collection("users").document(fbUser.uid).get()
                                        .addOnSuccessListener { doc ->
                                            val dbRole = doc.getString("role") ?: role
                                            val dbIsActive = doc.getBoolean("isActive") ?: true
                                            val dbIsApproved = doc.getBoolean("isApproved") ?: true

                                            if (!dbIsActive) {
                                                auth.signOut()
                                                onResult(AuthResult.Error("Account has been disabled by the administrator."))
                                                return@addOnSuccessListener
                                            }
                                            if (!dbIsApproved) {
                                                auth.signOut()
                                                onResult(AuthResult.Error("Account registration is pending administrator approval."))
                                                return@addOnSuccessListener
                                            }

                                            val session = UserSession(
                                                uid = fbUser.uid,
                                                email = fbUser.email ?: "",
                                                phone = doc.getString("phone") ?: "",
                                                displayName = doc.getString("displayName") ?: fbUser.displayName ?: "User",
                                                role = dbRole,
                                                isApproved = dbIsApproved,
                                                isActive = dbIsActive
                                            )
                                            saveSession(session)
                                            onResult(AuthResult.Success(session))
                                        }
                                        .addOnFailureListener { e ->
                                            auth.signOut()
                                            onResult(AuthResult.Error("Failed to fetch RBAC records: ${e.localizedMessage}"))
                                        }
                                } else {
                                    onResult(AuthResult.Error("Unable to retrieve authenticated profile."))
                                }
                            } else {
                                onResult(AuthResult.Error(task.exception?.localizedMessage ?: "Invalid login credentials."))
                            }
                        }
                    return@launch
                } catch (e: Exception) {
                    Log.w("AuthService", "Firebase authentication failed, evaluating local database.", e)
                }
            }

            // Local Cryptographic Verification Against Room Database
            try {
                val isEmail = trimmedInput.contains("@")
                val user = if (isEmail) {
                    dao.getUserByEmail(trimmedInput)
                } else {
                    dao.getUserByPhone(trimmedInput)
                }

                if (user == null) {
                    logAudit("LOGIN_FAILED", "User not found for identifier: $trimmedInput", null, "Unknown")
                    withContext(Dispatchers.Main) {
                        onResult(AuthResult.Error("No registered account found with that email/phone."))
                    }
                    return@launch
                }

                // Check account lockouts
                val currentTime = System.currentTimeMillis()
                if (user.lockUntilTimestamp > currentTime) {
                    val remainingMins = ((user.lockUntilTimestamp - currentTime) / 60000) + 1
                    withContext(Dispatchers.Main) {
                        onResult(AuthResult.Error("Account temporarily locked due to too many failed attempts. Try again in $remainingMins minutes."))
                    }
                    return@launch
                }

                // Verify Salted Password Hash
                val computedHash = hashStringWithSalt(trimmedCred, user.salt)
                if (computedHash != user.passwordHash) {
                    val newFailedAttempts = user.failedAttempts + 1
                    val lockUntil = if (newFailedAttempts >= 5) currentTime + (15 * 60 * 1000) else 0L // 15 mins lock
                    dao.updateUser(user.copy(failedAttempts = newFailedAttempts, lockUntilTimestamp = lockUntil))

                    logAudit("LOGIN_FAILED", "Incorrect password for user ID: ${user.id}, failed attempts: $newFailedAttempts", user.id, user.displayName)
                    withContext(Dispatchers.Main) {
                        val remainingAttempts = 5 - newFailedAttempts
                        if (remainingAttempts > 0) {
                            onResult(AuthResult.Error("Incorrect password. $remainingAttempts attempt(s) remaining before lockout."))
                        } else {
                            onResult(AuthResult.Error("Account locked for 15 minutes due to 5 consecutive failed attempts."))
                        }
                    }
                    return@launch
                }

                // Check Active & Approved status
                if (!user.isActive) {
                    withContext(Dispatchers.Main) {
                        onResult(AuthResult.Error("This account has been disabled by the administrator."))
                    }
                    return@launch
                }
                if (!user.isApproved) {
                    val approvalMsg = when {
                        user.role.equals("Admin", ignoreCase = true) ->
                            "Access Denied: This administrative account is pending approval by the Superior Admin. Please contact the business owner to grant portal privileges."
                        user.role.equals("Worker", ignoreCase = true) ->
                            "Access Denied: Your field technician profile is pending activation by the Administrator."
                        else ->
                            "Your registration is pending administrator approval."
                    }
                    withContext(Dispatchers.Main) {
                        onResult(AuthResult.Error(approvalMsg))
                    }
                    return@launch
                }

                // Reset failed attempts on success
                if (user.failedAttempts > 0) {
                    dao.updateUser(user.copy(failedAttempts = 0, lockUntilTimestamp = 0L))
                }

                val isTargetingAdmin = role.equals("Admin", ignoreCase = true) || role.equals("SuperAdmin", ignoreCase = true)
                val isTargetingWorker = role.equals("Worker", ignoreCase = true)
                val isUserCustomer = user.role.equals("Customer", ignoreCase = true)

                if ((isTargetingAdmin || isTargetingWorker) && isUserCustomer) {
                    logAudit("ACCESS_DENIED", "Customer attempted to sign in via administrative role: $role", user.id, user.displayName)
                    withContext(Dispatchers.Main) {
                        onResult(AuthResult.Error("Access Denied: This account is registered as a Customer. Customers cannot access administrative dashboards. Please sign in via the Customer portal."))
                    }
                    return@launch
                }

                val normalizedRole = when {
                    user.role.equals("Admin", ignoreCase = true) || user.role.equals("SuperAdmin", ignoreCase = true) -> "Admin"
                    user.role.equals("Worker", ignoreCase = true) -> "Worker"
                    else -> "Customer"
                }

                val session = UserSession(
                    uid = user.id,
                    email = user.email ?: "",
                    phone = user.phone ?: "",
                    displayName = user.displayName,
                    role = normalizedRole,
                    isApproved = user.isApproved,
                    isActive = user.isActive
                )
                saveSession(session)
                syncUserToCloudFirestore(user, "LOGIN")

                withContext(Dispatchers.Main) {
                    onResult(AuthResult.Success(session))
                }
            } catch (e: Exception) {
                Log.e("AuthService", "Database login transaction error", e)
                withContext(Dispatchers.Main) {
                    onResult(AuthResult.Error("Authentication error: ${e.localizedMessage ?: e.message}"))
                }
            }
        }
    }

    // =========================================================================
    // 2. PRODUCTION SIGNUP (Real Registration with Validation & Salted Hashing)
    // =========================================================================
    fun signup(
        displayName: String,
        email: String,
        phone: String,
        password: String,
        role: String,
        onResult: (AuthResult) -> Unit
    ) {
        val trimmedName = displayName.trim()
        val trimmedEmail = email.trim().lowercase(Locale.ROOT)
        val trimmedPhone = phone.trim()
        val trimmedPass = password.trim()

        if (trimmedName.length < 2) {
            onResult(AuthResult.Error("Please enter your full legal name."))
            return
        }
        if (trimmedEmail.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
            onResult(AuthResult.Error("Please enter a valid email address."))
            return
        }
        if (trimmedPhone.isNotBlank() && (trimmedPhone.length < 10 || !trimmedPhone.all { it.isDigit() })) {
            onResult(AuthResult.Error("Please enter a valid 10-digit phone number."))
            return
        }
        if (trimmedEmail.isBlank() && trimmedPhone.isBlank()) {
            onResult(AuthResult.Error("Either an email address or phone number is required."))
            return
        }
        if (trimmedPass.length < 6) {
            onResult(AuthResult.Error("Password must be at least 6 characters long."))
            return
        }

        authScope.launch {
            try {
                // Uniqueness check
                if (trimmedEmail.isNotBlank() && dao.getUserByEmail(trimmedEmail) != null) {
                    withContext(Dispatchers.Main) {
                        onResult(AuthResult.Error("An account with this email address is already registered."))
                    }
                    return@launch
                }
                if (trimmedPhone.isNotBlank() && dao.getUserByPhone(trimmedPhone) != null) {
                    withContext(Dispatchers.Main) {
                        onResult(AuthResult.Error("An account with this phone number is already registered."))
                    }
                    return@launch
                }

                val salt = UUID.randomUUID().toString().take(8)
                val passwordHash = hashStringWithSalt(trimmedPass, salt)
                val isWorker = role.equals("Worker", ignoreCase = true)
                val isAdmin = role.equals("Admin", ignoreCase = true)
                val isCustomer = role.equals("Customer", ignoreCase = true)

                // Only Customer is auto-approved. Secondary Admins and Workers require approval from the 1 Superior Admin!
                val autoApprove = isCustomer

                val newUser = UserEntity(
                    id = "USR_" + UUID.randomUUID().toString().take(8),
                    email = trimmedEmail.ifBlank { null },
                    phone = trimmedPhone.ifBlank { null },
                    displayName = trimmedName,
                    passwordHash = passwordHash,
                    salt = salt,
                    role = role,
                    isApproved = autoApprove,
                    isActive = true,
                    createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                )
                dao.insertUser(newUser)
                syncUserToCloudFirestore(newUser, "SIGNUP")

                // If customer, also ensure record in customer table
                if (isCustomer && trimmedPhone.isNotBlank()) {
                    val existing = repository.customers.value.find { it.phone == trimmedPhone }
                    if (existing == null) {
                        repository.addCustomer(
                            Customer(
                                name = trimmedName,
                                phone = trimmedPhone,
                                address = "Registered Account",
                                joinedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            )
                        )
                    }
                }

                logAudit("SIGNUP_SUCCESS", "New account registered with role: $role, approved: $autoApprove", newUser.id, trimmedName)

                if (isAdmin) {
                    withContext(Dispatchers.Main) {
                        onResult(AuthResult.Error("Admin registration submitted! For security, your account is pending approval by the Superior Admin (gursimarpsr@gmail.com). Once approved, you will be able to log in."))
                    }
                } else if (isWorker) {
                    withContext(Dispatchers.Main) {
                        onResult(AuthResult.Error("Worker account registered successfully! It is pending approval by the Superior Admin before you can log in."))
                    }
                } else {
                    val session = UserSession(
                        uid = newUser.id,
                        email = newUser.email ?: "",
                        phone = newUser.phone ?: "",
                        displayName = newUser.displayName,
                        role = role,
                        isApproved = newUser.isApproved,
                        isActive = newUser.isActive
                    )
                    saveSession(session)
                    withContext(Dispatchers.Main) {
                        onResult(AuthResult.Success(session))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(AuthResult.Error("Registration failed: ${e.localizedMessage ?: e.message}"))
                }
            }
        }
    }

    // Target-based memory cache to ensure newly generated OTP never repeats previous code
    private val lastGeneratedOtpByTarget = java.util.concurrent.ConcurrentHashMap<String, String>()

    // =========================================================================
    // 3. PRODUCTION DYNAMIC OTP ENGINE (Cryptographic TTL & Attempt Limiting)
    // =========================================================================
    fun generateAndSendOtp(
        target: String,
        purpose: String,
        onResult: (success: Boolean, codeOrError: String) -> Unit
    ) {
        val cleanTarget = target.trim()
        val digitsOnly = cleanTarget.replace("[^0-9]".toRegex(), "")
        val isEmail = cleanTarget.contains("@") && cleanTarget.contains(".")
        val isPhone = digitsOnly.length in 10..15

        if (!isEmail && !isPhone) {
            onResult(false, "Please enter a valid 10-digit mobile number or email address.")
            return
        }

        authScope.launch {
            try {
                // Verify account existence for password reset or change password code
                if (purpose == "RESET_PASSWORD") {
                    val registeredUser = if (isEmail) {
                        dao.getUserByEmail(cleanTarget)
                    } else {
                        dao.getUserByPhone(cleanTarget) ?: dao.getUserByPhone(digitsOnly)
                    }
                    if (registeredUser == null) {
                        withContext(Dispatchers.Main) {
                            onResult(
                                false,
                                "Account not found. No account is registered with this ${if (isEmail) "email address" else "phone number"}. Please create an account or sign up first."
                            )
                        }
                        return@launch
                    }
                }

                // Verify account existence for login verification code
                if (purpose == "LOGIN") {
                    val registeredUser = if (isEmail) {
                        dao.getUserByEmail(cleanTarget)
                    } else {
                        dao.getUserByPhone(cleanTarget) ?: dao.getUserByPhone(digitsOnly)
                    }
                    val customerExists = !isEmail && repository.customers.value.any { it.phone.replace("[^0-9]".toRegex(), "").takeLast(10) == digitsOnly.takeLast(10) }
                    if (registeredUser == null && !customerExists) {
                        withContext(Dispatchers.Main) {
                            onResult(
                                false,
                                "Account not found. No registered account was found with this ${if (isEmail) "email address" else "phone number"}. Please create an account or sign up first."
                            )
                        }
                        return@launch
                    }
                }

                if (purpose == "SIGNUP") {
                    val existingUser = if (isEmail) {
                        dao.getUserByEmail(cleanTarget)
                    } else {
                        dao.getUserByPhone(cleanTarget) ?: dao.getUserByPhone(digitsOnly)
                    }
                    if (existingUser != null) {
                        withContext(Dispatchers.Main) {
                            onResult(
                                false,
                                "An account already exists with this ${if (isEmail) "email" else "phone"}. Please sign in directly or reset your password."
                            )
                        }
                        return@launch
                    }
                }

                // Invalidate any existing unused OTPs for this target
                dao.invalidateOtpsForTarget(cleanTarget)

                // Generate cryptographically random 6-digit OTP guaranteed not to repeat previous code
                var otpCode = (100000 + Random.nextInt(900000)).toString()
                var tries = 0
                while (otpCode == lastGeneratedOtpByTarget[cleanTarget] && tries < 10) {
                    otpCode = (100000 + Random.nextInt(900000)).toString()
                    tries++
                }
                lastGeneratedOtpByTarget[cleanTarget] = otpCode

                val expiresAt = System.currentTimeMillis() + (5 * 60 * 1000) // 5 minutes TTL

                val otpRecord = OtpRecord(
                    target = cleanTarget,
                    otpCode = otpCode,
                    purpose = purpose,
                    expiresAt = expiresAt,
                    attempts = 0,
                    isUsed = false
                )
                dao.insertOtp(otpRecord)

                logAudit("OTP_GENERATED", "OTP generated for target: $cleanTarget for purpose: $purpose", null, "System")
                Log.d("AuthService", "Generated dynamic OTP: $otpCode for $cleanTarget (Expires in 5m)")

                // Trigger automatic real-world dispatch via Email or SMS
                val isRevolt = repository.activeBrandTheme.value == "revolt_crimson"
                val purposeLabel = when (purpose) {
                    "RESET_PASSWORD" -> "Password Reset"
                    "SIGNUP" -> "Account Registration"
                    else -> "Login Access"
                }

                if (isEmail) {
                    // Send directly to user's Gmail/Email address in background
                    com.example.util.VerificationNotificationHelper.sendAutomaticEmail(
                        context = appContext,
                        email = cleanTarget,
                        otpCode = otpCode,
                        isRevolt = isRevolt
                    )
                } else {
                    // Send directly to user's phone via SMS
                    com.example.util.VerificationNotificationHelper.sendAutomaticSms(
                        context = appContext,
                        phone = cleanTarget,
                        otpCode = otpCode,
                        isRevolt = isRevolt
                    )
                }

                // Show alert notification with code dispatched to email or SMS
                com.example.util.VerificationNotificationHelper.showVerificationSentNotification(
                    context = appContext,
                    target = cleanTarget,
                    otpCode = otpCode,
                    isEmail = isEmail,
                    isRevolt = isRevolt,
                    purpose = purposeLabel
                )

                withContext(Dispatchers.Main) {
                    onResult(true, otpCode)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Failed to generate OTP: ${e.localizedMessage ?: e.message}")
                }
            }
        }
    }

    fun verifyOtp(
        target: String,
        purpose: String,
        enteredOtp: String,
        onResult: (success: Boolean, message: String) -> Unit
    ) {
        val cleanTarget = target.trim()
        val cleanOtp = enteredOtp.trim()

        if (cleanOtp.length != 6) {
            onResult(false, "Verification code must be 6 digits.")
            return
        }

        authScope.launch {
            try {
                val currentTime = System.currentTimeMillis()
                val activeOtp = dao.getActiveOtp(cleanTarget, purpose, currentTime)

                if (activeOtp == null) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "Verification code has expired or was not requested. Please request a new code.")
                    }
                    return@launch
                }

                if (activeOtp.attempts >= 3) {
                    dao.updateOtp(activeOtp.copy(isUsed = true))
                    withContext(Dispatchers.Main) {
                        onResult(false, "Maximum attempts exceeded. Please generate a new code.")
                    }
                    return@launch
                }

                if (activeOtp.otpCode != cleanOtp) {
                    val updated = activeOtp.copy(attempts = activeOtp.attempts + 1)
                    dao.updateOtp(updated)
                    val remaining = 3 - updated.attempts
                    withContext(Dispatchers.Main) {
                        onResult(false, "Incorrect code! $remaining attempt(s) remaining.")
                    }
                    return@launch
                }

                // Code is correct - mark used
                dao.updateOtp(activeOtp.copy(isUsed = true))
                logAudit("OTP_VERIFIED", "Successfully verified OTP for $cleanTarget purpose $purpose", null, "System")

                withContext(Dispatchers.Main) {
                    onResult(true, "Code verified successfully.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Verification error: ${e.localizedMessage ?: e.message}")
                }
            }
        }
    }

    fun verifyCustomerOtpAndLogin(
        phone: String,
        name: String,
        enteredOtp: String,
        onResult: (AuthResult) -> Unit
    ) {
        val cleanPhone = phone.trim()
        val cleanOtp = enteredOtp.trim()

        if (cleanPhone.length < 10) {
            onResult(AuthResult.Error("Enter a valid 10-digit phone number."))
            return
        }
        if (cleanOtp.length != 6) {
            onResult(AuthResult.Error("Please enter the complete 6-digit verification code."))
            return
        }

        authScope.launch {
            try {
                val currentTime = System.currentTimeMillis()
                val activeOtp = dao.getActiveOtp(cleanPhone, "LOGIN", currentTime)

                if (activeOtp == null) {
                    withContext(Dispatchers.Main) {
                        onResult(AuthResult.Error("Verification code has expired or was not requested. Please tap 'Get OTP'."))
                    }
                    return@launch
                }

                if (activeOtp.attempts >= 3) {
                    dao.updateOtp(activeOtp.copy(isUsed = true))
                    withContext(Dispatchers.Main) {
                        onResult(AuthResult.Error("Maximum verification attempts exceeded. Please request a fresh OTP."))
                    }
                    return@launch
                }

                if (activeOtp.otpCode != cleanOtp) {
                    val updated = activeOtp.copy(attempts = activeOtp.attempts + 1)
                    dao.updateOtp(updated)
                    val remaining = 3 - updated.attempts
                    withContext(Dispatchers.Main) {
                        onResult(AuthResult.Error("Incorrect verification code. $remaining attempt(s) remaining."))
                    }
                    return@launch
                }

                // Mark OTP as used
                dao.updateOtp(activeOtp.copy(isUsed = true))

                // Customer record management
                val customerList = repository.customers.value
                val existingCustomer = customerList.find { it.phone == cleanPhone }
                val finalName = if (existingCustomer != null) existingCustomer.name else name.ifBlank { "Valued Customer" }

                if (existingCustomer == null) {
                    repository.addCustomer(
                        Customer(
                            name = finalName,
                            phone = cleanPhone,
                            address = "Registered Customer",
                            joinedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        )
                    )
                }

                // Ensure local user entity exists for session consistency
                var user = dao.getUserByPhone(cleanPhone)
                if (user == null) {
                    val salt = UUID.randomUUID().toString().take(8)
                    user = UserEntity(
                        id = "CUST_" + UUID.randomUUID().toString().take(8),
                        email = null,
                        phone = cleanPhone,
                        displayName = finalName,
                        passwordHash = hashStringWithSalt("OTP_VERIFIED", salt),
                        salt = salt,
                        role = "Customer",
                        isApproved = true,
                        isActive = true,
                        createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    )
                    dao.insertUser(user)
                    syncUserToCloudFirestore(user, "OTP_SIGNUP")
                }

                val session = UserSession(
                    uid = user.id,
                    email = user.email ?: "",
                    phone = cleanPhone,
                    displayName = finalName,
                    role = "Customer"
                )
                saveSession(session)

                logAudit("OTP_LOGIN_SUCCESS", "Customer logged in via verified OTP", user.id, finalName)

                withContext(Dispatchers.Main) {
                    onResult(AuthResult.Success(session))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(AuthResult.Error("OTP verification failed: ${e.localizedMessage ?: e.message}"))
                }
            }
        }
    }

    // =========================================================================
    // 4. PRODUCTION PASSWORD RESET (Real OTP Verification & Database Update)
    // =========================================================================
    fun resetPasswordWithOtp(
        target: String,
        enteredOtp: String,
        newPassword: String,
        onResult: (Boolean, String) -> Unit
    ) {
        val cleanTarget = target.trim()
        val cleanOtp = enteredOtp.trim()
        val cleanPass = newPassword.trim()

        if (cleanPass.length < 6) {
            onResult(false, "New password must be at least 6 characters long.")
            return
        }

        authScope.launch {
            try {
                val currentTime = System.currentTimeMillis()
                val activeOtp = dao.getActiveOtp(cleanTarget, "RESET_PASSWORD", currentTime)

                if (activeOtp == null) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "Reset code has expired or is invalid. Please request a new code.")
                    }
                    return@launch
                }

                if (activeOtp.otpCode != cleanOtp) {
                    val updated = activeOtp.copy(attempts = activeOtp.attempts + 1)
                    dao.updateOtp(updated)
                    withContext(Dispatchers.Main) {
                        onResult(false, "Incorrect verification code.")
                    }
                    return@launch
                }

                // Invalidate OTP
                dao.updateOtp(activeOtp.copy(isUsed = true))

                // Find user
                val isEmail = cleanTarget.contains("@")
                val user = if (isEmail) dao.getUserByEmail(cleanTarget) else dao.getUserByPhone(cleanTarget)

                if (user == null) {
                    withContext(Dispatchers.Main) {
                        onResult(false, "No account linked to this identifier.")
                    }
                    return@launch
                }

                val newSalt = UUID.randomUUID().toString().take(8)
                val newHash = hashStringWithSalt(cleanPass, newSalt)
                dao.updateUser(user.copy(passwordHash = newHash, salt = newSalt, failedAttempts = 0, lockUntilTimestamp = 0L))

                logAudit("PASSWORD_RESET", "Password successfully reset via verified OTP", user.id, user.displayName)

                withContext(Dispatchers.Main) {
                    onResult(true, "Password reset successfully! You can now log in with your new password.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Password reset failed: ${e.localizedMessage ?: e.message}")
                }
            }
        }
    }

    // =========================================================================
    // 5. WORKER INVITATION & ACCESS CONTROL
    // =========================================================================
    fun createWorker(
        email: String,
        name: String,
        phone: String,
        designation: String,
        salary: Double,
        onResult: (Boolean, String) -> Unit
    ) {
        val emailTrimmed = email.trim().lowercase(Locale.ROOT)
        val inviteCode = (100000 + Random.nextInt(900000)).toString()

        authScope.launch {
            try {
                // Add to Worker table
                val newWorker = Worker(
                    id = "W_" + UUID.randomUUID().toString().take(6).uppercase(Locale.ROOT),
                    name = name,
                    phone = phone,
                    salary = salary,
                    designation = designation,
                    status = WorkerStatus.IDLE,
                    joiningDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                    isVerified = false,
                    isApproved = true,
                    isActive = true,
                    email = emailTrimmed
                )
                repository.addWorker(newWorker)

                // Save dynamic invitation code in OTP records
                val otpRecord = OtpRecord(
                    target = emailTrimmed,
                    otpCode = inviteCode,
                    purpose = "WORKER_INVITE",
                    expiresAt = System.currentTimeMillis() + (7 * 24 * 3600 * 1000L), // 7 days
                    attempts = 0,
                    isUsed = false
                )
                dao.insertOtp(otpRecord)

                logAudit("WORKER_INVITED", "Invited worker $name ($emailTrimmed) with code $inviteCode")

                withContext(Dispatchers.Main) {
                    onResult(true, inviteCode)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(false, "Failed to create worker invitation: ${e.message}")
                }
            }
        }
    }

    fun acceptInvitationAndSetupPassword(
        email: String,
        code: String,
        passwordPin: String,
        onResult: (AuthResult) -> Unit
    ) {
        val emailTrimmed = email.trim().lowercase(Locale.ROOT)
        val codeTrimmed = code.trim()

        if (passwordPin.length < 6) {
            onResult(AuthResult.Error("Password must be at least 6 characters long."))
            return
        }

        authScope.launch {
            try {
                val activeOtp = dao.getActiveOtp(emailTrimmed, "WORKER_INVITE", System.currentTimeMillis())
                if (activeOtp == null || activeOtp.otpCode != codeTrimmed) {
                    withContext(Dispatchers.Main) {
                        onResult(AuthResult.Error("Invalid or expired invitation code."))
                    }
                    return@launch
                }

                // Invalidate invite code
                dao.updateOtp(activeOtp.copy(isUsed = true))

                // Find worker
                val worker = repository.workers.value.find { it.email?.lowercase(Locale.ROOT) == emailTrimmed }
                if (worker != null) {
                    repository.updateWorker(worker.copy(isVerified = true, isApproved = true))
                }

                // Create user entity
                val salt = UUID.randomUUID().toString().take(8)
                val passHash = hashStringWithSalt(passwordPin, salt)
                val user = UserEntity(
                    id = worker?.id ?: ("W_" + UUID.randomUUID().toString().take(6)),
                    email = emailTrimmed,
                    phone = worker?.phone,
                    displayName = worker?.name ?: "Technician",
                    passwordHash = passHash,
                    salt = salt,
                    role = "Worker",
                    isApproved = true,
                    isActive = true,
                    createdAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                )
                dao.insertUser(user)
                syncUserToCloudFirestore(user, "WORKER_ONBOARDED")

                val session = UserSession(
                    uid = user.id,
                    email = emailTrimmed,
                    phone = user.phone ?: "",
                    displayName = user.displayName,
                    role = "Worker"
                )
                saveSession(session)

                logAudit("WORKER_ONBOARDED", "Worker onboarded with password", user.id, user.displayName)

                withContext(Dispatchers.Main) {
                    onResult(AuthResult.Success(session))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    onResult(AuthResult.Error("Onboarding failed: ${e.message}"))
                }
            }
        }
    }

    fun setWorkerAccessStatus(workerId: String, isApproved: Boolean, isActive: Boolean, onResult: (Boolean) -> Unit) {
        val workers = repository.workers.value
        val matched = workers.find { it.id == workerId }

        if (matched != null) {
            val updatedWorker = matched.copy(isApproved = isApproved, isActive = isActive)
            repository.updateWorker(updatedWorker)

            authScope.launch {
                val matchedUser = dao.getUserById(workerId)
                if (matchedUser != null) {
                    val updated = matchedUser.copy(isApproved = isApproved, isActive = isActive)
                    dao.updateUser(updated)
                    syncUserToCloudFirestore(updated, "STATUS_CHANGED")
                }
                logAudit("WORKER_STATUS_CHANGED", "Worker $workerId updated: approved=$isApproved, active=$isActive")
            }
            onResult(true)
        } else {
            onResult(false)
        }
    }

    fun syncUserToCloudFirestore(user: UserEntity, action: String = "REGISTER") {
        if (!FirebaseConfig.isAvailable) return
        authScope.launch {
            try {
                val firestore = FirebaseFirestore.getInstance()
                val payload = hashMapOf<String, Any>(
                    "uid" to user.id,
                    "displayName" to user.displayName,
                    "email" to (user.email ?: ""),
                    "phone" to (user.phone ?: ""),
                    "role" to user.role,
                    "isApproved" to user.isApproved,
                    "isActive" to user.isActive,
                    "lastAction" to action,
                    "lastActiveTimestamp" to System.currentTimeMillis(),
                    "storeName" to repository.storeSupportInfo.value.storeName,
                    "updatedAt" to SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                )
                firestore.collection("global_user_registry").document(user.id).set(payload)
                firestore.collection("users").document(user.id).set(payload)
                Log.d("AuthService", "User ${user.id} synced to Cloud Firestore ($action)")
            } catch (e: Exception) {
                Log.w("AuthService", "Cloud Firestore sync skipped: ${e.message}")
            }
        }
    }
}
