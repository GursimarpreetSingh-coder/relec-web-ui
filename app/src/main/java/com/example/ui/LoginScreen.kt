package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonRed
import com.example.ui.viewmodels.AuthViewModel
import com.example.R
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.data.ReLectRepository
import com.example.ui.components.BrandLogoView
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class AuthScreenMode {
    SIGN_IN,
    SIGN_UP,
    FORGOT_PASSWORD
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    accentColor: Color,
    isDarkTheme: Boolean,
    brandTheme: String = "cyber_blue",
    onBrandThemeChange: ((String) -> Unit)? = null,
    onLoginSuccess: (String) -> Unit, // passes selected role
    onThemeToggle: () -> Unit,
    authViewModel: AuthViewModel,
    repository: ReLectRepository? = null
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val isRevolt = brandTheme == "revolt_crimson"

    // View States observed from ViewModel
    val session by authViewModel.sessionState.collectAsState()
    val isViewModelLoading by authViewModel.isLoading.collectAsState()
    val authError by authViewModel.errorMessage.collectAsState()
    val successMsg by authViewModel.successMessage.collectAsState()
    val inviteCodeState by authViewModel.inviteCode.collectAsState()
    val otpSent by authViewModel.otpSent.collectAsState()
    val simulatedOtp by authViewModel.lastSimulatedOtp.collectAsState()
    val deliveryStatus by authViewModel.deliveryStatus.collectAsState()

    var authMode by remember { mutableStateOf(AuthScreenMode.SIGN_IN) }
    var selectedRole by remember { mutableStateOf("Admin") } // Admin, Worker, Customer
    var isWorkerOnboarding by remember { mutableStateOf(false) } // Worker invite flow

    // Sign In Inputs
    var loginInputEmailOrPhone by remember { mutableStateOf("") }
    var loginInputCredentialPassword by remember { mutableStateOf("") }
    var credentialVisible by remember { mutableStateOf(false) }

    // Sign Up OTP & Verification state
    var signupOtp by remember { mutableStateOf("") }
    var signupOtpDispatched by remember { mutableStateOf(false) }
    var isSignupEmailVerified by remember { mutableStateOf(false) }
    var isSignupPhoneVerified by remember { mutableStateOf(false) }
    var signupEmailOtpSent by remember { mutableStateOf(false) }
    var signupPhoneOtpSent by remember { mutableStateOf(false) }
    var signupEmailOtpInput by remember { mutableStateOf("") }
    var signupPhoneOtpInput by remember { mutableStateOf("") }
    var isSendingEmailOtp by remember { mutableStateOf(false) }
    var isSendingPhoneOtp by remember { mutableStateOf(false) }

    // Worker Onboarding Inputs
    var inputOnboardingEmail by remember { mutableStateOf("") }
    var inputOnboardingCode by remember { mutableStateOf("") }
    var inputOnboardingPin by remember { mutableStateOf("") }

    // Customer OTP Inputs
    var inputCustomerName by remember { mutableStateOf("") }
    var inputCustomerPhone by remember { mutableStateOf("") }
    var inputCustomerOtp by remember { mutableStateOf("") }

    // Sign Up Inputs
    var signupName by remember { mutableStateOf("") }
    var signupEmail by remember { mutableStateOf("") }
    var signupPhone by remember { mutableStateOf("") }
    var signupRole by remember { mutableStateOf("Customer") }
    var signupPassword by remember { mutableStateOf("") }
    var signupConfirmPassword by remember { mutableStateOf("") }
    var signupPasswordVisible by remember { mutableStateOf(false) }

    // Forgot Password Inputs
    var resetTarget by remember { mutableStateOf("") }
    var resetOtp by remember { mutableStateOf("") }
    var resetNewPassword by remember { mutableStateOf("") }
    var resetConfirmPassword by remember { mutableStateOf("") }
    var resetOtpDispatched by remember { mutableStateOf(false) }

    var showSuccessDialog by remember { mutableStateOf(false) }

    val isLight = !isDarkTheme
    val cardColor = if (isLight) Color.White else Color(0xFF1E293B)
    val textColor = if (isLight) Color(0xFF0F172A) else Color.White
    val grayText = if (isLight) Color(0xFF475569) else Color(0xFF94A3B8)
    val borderStrokeColor = if (isLight) Color(0xFFE2E8F0) else Color(0xFF334155)

    // Clear errors when switching tabs
    LaunchedEffect(authMode, selectedRole) {
        authViewModel.resetError()
        authViewModel.resetSuccessMessage()
    }

    val backgroundBrush = if (isRevolt) {
        if (isDarkTheme) {
            Brush.verticalGradient(listOf(Color(0xFF140306), Color(0xFF1F060A), Color(0xFF0F0204)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFFFF5F5), Color(0xFFFEE2E2), Color(0xFFFEE2E2)))
        }
    } else {
        if (isDarkTheme) {
            Brush.verticalGradient(listOf(Color(0xFF020712), Color(0xFF06152F), Color(0xFF020712)))
        } else {
            Brush.verticalGradient(listOf(Color(0xFFF0F7FF), Color(0xFFE0EFFF), Color(0xFFD0E6FF)))
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
            .drawBehind {
                val gridSpacing = 32.dp.toPx()
                val gridColor = if (isDarkTheme) {
                    if (isRevolt) Color(0xFF3B0D11).copy(alpha = 0.25f) else Color(0xFF1E293B).copy(alpha = 0.18f)
                } else {
                    if (isRevolt) Color(0xFFFECACA).copy(alpha = 0.35f) else Color(0xFFCBD5E1).copy(alpha = 0.22f)
                }
                val strokeWidth = 0.8f.dp.toPx()
                var y = 0f
                while (y < size.height) {
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth)
                    y += gridSpacing
                }
                var x = 0f
                while (x < size.width) {
                    drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), strokeWidth)
                    x += gridSpacing
                }
            }
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.TopCenter
    ) {
        val scrollState = rememberScrollState()
        LaunchedEffect(authMode) {
            scrollState.scrollTo(0)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .padding(top = 76.dp, bottom = 20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Branding & Theme Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isRevolt) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            BrandLogoView(
                                repository = repository,
                                isRevolt = true,
                                modifier = Modifier
                                    .fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "RE-VOLT",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textColor,
                                    letterSpacing = (-0.5).sp,
                                    lineHeight = 22.sp
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(accentColor.copy(alpha = 0.12f))
                                        .border(0.8.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "ENTERPRISE",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = accentColor,
                                        letterSpacing = 0.6.sp,
                                        style = LocalTextStyle.current.copy(
                                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                                            lineHeight = 9.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                }
                            }
                            Text(
                                text = "Rattan Electricals",
                                fontSize = 11.5.sp,
                                color = grayText,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 13.sp
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White)
                                .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            BrandLogoView(
                                repository = repository,
                                isRevolt = false,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(2.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "ReLec",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textColor,
                                    letterSpacing = (-0.5).sp,
                                    lineHeight = 22.sp
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(accentColor.copy(alpha = 0.12f))
                                        .border(0.8.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "PRO ERP",
                                        fontSize = 8.5.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = accentColor,
                                        letterSpacing = 0.6.sp,
                                        style = LocalTextStyle.current.copy(
                                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                                            lineHeight = 9.sp,
                                            textAlign = TextAlign.Center
                                        )
                                    )
                                }
                            }
                            Text(
                                text = "Rattan Electricals",
                                fontSize = 11.5.sp,
                                color = grayText,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(vertical = 4.dp)
                ) {
                    if (onBrandThemeChange != null) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.12f))
                                .border(1.2.dp, accentColor.copy(alpha = 0.5f), CircleShape)
                                .clickable {
                                    val nextTheme = if (brandTheme == "cyber_blue") "revolt_crimson" else "cyber_blue"
                                    onBrandThemeChange(nextTheme)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Switch Theme",
                                tint = accentColor,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0))
                            .border(
                                1.2.dp,
                                if (isDarkTheme) Color(0xFF475569) else Color(0xFFCBD5E1),
                                CircleShape
                            )
                            .clickable(onClick = onThemeToggle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                            contentDescription = "Toggle Theme Mode",
                            tint = if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFF334155),
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Themed Brand Hero Banner
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(112.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRevolt) Color.White else Color(0xFF0F172A)
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(if (isRevolt) Color.White else Color(0xFF0B1329)),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(
                                id = if (isRevolt) R.drawable.revolt_banner_rattan_official_1790190332784 else R.drawable.relec_banner_clean_1789665642270
                            ),
                            contentDescription = if (isRevolt) "RE-VOLT Crimson Banner" else "ReLec Cyber Blue Banner",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.FillBounds
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Navigation Segmented Control: Sign In / Create Account / Reset Password
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFE2E8F0))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(
                    AuthScreenMode.SIGN_IN to "Sign In",
                    AuthScreenMode.SIGN_UP to "Sign Up",
                    AuthScreenMode.FORGOT_PASSWORD to "Reset PIN"
                ).forEach { (mode, label) ->
                    val isSelected = authMode == mode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) cardColor else Color.Transparent)
                            .clickable { authMode = mode }
                            .padding(vertical = 7.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Black else FontWeight.SemiBold,
                            color = if (isSelected) accentColor else grayText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Auth Form Card with Smooth Gradient Merge
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(
                            accentColor.copy(alpha = 0.5f),
                            borderStrokeColor,
                            borderStrokeColor.copy(alpha = 0.25f)
                        )
                    )
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    cardColor,
                                    if (isDarkTheme) {
                                        if (isRevolt) Color(0xFF190609) else Color(0xFF0F172A)
                                    } else {
                                        if (isRevolt) Color(0xFFFFF7F7) else Color(0xFFF8FAFC)
                                    },
                                    cardColor
                                )
                            )
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                    // Inline Error Display
                    if (authError != null) {
                        val isAccountNotFound = authError?.contains("Account not found", ignoreCase = true) == true ||
                                authError?.contains("No registered account", ignoreCase = true) == true
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAccountNotFound) Color(0xFFFEF2F2) else NeonRed.copy(alpha = 0.12f)
                            ),
                            border = BorderStroke(1.5.dp, if (isAccountNotFound) Color(0xFFEF4444) else NeonRed.copy(alpha = 0.35f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.ErrorOutline,
                                        contentDescription = null,
                                        tint = if (isAccountNotFound) Color(0xFFDC2626) else NeonRed,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (isAccountNotFound) "ACCOUNT NOT FOUND" else "AUTHENTICATION ERROR",
                                        color = if (isAccountNotFound) Color(0xFFDC2626) else NeonRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 0.5.sp
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = authError ?: "",
                                    color = if (isAccountNotFound) Color(0xFF991B1B) else NeonRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 17.sp
                                )
                                if (isAccountNotFound) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Button(
                                        onClick = {
                                            authMode = AuthScreenMode.SIGN_UP
                                            authViewModel.clearErrors()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                        modifier = Modifier.fillMaxWidth().height(40.dp)
                                    ) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Create Account / Sign Up Now",
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Inline Success Message Display
                    if (successMsg != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(NeonGreen.copy(alpha = 0.12f))
                                .border(1.dp, NeonGreen.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(successMsg ?: "", color = NeonGreen, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    when (authMode) {
                        // ==========================================
                        // 1. SIGN IN MODE
                        // ==========================================
                        AuthScreenMode.SIGN_IN -> {
                            // Role Selector Tabs
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isDarkTheme) Color(0xFF090D1A) else Color(0xFFF1F5F9))
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf("Admin", "Worker", "Customer").forEach { role ->
                                    val isSel = selectedRole == role
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(if (isSel) accentColor.copy(alpha = 0.15f) else Color.Transparent)
                                            .border(
                                                1.dp,
                                                if (isSel) accentColor.copy(alpha = 0.4f) else Color.Transparent,
                                                RoundedCornerShape(9.dp)
                                            )
                                            .clickable {
                                                selectedRole = role
                                                isWorkerOnboarding = false
                                                when (role) {
                                                    "Admin" -> {
                                                        loginInputEmailOrPhone = "gursimarpsr@gmail.com"
                                                        loginInputCredentialPassword = "Admin@123"
                                                    }
                                                    "Worker" -> {
                                                        loginInputEmailOrPhone = "worker@relect.com"
                                                        loginInputCredentialPassword = "Worker@123"
                                                    }
                                                    "Customer" -> {
                                                        inputCustomerPhone = "9812045612"
                                                        inputCustomerName = "Rajesh Kumar"
                                                    }
                                                }
                                            }
                                            .padding(vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = role,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSel) FontWeight.Black else FontWeight.Bold,
                                            color = if (isSel) accentColor else grayText
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Role Hint Banner
                            Text(
                                text = when (selectedRole) {
                                    "Admin" -> "Master Superior Administrator Console"
                                    "Worker" -> "Registered technician phone/email and secure password"
                                    else -> "Instant customer access via 6-digit phone verification OTP"
                                },
                                fontSize = 11.sp,
                                color = grayText,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            when (selectedRole) {
                                "Admin", "Worker" -> {
                                    if (selectedRole == "Worker" && isWorkerOnboarding) {
                                        // Worker Invite Setup
                                        Text("Worker Invitation Setup", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = textColor)
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedTextField(
                                            value = inputOnboardingEmail,
                                            onValueChange = { inputOnboardingEmail = it },
                                            label = { Text("Invited Email Address") },
                                            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = inputOnboardingCode,
                                            onValueChange = { inputOnboardingCode = it },
                                            label = { Text("6-Digit Invite Code") },
                                            leadingIcon = { Icon(Icons.Default.Pin, contentDescription = null) },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        OutlinedTextField(
                                            value = inputOnboardingPin,
                                            onValueChange = { inputOnboardingPin = it },
                                            label = { Text("Create New Password (min 6 chars)") },
                                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                            visualTransformation = PasswordVisualTransformation(),
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        TextButton(onClick = { isWorkerOnboarding = false }, modifier = Modifier.align(Alignment.End)) {
                                            Text("Back to Standard Login", color = accentColor, fontSize = 11.5.sp)
                                        }
                                    } else {
                                        // Standard Email / Phone + Password
                                        OutlinedTextField(
                                            value = loginInputEmailOrPhone,
                                            onValueChange = { loginInputEmailOrPhone = it },
                                            label = { Text(if (selectedRole == "Admin") "Admin Email or Phone" else "Worker Phone or Email") },
                                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                            placeholder = { Text(if (selectedRole == "Admin") "admin@company.com or phone" else "worker@company.com or phone") },
                                            modifier = Modifier.fillMaxWidth().testTag("login_identifier_input"),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        OutlinedTextField(
                                            value = loginInputCredentialPassword,
                                            onValueChange = { loginInputCredentialPassword = it },
                                            label = { Text("Password") },
                                            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                            trailingIcon = {
                                                IconButton(onClick = { credentialVisible = !credentialVisible }) {
                                                    Icon(
                                                        imageVector = if (credentialVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                                        contentDescription = null
                                                    )
                                                }
                                            },
                                            visualTransformation = if (credentialVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                            modifier = Modifier.fillMaxWidth().testTag("login_password_input"),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )

                                        Spacer(modifier = Modifier.height(3.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            if (selectedRole == "Worker") {
                                                TextButton(
                                                    onClick = { isWorkerOnboarding = true },
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp)
                                                ) {
                                                    Text("Invited Worker Setup", color = accentColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                }
                                            } else {
                                                Spacer(modifier = Modifier.weight(1f))
                                            }

                                            TextButton(
                                                onClick = {
                                                    authMode = AuthScreenMode.FORGOT_PASSWORD
                                                    resetTarget = loginInputEmailOrPhone
                                                    authViewModel.clearErrors()
                                                },
                                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 0.dp),
                                                modifier = Modifier.testTag("forgot_password_button")
                                            ) {
                                                Icon(Icons.Default.LockReset, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    "Forgot Password?",
                                                    color = accentColor,
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                                )
                                            }
                                        }
                                    }
                                }

                                "Customer" -> {
                                    OutlinedTextField(
                                        value = inputCustomerPhone,
                                        onValueChange = { inputCustomerPhone = it },
                                        label = { Text("10-Digit Mobile Number") },
                                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        placeholder = { Text("9812045612") },
                                        modifier = Modifier.fillMaxWidth().testTag("customer_phone_input"),
                                        shape = RoundedCornerShape(14.dp),
                                        singleLine = true
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    OutlinedTextField(
                                        value = inputCustomerName,
                                        onValueChange = { inputCustomerName = it },
                                        label = { Text("Full Name (Optional for existing customer)") },
                                        leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null) },
                                        modifier = Modifier.fillMaxWidth().testTag("customer_name_input"),
                                        shape = RoundedCornerShape(14.dp),
                                        singleLine = true
                                    )

                                    if (otpSent) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Surface(
                                            color = accentColor.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                ) {
                                                    Icon(Icons.Default.Security, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                                                    Text(
                                                        "SMS Verification Code Dispatched",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = textColor
                                                    )
                                                }
                                                Text(
                                                    "Check your phone for the 6-digit code sent to $inputCustomerPhone",
                                                    fontSize = 11.sp,
                                                    color = grayText,
                                                    modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                                                )

                                                val activeOtpCode = simulatedOtp ?: repository?.lastDispatchedOtp?.collectAsState()?.value?.code
                                                if (!activeOtpCode.isNullOrBlank()) {
                                                    Surface(
                                                        color = accentColor.copy(alpha = 0.12f),
                                                        shape = RoundedCornerShape(10.dp),
                                                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                                                        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)
                                                    ) {
                                                        Row(
                                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.SpaceBetween
                                                        ) {
                                                            Column(modifier = Modifier.weight(1f)) {
                                                                Text(
                                                                    "SMS OTP: $activeOtpCode",
                                                                    fontSize = 14.sp,
                                                                    fontWeight = FontWeight.Black,
                                                                    color = accentColor,
                                                                    letterSpacing = 1.sp
                                                                )
                                                                Text(
                                                                    "Dispatched to $inputCustomerPhone",
                                                                    fontSize = 10.sp,
                                                                    color = grayText
                                                                )
                                                            }
                                                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                                                Button(
                                                                    onClick = {
                                                                        inputCustomerOtp = activeOtpCode
                                                                        Toast.makeText(context, "OTP Code auto-filled!", Toast.LENGTH_SHORT).show()
                                                                    },
                                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                                    modifier = Modifier.height(30.dp),
                                                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                                                ) {
                                                                    Text("⚡ Auto-fill", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                                }
                                                                OutlinedButton(
                                                                    onClick = {
                                                                        com.example.util.VerificationNotificationHelper.openSmsAppWithPrefilledOtp(
                                                                            context = context,
                                                                            phone = inputCustomerPhone,
                                                                            otpCode = activeOtpCode,
                                                                            isRevolt = isRevolt
                                                                        )
                                                                    },
                                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                                    modifier = Modifier.height(30.dp)
                                                                ) {
                                                                    Text("💬 SMS App", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                OutlinedTextField(
                                                    value = inputCustomerOtp,
                                                    onValueChange = { if (it.length <= 6) inputCustomerOtp = it.filter { ch -> ch.isDigit() } },
                                                    placeholder = {
                                                        Text("000000", letterSpacing = 6.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                                    },
                                                    textStyle = LocalTextStyle.current.copy(
                                                        fontSize = 20.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        letterSpacing = 6.sp,
                                                        textAlign = TextAlign.Center,
                                                        color = textColor
                                                    ),
                                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                                    modifier = Modifier.fillMaxWidth().height(54.dp).testTag("customer_otp_input"),
                                                    shape = RoundedCornerShape(12.dp),
                                                    singleLine = true
                                                )
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                                    horizontalArrangement = Arrangement.End
                                                ) {
                                                    TextButton(
                                                        onClick = {
                                                            authViewModel.sendCustomerOtp(inputCustomerPhone) { success ->
                                                                if (success) Toast.makeText(context, "New SMS code dispatched!", Toast.LENGTH_SHORT).show()
                                                            }
                                                        },
                                                        contentPadding = PaddingValues(0.dp)
                                                    ) {
                                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                                        Spacer(modifier = Modifier.width(4.dp))
                                                        Text("Resend SMS Code", fontSize = 11.sp)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // ==========================================
                        // 2. SIGN UP MODE
                        // ==========================================
                        AuthScreenMode.SIGN_UP -> {
                            Text("Create a Verified Account", fontSize = 16.sp, fontWeight = FontWeight.Black, color = textColor)
                            Text("Standard registration with Gmail / SMS code verification.", fontSize = 11.sp, color = grayText)
                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = signupName,
                                onValueChange = { signupName = it },
                                label = { Text("Full Legal Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // ----------------------------------------------------
                            // 1. Google Email Verification (Gmail) Card
                            // ----------------------------------------------------
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isLight) Color(0xFFF8FAFC) else Color(0xFF1E293B)),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSignupEmailVerified) Color(0xFF22C55E)
                                    else if (signupEmailOtpSent) accentColor
                                    else borderStrokeColor
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Header with Status Badge
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                color = if (isSignupEmailVerified) Color(0xFFDCFCE7) else accentColor.copy(alpha = 0.12f),
                                                shape = CircleShape,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.Email,
                                                        contentDescription = null,
                                                        tint = if (isSignupEmailVerified) Color(0xFF16A34A) else accentColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Column {
                                                Text(
                                                    "Email Verification (Gmail)",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textColor
                                                )
                                                Text(
                                                    "Direct cloud dispatch to inbox",
                                                    fontSize = 10.5.sp,
                                                    color = grayText
                                                )
                                            }
                                        }

                                        // Status Pill
                                        Surface(
                                            color = when {
                                                isSignupEmailVerified -> Color(0xFFDCFCE7)
                                                signupEmailOtpSent -> accentColor.copy(alpha = 0.15f)
                                                else -> borderStrokeColor.copy(alpha = 0.5f)
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = when {
                                                    isSignupEmailVerified -> "✓ Verified"
                                                    signupEmailOtpSent -> "Code Sent"
                                                    else -> "Required"
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when {
                                                    isSignupEmailVerified -> Color(0xFF15803D)
                                                    signupEmailOtpSent -> accentColor
                                                    else -> grayText
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Email Input Field
                                    OutlinedTextField(
                                        value = signupEmail,
                                        onValueChange = {
                                            signupEmail = it
                                            if (isSignupEmailVerified) isSignupEmailVerified = false
                                            if (signupEmailOtpSent) signupEmailOtpSent = false
                                        },
                                        label = { Text("Gmail / Email Address") },
                                        placeholder = { Text("e.g. gursimarpsr@gmail.com") },
                                        leadingIcon = { Icon(Icons.Default.AlternateEmail, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        enabled = !isSignupEmailVerified,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )

                                    if (isSignupEmailVerified) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            color = Color(0xFFDCFCE7),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Verified: $signupEmail", color = Color(0xFF15803D), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                                }
                                                TextButton(
                                                    onClick = {
                                                        isSignupEmailVerified = false
                                                        signupEmailOtpSent = false
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                                ) {
                                                    Text("Change", fontSize = 11.sp, color = Color(0xFF15803D))
                                                }
                                            }
                                        }
                                    } else if (signupEmailOtpSent) {
                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Dispatch Confirmation Banner
                                        Surface(
                                            color = accentColor.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.MarkEmailRead, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp).padding(top = 1.dp))
                                                Column {
                                                    Text(
                                                        "6-digit code dispatched to $signupEmail via Resend Cloud.",
                                                        fontSize = 11.sp,
                                                        color = textColor,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Text(
                                                        "Please check your Inbox (or Spam/Promotions folder).",
                                                        fontSize = 10.sp,
                                                        color = grayText
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        // EQUAL-HEIGHT 54dp OTP Input and Verify Box
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = signupEmailOtpInput,
                                                onValueChange = {
                                                    if (it.length <= 6) signupEmailOtpInput = it.filter { ch -> ch.isDigit() }
                                                },
                                                placeholder = {
                                                    Text(
                                                        "000000",
                                                        letterSpacing = 6.sp,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        color = grayText.copy(alpha = 0.4f)
                                                    )
                                                },
                                                textStyle = LocalTextStyle.current.copy(
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 6.sp,
                                                    textAlign = TextAlign.Center,
                                                    color = textColor
                                                ),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                                modifier = Modifier.weight(1f).height(54.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                singleLine = true
                                            )

                                            Button(
                                                onClick = {
                                                    if (signupEmailOtpInput.trim().length != 6) {
                                                        Toast.makeText(context, "Enter the 6-digit code from Gmail", Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }
                                                    authViewModel.verifySignupOtp(signupEmail.trim(), signupEmailOtpInput.trim()) { ok, msg ->
                                                        if (ok) {
                                                            isSignupEmailVerified = true
                                                            Toast.makeText(context, "Email successfully verified!", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                enabled = signupEmailOtpInput.trim().length == 6,
                                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                                modifier = Modifier.height(54.dp)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Verify", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Helpers
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = { com.example.util.VerificationNotificationHelper.openEmailApp(context) },
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Open Gmail App", fontSize = 11.sp)
                                            }
                                            TextButton(
                                                onClick = {
                                                    authViewModel.sendSignupOtp(signupEmail.trim()) { ok ->
                                                        if (ok) Toast.makeText(context, "New code dispatched to Gmail!", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Resend Code", fontSize = 11.sp)
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        Button(
                                            onClick = {
                                                isSendingEmailOtp = true
                                                authViewModel.sendSignupOtp(signupEmail.trim()) { ok ->
                                                    isSendingEmailOtp = false
                                                    if (ok) signupEmailOtpSent = true
                                                }
                                            },
                                            enabled = !isSendingEmailOtp && signupEmail.contains("@") && signupEmail.contains("."),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                        ) {
                                            if (isSendingEmailOtp) {
                                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Dispatching Real Email...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            } else {
                                                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Send Verification Code to Gmail", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // ----------------------------------------------------
                            // 2. Mobile Phone Verification (SMS) Card
                            // ----------------------------------------------------
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = if (isLight) Color(0xFFF8FAFC) else Color(0xFF1E293B)),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSignupPhoneVerified) Color(0xFF22C55E)
                                    else if (signupPhoneOtpSent) accentColor
                                    else borderStrokeColor
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    // Header with Status Badge
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Surface(
                                                color = if (isSignupPhoneVerified) Color(0xFFDCFCE7) else accentColor.copy(alpha = 0.12f),
                                                shape = CircleShape,
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Box(contentAlignment = Alignment.Center) {
                                                    Icon(
                                                        Icons.Default.Phone,
                                                        contentDescription = null,
                                                        tint = if (isSignupPhoneVerified) Color(0xFF16A34A) else accentColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Column {
                                                Text(
                                                    "Mobile Phone Verification (SMS)",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = textColor
                                                )
                                                Text(
                                                    "Secure SMS dispatch to phone",
                                                    fontSize = 10.5.sp,
                                                    color = grayText
                                                )
                                            }
                                        }

                                        // Status Pill
                                        Surface(
                                            color = when {
                                                isSignupPhoneVerified -> Color(0xFFDCFCE7)
                                                signupPhoneOtpSent -> accentColor.copy(alpha = 0.15f)
                                                else -> borderStrokeColor.copy(alpha = 0.5f)
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                text = when {
                                                    isSignupPhoneVerified -> "✓ Verified"
                                                    signupPhoneOtpSent -> "Code Sent"
                                                    else -> "Optional"
                                                },
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when {
                                                    isSignupPhoneVerified -> Color(0xFF15803D)
                                                    signupPhoneOtpSent -> accentColor
                                                    else -> grayText
                                                },
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Phone Input Field
                                    OutlinedTextField(
                                        value = signupPhone,
                                        onValueChange = {
                                            signupPhone = it
                                            if (isSignupPhoneVerified) isSignupPhoneVerified = false
                                            if (signupPhoneOtpSent) signupPhoneOtpSent = false
                                        },
                                        label = { Text("10-Digit Mobile Number") },
                                        placeholder = { Text("e.g. 9872134590") },
                                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        enabled = !isSignupPhoneVerified,
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )

                                    if (isSignupPhoneVerified) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Surface(
                                            color = Color(0xFFDCFCE7),
                                            shape = RoundedCornerShape(10.dp),
                                            border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(16.dp))
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text("Verified: $signupPhone", color = Color(0xFF15803D), fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                                                }
                                                TextButton(
                                                    onClick = {
                                                        isSignupPhoneVerified = false
                                                        signupPhoneOtpSent = false
                                                    },
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                                ) {
                                                    Text("Change", fontSize = 11.sp, color = Color(0xFF15803D))
                                                }
                                            }
                                        }
                                    } else if (signupPhoneOtpSent) {
                                        Spacer(modifier = Modifier.height(10.dp))

                                        // Dispatch Confirmation Banner
                                        Surface(
                                            color = accentColor.copy(alpha = 0.08f),
                                            shape = RoundedCornerShape(10.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.Top,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(Icons.Default.Sms, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp).padding(top = 1.dp))
                                                Column {
                                                    Text(
                                                        "6-digit code dispatched to $signupPhone via SMS.",
                                                        fontSize = 11.sp,
                                                        color = textColor,
                                                        fontWeight = FontWeight.SemiBold
                                                    )
                                                    Text(
                                                        "Check your incoming text messages.",
                                                        fontSize = 10.sp,
                                                        color = grayText
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(10.dp))

                                        val activeSignupPhoneOtp = simulatedOtp ?: repository?.lastDispatchedOtp?.collectAsState()?.value?.code
                                        if (!activeSignupPhoneOtp.isNullOrBlank()) {
                                            Surface(
                                                color = accentColor.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            "SMS Code: $activeSignupPhoneOtp",
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = accentColor,
                                                            letterSpacing = 1.sp
                                                        )
                                                        Text(
                                                            "Dispatched to $signupPhone",
                                                            fontSize = 9.5.sp,
                                                            color = grayText
                                                        )
                                                    }
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Button(
                                                            onClick = {
                                                                signupPhoneOtpInput = activeSignupPhoneOtp
                                                                Toast.makeText(context, "OTP Code auto-filled!", Toast.LENGTH_SHORT).show()
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            modifier = Modifier.height(28.dp),
                                                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                                        ) {
                                                            Text("⚡ Auto-fill", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                        }
                                                        OutlinedButton(
                                                            onClick = {
                                                                com.example.util.VerificationNotificationHelper.openSmsAppWithPrefilledOtp(
                                                                    context = context,
                                                                    phone = signupPhone,
                                                                    otpCode = activeSignupPhoneOtp,
                                                                    isRevolt = isRevolt
                                                                )
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            modifier = Modifier.height(28.dp)
                                                        ) {
                                                            Text("💬 SMS App", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        // EQUAL-HEIGHT 54dp OTP Input and Verify Box
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = signupPhoneOtpInput,
                                                onValueChange = {
                                                    if (it.length <= 6) signupPhoneOtpInput = it.filter { ch -> ch.isDigit() }
                                                },
                                                placeholder = {
                                                    Text(
                                                        "000000",
                                                        letterSpacing = 6.sp,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.fillMaxWidth(),
                                                        color = grayText.copy(alpha = 0.4f)
                                                    )
                                                },
                                                textStyle = LocalTextStyle.current.copy(
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    letterSpacing = 6.sp,
                                                    textAlign = TextAlign.Center,
                                                    color = textColor
                                                ),
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                                modifier = Modifier.weight(1f).height(54.dp),
                                                shape = RoundedCornerShape(12.dp),
                                                singleLine = true
                                            )

                                            Button(
                                                onClick = {
                                                    if (signupPhoneOtpInput.trim().length != 6) {
                                                        Toast.makeText(context, "Enter the 6-digit code from SMS", Toast.LENGTH_SHORT).show()
                                                        return@Button
                                                    }
                                                    authViewModel.verifySignupOtp(signupPhone.trim(), signupPhoneOtpInput.trim()) { ok, msg ->
                                                        if (ok) {
                                                            isSignupPhoneVerified = true
                                                            Toast.makeText(context, "Mobile number successfully verified!", Toast.LENGTH_SHORT).show()
                                                        } else {
                                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                                        }
                                                    }
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                enabled = signupPhoneOtpInput.trim().length == 6,
                                                colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                                                modifier = Modifier.height(54.dp)
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Verify", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        // Helpers
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    authViewModel.sendSignupOtp(signupPhone.trim()) { ok ->
                                                        if (ok) Toast.makeText(context, "New SMS code dispatched!", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Resend SMS Code", fontSize = 11.sp)
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedButton(
                                            onClick = {
                                                isSendingPhoneOtp = true
                                                authViewModel.sendSignupOtp(signupPhone.trim()) { ok ->
                                                    isSendingPhoneOtp = false
                                                    if (ok) signupPhoneOtpSent = true
                                                }
                                            },
                                            enabled = !isSendingPhoneOtp && signupPhone.trim().length >= 10,
                                            shape = RoundedCornerShape(12.dp),
                                            modifier = Modifier.fillMaxWidth().height(48.dp)
                                        ) {
                                            if (isSendingPhoneOtp) {
                                                CircularProgressIndicator(modifier = Modifier.size(18.dp), color = accentColor, strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text("Dispatching SMS...", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            } else {
                                                Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Send Code via SMS", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Role selection for signup
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Register As:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                                Row {
                                    listOf("Customer", "Worker", "Admin").forEach { r ->
                                        FilterChip(
                                            selected = signupRole == r,
                                            onClick = { signupRole = r },
                                            label = { Text(r, fontSize = 11.sp) },
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = signupPassword,
                                onValueChange = { signupPassword = it },
                                label = { Text("Password (min 6 characters)") },
                                leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                trailingIcon = {
                                    IconButton(onClick = { signupPasswordVisible = !signupPasswordVisible }) {
                                        Icon(
                                            imageVector = if (signupPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                            contentDescription = null
                                        )
                                    }
                                },
                                visualTransformation = if (signupPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            OutlinedTextField(
                                value = signupConfirmPassword,
                                onValueChange = { signupConfirmPassword = it },
                                label = { Text("Confirm Password") },
                                leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                                visualTransformation = PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )

                            if (!isSignupEmailVerified && !isSignupPhoneVerified) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = Color(0xFFFEF3C7),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFFFDE68A)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Verification Required: Please verify your Email (via Gmail) or Phone Number (via SMS) before creating your account.",
                                            fontSize = 11.sp,
                                            color = Color(0xFFB45309),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }

                        // ==========================================
                        // 3. FORGOT PASSWORD / RESET PIN MODE
                        // ==========================================
                        AuthScreenMode.FORGOT_PASSWORD -> {
                            Text("Reset Password / PIN", fontSize = 16.sp, fontWeight = FontWeight.Black, color = textColor)
                            Text("Receive a secure single-use OTP to restore access.", fontSize = 11.sp, color = grayText)
                            Spacer(modifier = Modifier.height(14.dp))

                            OutlinedTextField(
                                value = resetTarget,
                                onValueChange = { resetTarget = it },
                                label = { Text("Registered Email or Phone") },
                                leadingIcon = { Icon(Icons.Default.LockReset, contentDescription = null) },
                                placeholder = { Text("Enter email or 10-digit phone") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                singleLine = true
                            )

                            if (resetOtpDispatched) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Surface(
                                    color = accentColor.copy(alpha = 0.08f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = BorderStroke(1.dp, accentColor.copy(alpha = 0.3f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(Icons.Default.LockReset, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                                            Text(
                                                "Single-Use Reset Code Dispatched",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor
                                            )
                                        }
                                        Text(
                                            "Enter the 6-digit code sent to $resetTarget",
                                            fontSize = 11.sp,
                                            color = grayText,
                                            modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                                        )

                                        val activeResetOtp = simulatedOtp ?: repository?.lastDispatchedOtp?.collectAsState()?.value?.code
                                        if (!activeResetOtp.isNullOrBlank()) {
                                            Surface(
                                                color = accentColor.copy(alpha = 0.12f),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, accentColor.copy(alpha = 0.45f)),
                                                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                                            ) {
                                                Row(
                                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Column(modifier = Modifier.weight(1f)) {
                                                        Text(
                                                            "Code: $activeResetOtp",
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Black,
                                                            color = accentColor,
                                                            letterSpacing = 1.sp
                                                        )
                                                        Text(
                                                            "Dispatched to $resetTarget",
                                                            fontSize = 9.5.sp,
                                                            color = grayText
                                                        )
                                                    }
                                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                        Button(
                                                            onClick = {
                                                                resetOtp = activeResetOtp
                                                                Toast.makeText(context, "Reset Code auto-filled!", Toast.LENGTH_SHORT).show()
                                                            },
                                                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                            modifier = Modifier.height(28.dp),
                                                            colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                                                        ) {
                                                            Text("⚡ Auto-fill", fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                        }
                                                        if (!resetTarget.contains("@")) {
                                                            OutlinedButton(
                                                                onClick = {
                                                                    com.example.util.VerificationNotificationHelper.openSmsAppWithPrefilledOtp(
                                                                        context = context,
                                                                        phone = resetTarget,
                                                                        otpCode = activeResetOtp,
                                                                        isRevolt = isRevolt
                                                                    )
                                                                },
                                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                                modifier = Modifier.height(28.dp)
                                                            ) {
                                                                Text("💬 SMS App", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        OutlinedTextField(
                                            value = resetOtp,
                                            onValueChange = { if (it.length <= 6) resetOtp = it.filter { ch -> ch.isDigit() } },
                                            placeholder = {
                                                Text("000000", letterSpacing = 6.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                            },
                                            textStyle = LocalTextStyle.current.copy(
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                letterSpacing = 6.sp,
                                                textAlign = TextAlign.Center,
                                                color = textColor
                                            ),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                            modifier = Modifier.fillMaxWidth().height(54.dp),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.End
                                        ) {
                                            TextButton(
                                                onClick = {
                                                    authViewModel.requestPasswordResetOtp(resetTarget) { success ->
                                                        if (success) Toast.makeText(context, "New reset code dispatched!", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Resend Code", fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = resetNewPassword,
                                    onValueChange = { resetNewPassword = it },
                                    label = { Text("New Password (min 6 characters)") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(10.dp))
                                OutlinedTextField(
                                    value = resetConfirmPassword,
                                    onValueChange = { resetConfirmPassword = it },
                                    label = { Text("Confirm New Password") },
                                    leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) },
                                    visualTransformation = PasswordVisualTransformation(),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    singleLine = true
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Primary Action Button
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            when (authMode) {
                                AuthScreenMode.SIGN_IN -> {
                                    if (selectedRole == "Customer") {
                                        if (!otpSent) {
                                            authViewModel.sendCustomerOtp(inputCustomerPhone) { success ->
                                                if (success) {
                                                    Toast.makeText(context, "Verification code sent to phone!", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            authViewModel.verifyCustomerOtpAndLogin(
                                                phone = inputCustomerPhone,
                                                name = inputCustomerName,
                                                otp = inputCustomerOtp
                                            ) { session ->
                                                scope.launch {
                                                    showSuccessDialog = true
                                                    delay(800)
                                                    showSuccessDialog = false
                                                    onLoginSuccess("Customer")
                                                }
                                            }
                                        }
                                    } else if (selectedRole == "Worker" && isWorkerOnboarding) {
                                        authViewModel.verifyWorkerInvitationAndRegister(
                                            email = inputOnboardingEmail,
                                            code = inputOnboardingCode,
                                            passwordPin = inputOnboardingPin
                                        ) { session ->
                                            scope.launch {
                                                showSuccessDialog = true
                                                delay(800)
                                                showSuccessDialog = false
                                                isWorkerOnboarding = false
                                                onLoginSuccess("Worker")
                                            }
                                        }
                                    } else {
                                        authViewModel.login(
                                            emailOrPhone = loginInputEmailOrPhone,
                                            credential = loginInputCredentialPassword,
                                            role = selectedRole
                                        ) { session ->
                                            scope.launch {
                                                showSuccessDialog = true
                                                delay(800)
                                                showSuccessDialog = false
                                                val resolvedRole = when {
                                                    session.role.equals("Admin", ignoreCase = true) || session.role.equals("SuperAdmin", ignoreCase = true) -> "Admin"
                                                    session.role.equals("Worker", ignoreCase = true) -> "Worker"
                                                    else -> "Customer"
                                                }
                                                onLoginSuccess(resolvedRole)
                                            }
                                        }
                                    }
                                }

                                AuthScreenMode.SIGN_UP -> {
                                    if (signupName.trim().isBlank()) {
                                        Toast.makeText(context, "Please enter your legal name", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (signupEmail.trim().isBlank() && signupPhone.trim().isBlank()) {
                                        Toast.makeText(context, "Please enter your email or phone number", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (signupPassword != signupConfirmPassword) {
                                        Toast.makeText(context, "Passwords do not match!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (signupPassword.length < 6) {
                                        Toast.makeText(context, "Password must be at least 6 characters!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    // Verification Gate: Either Email or Phone must be verified via external Gmail/SMS
                                    if (!isSignupEmailVerified && !isSignupPhoneVerified) {
                                        Toast.makeText(
                                            context,
                                            "Verification required! Please click 'Send Code to Gmail' or 'Send Code via SMS' and verify with your 6-digit code first.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        return@Button
                                    }

                                    authViewModel.signup(
                                        displayName = signupName.trim(),
                                        email = signupEmail.trim(),
                                        phone = signupPhone.trim(),
                                        password = signupPassword,
                                        role = signupRole
                                    ) { session ->
                                        scope.launch {
                                            showSuccessDialog = true
                                            delay(800)
                                            showSuccessDialog = false
                                            onLoginSuccess(session.role)
                                        }
                                    }
                                }

                                AuthScreenMode.FORGOT_PASSWORD -> {
                                    if (!resetOtpDispatched) {
                                        authViewModel.requestPasswordResetOtp(resetTarget) { success ->
                                            if (success) resetOtpDispatched = true
                                        }
                                    } else {
                                        if (resetNewPassword != resetConfirmPassword) {
                                            Toast.makeText(context, "New passwords do not match!", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        authViewModel.resetPasswordWithOtp(resetTarget, resetOtp, resetNewPassword) { success ->
                                            if (success) {
                                                authMode = AuthScreenMode.SIGN_IN
                                                resetOtpDispatched = false
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(45.dp)
                            .testTag("submit_auth_button")
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Brush.horizontalGradient(listOf(accentColor, accentColor.copy(alpha = 0.85f)))),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isViewModelLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp), strokeWidth = 2.2.dp)
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (authMode) {
                                            AuthScreenMode.SIGN_IN -> if (selectedRole == "Customer" && !otpSent) Icons.AutoMirrored.Filled.SendToMobile else Icons.AutoMirrored.Filled.Login
                                            AuthScreenMode.SIGN_UP -> Icons.Default.PersonAdd
                                            AuthScreenMode.FORGOT_PASSWORD -> if (!resetOtpDispatched) Icons.AutoMirrored.Filled.Send else Icons.Default.Key
                                        },
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = when (authMode) {
                                            AuthScreenMode.SIGN_IN -> when (selectedRole) {
                                                "Customer" -> if (!otpSent) "Request Verification Code" else "Verify & Enter Portal"
                                                "Worker" -> if (isWorkerOnboarding) "Activate Worker Console" else "Sign In to Worker Console"
                                                else -> "Sign In to Admin Console"
                                            }
                                            AuthScreenMode.SIGN_UP -> if (isSignupEmailVerified || isSignupPhoneVerified) "Complete Registration & Sign In" else "Verify Contact to Complete Registration"
                                            AuthScreenMode.FORGOT_PASSWORD -> if (!resetOtpDispatched) "Send Verification Code" else "Update Password & Sign In"
                                        },
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.5.sp,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

            Text(
                "RATTAN ELECTRICALS ENTERPRISE TRADERS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = grayText.copy(alpha = 0.85f),
                letterSpacing = 1.2.sp
            )
        }
    }

    if (showSuccessDialog) {
        AlertDialog(
            onDismissRequest = {},
            containerColor = cardColor,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier
                .border(
                    1.dp,
                    if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                    RoundedCornerShape(20.dp)
                )
                .width(300.dp),
            title = null,
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(
                                if (isDarkTheme) Color(0xFF14532D).copy(alpha = 0.35f)
                                else Color(0xFFDCFCE7)
                            )
                            .border(
                                1.5.dp,
                                Color(0xFF16A34A).copy(alpha = 0.5f),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        "Signed In Successfully",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (isRevolt) "Welcome back to ReVolt Enterprise" else "Welcome to Rattan Electricals",
                        fontSize = 12.5.sp,
                        color = grayText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                            )
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "$selectedRole Portal",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accentColor
                        )
                    }
                }
            },
            confirmButton = {}
        )
    }
}
