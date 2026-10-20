package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import com.example.data.ReLectRepository
import com.example.ui.AdminDashboardView
import com.example.ui.CustomerPublicPortal
import com.example.ui.WorkerDashboardView
import com.example.ui.LoginScreen
import com.example.ui.components.BrandLogoView
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.launch

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.viewmodels.AuthViewModel
import com.example.ui.viewmodels.AuthViewModelFactory
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    private val incomingDeepLink = MutableStateFlow<android.net.Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        intent?.data?.let { incomingDeepLink.value = it }
        setContent {
            val context = LocalContext.current
            val repository = remember { ReLectRepository.getInstance(context) }

            // Jetpack Auth ViewModel injection
            val authViewModel: AuthViewModel = viewModel(
                factory = AuthViewModelFactory(context)
            )
            val session by authViewModel.sessionState.collectAsState()

            // Core Navigation States
            val brandTheme by repository.activeBrandTheme.collectAsState()
            val savedCustomAccent by repository.customAccentColor.collectAsState()
            var showSplashScreen by rememberSaveable { mutableStateOf(true) } // Rich animated startup transition
            var activeRole by rememberSaveable { mutableStateOf("") } // Preserved across config changes
            var activeSection by rememberSaveable { mutableStateOf("Dashboard") } // ERP Tabs preserved

            // Handle incoming deep link / QR scan
            val currentDeepLink by incomingDeepLink.collectAsState()
            LaunchedEffect(currentDeepLink) {
                val uri = currentDeepLink
                if (uri != null) {
                    val q = uri.getQueryParameter("view")
                    val p = uri.path.orEmpty()
                    val s = uri.scheme.orEmpty()
                    val h = uri.host.orEmpty()
                    if (q.equals("customer", ignoreCase = true) ||
                        p.contains("customer", ignoreCase = true) ||
                        h.contains("customer", ignoreCase = true) ||
                        s.equals("relec", ignoreCase = true) ||
                        s.equals("revolt", ignoreCase = true)
                    ) {
                        showSplashScreen = false
                        activeRole = "Customer"
                    }
                }
            }
            var selectedAccentColor by remember(brandTheme, savedCustomAccent) {
                mutableStateOf<Color>(
                    if (savedCustomAccent != null) Color(savedCustomAccent!!.toInt())
                    else if (brandTheme == "revolt_crimson") Color(0xFFEF4444) else ElectricBlue
                )
            }
            var isDarkTheme by rememberSaveable { mutableStateOf(false) } // Default to premium light theme

            // Synchronize system status bar & navigation bar contrast dynamically with theme
            DisposableEffect(isDarkTheme, showSplashScreen) {
                val useDarkIcons = !isDarkTheme && !showSplashScreen
                val statusBarStyle = if (useDarkIcons) {
                    // Darker tinted scrim on top status bar for crystal clear time, battery & notification visibility
                    SystemBarStyle.light(
                        android.graphics.Color.argb(35, 15, 23, 42),
                        android.graphics.Color.argb(35, 15, 23, 42)
                    )
                } else {
                    // Deep dark top notice bar
                    SystemBarStyle.dark(android.graphics.Color.argb(220, 10, 15, 26))
                }
                val navigationBarStyle = if (useDarkIcons) {
                    SystemBarStyle.light(
                        android.graphics.Color.TRANSPARENT,
                        android.graphics.Color.TRANSPARENT
                    )
                } else {
                    SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                }
                enableEdgeToEdge(
                    statusBarStyle = statusBarStyle,
                    navigationBarStyle = navigationBarStyle
                )
                onDispose {}
            }

            // Auto-synchronize accent color when visual brand theme switches and no custom accent was manually pinned
            LaunchedEffect(brandTheme) {
                if (savedCustomAccent == null) {
                    selectedAccentColor = if (brandTheme == "revolt_crimson") Color(0xFFEF4444) else ElectricBlue
                }
            }

            // Synchronize UI activeRole with the secure session state
            LaunchedEffect(session) {
                if (session != null) {
                    val rawRole = session?.role?.trim()?.lowercase()
                    activeRole = when {
                        rawRole == "superadmin" || rawRole == "admin" -> "Admin"
                        rawRole == "worker" -> "Worker"
                        else -> "Customer" // SAFE DEFAULT: Any customer or unknown strictly defaults to Customer!
                    }
                } else {
                    activeRole = ""
                }
            }

            MyApplicationTheme(darkTheme = isDarkTheme, customAccentColor = selectedAccentColor) {
                val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    com.example.ui.theme.PremiumGlassBackdrop(isDarkTheme = isDarkTheme, accentColor = selectedAccentColor) {
                        AnimatedContent(
                            targetState = showSplashScreen,
                            transitionSpec = {
                                (scaleIn(initialScale = 0.95f, animationSpec = tween(450, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(400))) togetherWith
                                        (scaleOut(targetScale = 1.05f, animationSpec = tween(450, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(400)))
                            },
                            label = "splashTransition"
                        ) { isSplash ->
                            if (isSplash) {
                                SplashScreen(
                                    accentColor = selectedAccentColor,
                                    brandTheme = brandTheme,
                                    repository = repository,
                                    onEnterPortal = { showSplashScreen = false }
                                )
                            } else {
                                // Main App Routing based on Role
                            AnimatedContent(
                                targetState = activeRole,
                                transitionSpec = {
                                    (scaleIn(initialScale = 0.92f, animationSpec = tween(280, easing = FastOutSlowInEasing)) + fadeIn(animationSpec = tween(240))) togetherWith
                                            (scaleOut(targetScale = 0.94f, animationSpec = tween(240, easing = FastOutSlowInEasing)) + fadeOut(animationSpec = tween(200)))
                                },
                                label = "roleRoutingTransition"
                            ) { role ->
                                when (role) {
                                    "Admin" -> {
                                        val sessionRole = session?.role?.trim()?.lowercase()
                                        if (sessionRole == "customer") {
                                            // STRICT RBAC: Customers cannot view Admin, auto-redirect to Customer Public Portal
                                            CustomerPublicPortal(
                                                repository = repository,
                                                session = session,
                                                onNavigateBack = {
                                                    activeRole = ""
                                                    authViewModel.logout {}
                                                },
                                                accentColor = selectedAccentColor,
                                                isDarkTheme = isDarkTheme,
                                                onThemeToggle = { isDarkTheme = !isDarkTheme }
                                            )
                                        } else {
                                            AdminDashboardView(
                                                repository = repository,
                                                currentSection = activeSection,
                                                accentColor = selectedAccentColor,
                                                onRoleChange = { newRole ->
                                                    if (newRole.isEmpty()) {
                                                        activeRole = ""
                                                        authViewModel.logout {}
                                                    } else {
                                                        // Guard role changes by verified session authority
                                                        val authRole = session?.role?.trim()?.lowercase()
                                                        if (authRole == "superadmin" || authRole == "admin") {
                                                            activeRole = newRole
                                                        } else if (authRole == "worker" && newRole == "Worker") {
                                                            activeRole = "Worker"
                                                        } else {
                                                            activeRole = "Customer"
                                                        }
                                                    }
                                                },
                                                activeRole = activeRole,
                                                onSectionChange = { activeSection = it },
                                                isDarkTheme = isDarkTheme,
                                                onThemeToggle = { isDarkTheme = !isDarkTheme },
                                                onAccentColorChange = { color ->
                                                    selectedAccentColor = color
                                                    repository.setCustomAccentColor(color.toArgb().toLong())
                                                }
                                            )
                                        }
                                    }
                                    "Worker" -> {
                                        val sessionRole = session?.role?.trim()?.lowercase()
                                        if (sessionRole == "customer") {
                                            // STRICT RBAC: Customers cannot view Worker portal
                                            CustomerPublicPortal(
                                                repository = repository,
                                                session = session,
                                                onNavigateBack = {
                                                    activeRole = ""
                                                    authViewModel.logout {}
                                                },
                                                accentColor = selectedAccentColor,
                                                isDarkTheme = isDarkTheme,
                                                onThemeToggle = { isDarkTheme = !isDarkTheme }
                                            )
                                        } else {
                                            Scaffold(
                                                topBar = {
                                                    @OptIn(ExperimentalMaterial3Api::class)
                                                    CenterAlignedTopAppBar(
                                                        title = {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(
                                                                    "WORKER PORTAL",
                                                                    fontSize = 11.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = MaterialTheme.colorScheme.onSurface,
                                                                    letterSpacing = 2.sp
                                                                )
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Box(
                                                                    modifier = Modifier
                                                                        .size(4.dp)
                                                                        .clip(RoundedCornerShape(2.dp))
                                                                        .background(selectedAccentColor)
                                                                )
                                                            }
                                                        },
                                                        actions = {
                                                            // Theme toggle in worker portal
                                                            IconButton(onClick = { isDarkTheme = !isDarkTheme }) {
                                                                Icon(
                                                                    imageVector = if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.NightsStay,
                                                                    contentDescription = "Toggle Theme",
                                                                    tint = selectedAccentColor
                                                                )
                                                            }
                                                            IconButton(onClick = {
                                                                activeRole = ""
                                                                authViewModel.logout {}
                                                            }) {
                                                                Icon(
                                                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                                                    contentDescription = "Logout",
                                                                    tint = MaterialTheme.colorScheme.error
                                                                )
                                                            }
                                                        },
                                                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                                            containerColor = MaterialTheme.colorScheme.surface
                                                        )
                                                    )
                                                },
                                                containerColor = MaterialTheme.colorScheme.background
                                            ) { innerPadding ->
                                                Box(modifier = Modifier.padding(innerPadding)) {
                                                    WorkerDashboardView(
                                                        repository = repository,
                                                        accentColor = selectedAccentColor,
                                                        isDarkTheme = isDarkTheme
                                                    )
                                                }
                                            }
                                        }
                                    }
                                    "Customer" -> {
                                        CustomerPublicPortal(
                                            repository = repository,
                                            session = session,
                                            onNavigateBack = {
                                                activeRole = ""
                                                authViewModel.logout {}
                                            },
                                            accentColor = selectedAccentColor,
                                            isDarkTheme = isDarkTheme,
                                            onThemeToggle = { isDarkTheme = !isDarkTheme }
                                        )
                                    }
                                    else -> {
                                        LoginScreen(
                                            accentColor = selectedAccentColor,
                                            isDarkTheme = isDarkTheme,
                                            brandTheme = brandTheme,
                                            onBrandThemeChange = { repository.setBrandTheme(it) },
                                            onLoginSuccess = { selectedRole ->
                                                activeRole = selectedRole
                                            },
                                            onThemeToggle = { isDarkTheme = !isDarkTheme },
                                            authViewModel = authViewModel,
                                            repository = repository
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.data?.let { incomingDeepLink.value = it }
    }
}

@Composable
fun SplashScreen(
    accentColor: Color,
    brandTheme: String = "cyber_blue",
    repository: ReLectRepository? = null,
    onEnterPortal: () -> Unit
) {
    val isRevolt = brandTheme == "revolt_crimson"
    // Fast automatic transition timer
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        onEnterPortal()
    }

    // Entrance animation states
    val scaleAnim = remember { Animatable(0f) }
    val rotateAnim = remember { Animatable(-120f) }
    val alphaAnim = remember { Animatable(0f) }
    val textYOffset = remember { Animatable(50f) }

    LaunchedEffect(Unit) {
        // Run entrance animations simultaneously in parallel coroutines
        launch {
            scaleAnim.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            rotateAnim.animateTo(
                targetValue = 0f,
                animationSpec = tween(
                    durationMillis = 1200,
                    easing = CubicBezierEasing(0.34f, 1.56f, 0.64f, 1f) // Custom elastic EaseOutBack
                )
            )
        }
        launch {
            alphaAnim.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 900, easing = LinearOutSlowInEasing)
            )
        }
        launch {
            textYOffset.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    // Combine entry bounce scale with subtle continuous pulse
    val finalScale = scaleAnim.value * if (scaleAnim.value > 0.9f) pulseScale else 1f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onEnterPortal() }
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF020617), // Premium Cosmic Void
                        Color(0xFF0F172A),
                        Color(0xFF020617)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Animated glowing electricity shield container
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .graphicsLayer {
                        scaleX = finalScale
                        scaleY = finalScale
                        rotationZ = rotateAnim.value
                        alpha = alphaAnim.value
                    }
                    .clip(RoundedCornerShape(32.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(accentColor.copy(alpha = 0.15f), Color.Transparent),
                            radius = 300f
                        )
                    )
                    .border(
                        1.5.dp,
                        Brush.linearGradient(listOf(accentColor, Color(0xFF6366F1))),
                        RoundedCornerShape(32.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                BrandLogoView(
                    repository = repository,
                    isRevolt = isRevolt,
                    modifier = Modifier.size(96.dp).clip(RoundedCornerShape(26.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                if (isRevolt) "REVOLT ENTERPRISE" else "RATTAN ELECTRICALS",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = accentColor,
                letterSpacing = 4.sp,
                modifier = Modifier
                    .graphicsLayer {
                        translationY = textYOffset.value
                        alpha = alphaAnim.value
                    }
                    .padding(bottom = 4.dp)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.graphicsLayer {
                    translationY = textYOffset.value * 0.7f
                    alpha = alphaAnim.value
                }
            ) {
                Text(
                    if (isRevolt) "ReVolt" else "ReLec",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = (-1).sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(accentColor)
                )
            }

            Text(
                if (isRevolt) "Smart Business. Total Control. High-voltage operations, synchronized electrical billing, service board & inventory."
                else "Enterprise ERP suite with real-time billing, service board, customer records, live inventory, and multi-role operations.",
                fontSize = 12.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier
                    .graphicsLayer {
                        translationY = textYOffset.value * 0.5f
                        alpha = alphaAnim.value
                    }
                    .padding(horizontal = 32.dp)
                    .padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Premium Green "Let's Go" Action Button
            Button(
                onClick = onEnterPortal,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF22C55E),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp, pressedElevation = 2.dp),
                modifier = Modifier
                    .graphicsLayer {
                        alpha = alphaAnim.value
                        translationY = textYOffset.value * 0.3f
                    }
                    .height(52.dp)
                    .fillMaxWidth(0.68f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "LET'S GO",
                        fontWeight = FontWeight.Black,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            CircularProgressIndicator(
                color = Color(0xFF22C55E).copy(alpha = 0.6f),
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        alpha = alphaAnim.value
                    },
                strokeWidth = 2.dp
            )
        }
    }
}
