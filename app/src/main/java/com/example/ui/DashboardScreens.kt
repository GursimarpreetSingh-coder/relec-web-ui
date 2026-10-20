package com.example.ui

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.RepeatMode
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.automirrored.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.*
import com.example.R
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import coil.compose.AsyncImage
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import android.net.Uri
import com.example.ui.components.BrandLogoView
import androidx.compose.ui.graphics.asImageBitmap
import com.example.ui.components.CustomerQrDeskDialog
import com.example.ui.components.AccessControlSection
import com.example.ui.components.CUSTOMER_PORTAL_WEB_URL
import com.example.util.QrCodeGenerator

fun formatPhoneNumber(rawPhone: String): String {
    val digits = rawPhone.filter { it.isDigit() }
    return if (digits.length == 10) {
        "+91 ${digits.substring(0, 5)}-${digits.substring(5)}"
    } else if (digits.length > 10 && digits.startsWith("91")) {
        val last10 = digits.substring(digits.length - 10)
        "+91 ${last10.substring(0, 5)}-${last10.substring(5)}"
    } else {
        rawPhone
    }
}

fun cleanRawPhone(rawPhone: String): String {
    val digits = rawPhone.filter { it.isDigit() }
    return if (digits.length > 10 && digits.startsWith("91")) {
        digits.substring(digits.length - 10)
    } else {
        digits
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardView(
    repository: ReLectRepository,
    currentSection: String,
    accentColor: Color,
    onRoleChange: (String) -> Unit,
    activeRole: String,
    onSectionChange: (String) -> Unit,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onAccentColorChange: (Color) -> Unit
) {
    val context = LocalContext.current
    val brandTheme by repository.activeBrandTheme.collectAsState()
    val isRevolt = brandTheme == "revolt_crimson"
    var isMoreMenuOpen by remember { mutableStateOf(false) }
    val isSyncing by repository.isSyncing.collectAsState()

    var showScannerDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }

    if (showScannerDialog) {
        GlobalScannerDialog(repository = repository, accentColor = accentColor, onDismiss = { showScannerDialog = false })
    }
    if (showVoiceDialog) {
        AIVoiceEntryDialog(repository = repository, accentColor = accentColor, onDismiss = { showVoiceDialog = false })
    }

    // Custom back click handler to go back to main dashboard tab instead of exiting the app
    if (currentSection != "Dashboard") {
        androidx.activity.compose.BackHandler {
            onSectionChange("Dashboard")
        }
    }

    // Bottom Navigation destinations
    // 0: Dashboard, 1: Services (Jobs), 2: Customers, 3: Finances (Billing/Expenses), 4: More
    val selectedIndex = when (currentSection) {
        "Dashboard" -> 0
        "Services" -> 1
        "Customers" -> 2
        "Billing", "Expenses" -> 3
        else -> 4 // "Workers", "Inventory", "Reports", "Settings"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(end = 2.dp)
                    ) {
                        // Dynamic Brand Logo with smooth rounded corners matching reference design
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (isRevolt) Color.Transparent else Color.White)
                                .then(
                                    if (isRevolt) Modifier else Modifier.border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(9.dp))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            BrandLogoView(
                                repository = repository,
                                isRevolt = isRevolt,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = if (isRevolt) ContentScale.Fit else ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        // Two-line brand title and subtitle perfectly aligned and level with logo
                        Column(
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                Text(
                                    text = if (isRevolt) "RE-VOLT" else "ReLec",
                                    fontWeight = FontWeight.Black,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 15.5.sp,
                                    letterSpacing = (-0.3).sp,
                                    style = LocalTextStyle.current.copy(
                                        platformStyle = PlatformTextStyle(includeFontPadding = false),
                                        lineHeight = 17.sp
                                    ),
                                    maxLines = 1
                                )

                                // Pill Badge (PRO ADMIN)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(accentColor.copy(alpha = 0.12f))
                                        .border(0.8.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 4.5.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (activeRole.equals("admin", ignoreCase = true)) "PRO ADMIN" else activeRole.uppercase(),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 8.sp,
                                        color = accentColor,
                                        letterSpacing = 0.5.sp,
                                        style = LocalTextStyle.current.copy(
                                            platformStyle = PlatformTextStyle(includeFontPadding = false),
                                            lineHeight = 9.sp,
                                            textAlign = TextAlign.Center
                                        ),
                                        maxLines = 1
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(1.5.dp))

                            Text(
                                text = "Rattan Electricals",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                                letterSpacing = 0.1.sp,
                                style = LocalTextStyle.current.copy(
                                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                                    lineHeight = 12.sp
                                ),
                                maxLines = 1
                            )
                        }
                    }
                },
                actions = {
                    val actionBtnBg = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                    
                    // Global QR/Barcode Scanner
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(actionBtnBg)
                            .clickable { showScannerDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Scan QR/Barcode",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(5.dp))

                    // Global Speech Entry Desk
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(actionBtnBg)
                            .clickable { showVoiceDialog = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.RecordVoiceOver,
                            contentDescription = "Speech Command Desk",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(5.dp))

                    // Theme Toggle Button
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(actionBtnBg)
                            .clickable { onThemeToggle() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.NightsStay,
                            contentDescription = "Toggle Theme",
                            tint = accentColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(5.dp))

                    // Logout / Exit Button
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isDarkTheme) NeonRed.copy(alpha = 0.18f) else Color(0xFFFEE2E2))
                            .clickable { onRoleChange("") },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Log Out",
                            tint = NeonRed,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(6.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDarkTheme) Color(0xFF0F172A).copy(alpha = 0.95f) else Color.White
                ),
                modifier = Modifier.drawBehind {
                    drawLine(
                        color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                        start = Offset(0f, size.height),
                        end = Offset(size.width, size.height),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            )
        },
        bottomBar = {
            // Elegant bottom navigation
            val navItemColors = NavigationBarItemDefaults.colors(
                selectedIconColor = accentColor,
                selectedTextColor = accentColor,
                indicatorColor = accentColor.copy(alpha = if (isDarkTheme) 0.22f else 0.14f),
                unselectedIconColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B),
                unselectedTextColor = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)
            )

            NavigationBar(
                containerColor = if (isDarkTheme) Color(0xFF0F172A).copy(alpha = 0.92f) else Color.White.copy(alpha = 0.96f),
                tonalElevation = 6.dp,
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .border(
                        BorderStroke(
                            width = 1.dp,
                            color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0)
                        ),
                        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
                    )
            ) {
                // 1. Dashboard
                NavigationBarItem(
                    selected = selectedIndex == 0,
                    onClick = { onSectionChange("Dashboard") },
                    icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                    label = { Text("Dashboard", fontSize = 10.sp, fontWeight = if (selectedIndex == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = navItemColors
                )

                // 2. Jobs (Services)
                NavigationBarItem(
                    selected = selectedIndex == 1,
                    onClick = { onSectionChange("Services") },
                    icon = { Icon(Icons.Default.Handyman, contentDescription = "Jobs") },
                    label = { Text("Jobs", fontSize = 10.sp, fontWeight = if (selectedIndex == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = navItemColors
                )

                // 3. Customers
                NavigationBarItem(
                    selected = selectedIndex == 2,
                    onClick = { onSectionChange("Customers") },
                    icon = { Icon(Icons.Default.People, contentDescription = "Customers") },
                    label = { Text("Customers", fontSize = 10.sp, fontWeight = if (selectedIndex == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = navItemColors
                )

                // 4. Finances (Billing)
                NavigationBarItem(
                    selected = selectedIndex == 3,
                    onClick = { onSectionChange("Billing") },
                    icon = { Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = "Finances") },
                    label = { Text("Finances", fontSize = 10.sp, fontWeight = if (selectedIndex == 3) FontWeight.Bold else FontWeight.Normal) },
                    colors = navItemColors
                )

                // 5. More (Workers, Inventory, Reports, Settings)
                NavigationBarItem(
                    selected = selectedIndex == 4,
                    onClick = { isMoreMenuOpen = true },
                    icon = { Icon(Icons.Default.MoreHoriz, contentDescription = "More Menu") },
                    label = { Text("More", fontSize = 10.sp, fontWeight = if (selectedIndex == 4) FontWeight.Bold else FontWeight.Normal) },
                    colors = navItemColors
                )
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp)
        ) {
            when (currentSection) {
                "Dashboard" -> AdminDashboardScreen(repository, accentColor, onSectionChange, onRoleChange)
                "Customers" -> CustomersScreen(repository, accentColor)
                "Services" -> ServicesScreen(repository, accentColor)
                "Workers" -> WorkersScreen(repository, accentColor)
                "Inventory" -> InventoryScreen(repository, accentColor)
                "Billing" -> BillingScreen(repository, accentColor)
                "Expenses" -> ExpensesScreen(repository, accentColor)
                "Reports" -> ReportsScreen(repository, accentColor)
                "SupportDesk" -> SupportDeskManagementScreen(
                    repository = repository,
                    accentColor = accentColor,
                    onBack = { onSectionChange("Dashboard") }
                )
                "TeamApprovals" -> TeamApprovalsScreen(
                    repository = repository,
                    accentColor = accentColor,
                    onBack = { onSectionChange("Dashboard") }
                )
                "Settings" -> SettingsScreen(
                    repository = repository,
                    accentColor = accentColor,
                    onRoleChange = onRoleChange,
                    activeRole = activeRole,
                    isDarkTheme = isDarkTheme,
                    onThemeToggle = onThemeToggle,
                    onAccentColorChange = onAccentColorChange,
                    onNavigateToSection = onSectionChange
                )
            }
        }
    }

    // Modal Bottom Sheet for "More" menu items
    if (isMoreMenuOpen) {
        ModalBottomSheet(
            onDismissRequest = { isMoreMenuOpen = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "More Management Modules",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Grid of items: Workers, Inventory, Expenses, Reports, Support Desk, Team Approvals, Settings
                val gridItems = listOf(
                    Triple("Workers", Icons.Default.Groups, "Workers"),
                    Triple("Inventory", Icons.Default.Inventory, "Inventory"),
                    Triple("Expenses", Icons.Default.PriceChange, "Expenses"),
                    Triple("Reports", Icons.Default.Analytics, "Reports"),
                    Triple("Support Desk", Icons.Default.SupportAgent, "SupportDesk"),
                    Triple("Team Approvals", Icons.Default.AdminPanelSettings, "TeamApprovals"),
                    Triple("Settings", Icons.Default.Settings, "Settings")
                )

                gridItems.chunked(3).forEach { rowItems ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        rowItems.forEach { (label, icon, sec) ->
                            val isSpecial = sec == "TeamApprovals"
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSpecial) Color(0xFF8B5CF6).copy(alpha = 0.12f)
                                    else MaterialTheme.colorScheme.surfaceVariant
                                ),
                                border = if (isSpecial) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.4f)) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onSectionChange(sec)
                                        isMoreMenuOpen = false
                                    }
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        icon,
                                        contentDescription = label,
                                        tint = if (isSpecial) Color(0xFF8B5CF6) else accentColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSpecial) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                        // Fill remaining spaces in row if less than 3
                        val remaining = 3 - rowItems.size
                        for (i in 0 until remaining) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

// Sidebar Drawer layout
@Composable
fun SidebarContent(
    activeSection: String,
    onSectionChange: (String) -> Unit,
    accentColor: Color,
    activeRole: String,
    onRoleChange: (String) -> Unit,
    isSyncing: Boolean,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        "Dashboard" to Icons.Default.Dashboard,
        "Customers" to Icons.Default.People,
        "Services" to Icons.Default.Engineering,
        "Workers" to Icons.Default.Groups,
        "Inventory" to Icons.Default.Inventory,
        "Billing" to Icons.AutoMirrored.Filled.ReceiptLong,
        "Expenses" to Icons.Default.PriceChange,
        "Reports" to Icons.Default.Analytics,
        "Support Desk" to Icons.Default.SupportAgent,
        "Team Approvals" to Icons.Default.AdminPanelSettings,
        "Settings" to Icons.Default.Settings
    )

    Column(modifier = modifier.padding(12.dp)) {
        // App Logo branding header
        val isRevoltTheme = accentColor == Color(0xFFEF4444)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(if (isRevoltTheme) Color.Transparent else Color.White)
                    .then(
                        if (isRevoltTheme) Modifier else Modifier.border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(9.dp))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(
                        id = if (isRevoltTheme) R.drawable.img_revolt_red_logo_1791094396822 else R.drawable.img_relec_blue_logo_1791094376351
                    ),
                    contentDescription = "Brand Logo",
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(2.dp),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
                Text(
                    "RATTAN ELECTRICALS",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isRevoltTheme) "RE-VOLT" else "RELECT",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(accentColor)
                    )
                }
            }
        }

        HorizontalDivider(color = BorderSilver, modifier = Modifier.padding(bottom = 12.dp))

        // Navigation Items List
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(items) { (section, icon) ->
                val targetSec = when (section) {
                    "Support Desk" -> "SupportDesk"
                    "Team Approvals" -> "TeamApprovals"
                    else -> section
                }
                val isSelected = activeSection == section || activeSection == targetSec
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) accentColor.copy(alpha = 0.15f) else Color.Transparent)
                        .clickable { onSectionChange(targetSec) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = section,
                        tint = if (isSelected) accentColor else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        section,
                        color = if (isSelected) Color.White else Color.LightGray,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Bottom section: Secure Workspace Role Indicator
        HorizontalDivider(color = BorderSilver, modifier = Modifier.padding(vertical = 12.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(accentColor.copy(alpha = 0.12f))
                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.VerifiedUser, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("ENTERPRISE RBAC", fontSize = 9.sp, fontWeight = FontWeight.Black, color = accentColor, letterSpacing = 1.sp)
                Text("Role: ${activeRole.uppercase()}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Button(
            onClick = { onRoleChange("") },
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Secure Sign Out", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// Dynamic Rotation Helper for Sync Icon
@Composable
fun Modifier.rotateAnimation(): Modifier {
    val transition = rememberInfiniteTransition(label = "rotation")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing)),
        label = "angle"
    )
    return this.rotate(angle)
}

// --- MODULE 1: ADMIN DASHBOARD VIEW ---
@Composable
fun AdminDashboardScreen(
    repository: ReLectRepository,
    accentColor: Color,
    onSectionChange: (String) -> Unit,
    onRoleChange: (String) -> Unit = {}
) {
    val brandTheme by repository.activeBrandTheme.collectAsState()
    val isRevolt = brandTheme == "revolt_crimson"
    val accentColor = if (isRevolt) Color(0xFFEF4444) else ElectricBlue
    val customers by repository.customers.collectAsState()
    val services by repository.services.collectAsState()
    val inventory by repository.inventory.collectAsState()
    val bills by repository.bills.collectAsState()

    // Calculate metrics from actual database
    val totalCust = customers.size
    val pendingServ = services.filter { it.status != ServiceStatus.COMPLETED && it.status != ServiceStatus.CLOSED }.size
    val lowStockCount = inventory.filter { it.stockLevel <= it.minAlertLevel }.size
    val totalRevenue = bills.sumOf { it.totalAmount }

    // Dialog states for quick actions
    var showQuickAddJob by remember { mutableStateOf(false) }
    var showQuickAddCustomer by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showVoiceDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val isLight = MaterialTheme.colorScheme.surface == Color.White

    // Electric pulse animation for brand header
    val infiniteTransition = rememberInfiniteTransition(label = "electricPulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Premium Electric Header Banner
        item {
            val showBannerText by repository.showBannerText.collectAsState()
            val bannerRes = if (isRevolt) R.drawable.revolt_banner_rattan_official_1790190332784 else R.drawable.relec_banner_clean_1789665642270

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(135.dp)
                        .clip(RoundedCornerShape(20.dp)),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isRevolt) Color.White else Color(0xFF030D20)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            painter = painterResource(id = bannerRes),
                            contentDescription = if (isRevolt) "RE-VOLT Dashboard Banner" else "Dashboard Banner",
                            modifier = Modifier
                                .fillMaxSize()
                                .alpha(if (showBannerText) 0.78f else 1f),
                            contentScale = ContentScale.FillBounds,
                            alignment = Alignment.Center
                        )

                        // Scrim overlay fades the entire banner smoothly when text overlay is enabled
                        // When text overlay is OFF, show clear clean artwork banner
                        if (showBannerText) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.verticalGradient(
                                            listOf(
                                                if (isRevolt) Color(0xD9120205) else Color(0xD9030D20),
                                                if (isRevolt) Color(0xBF120205) else Color(0xBF030D20),
                                                if (isRevolt) Color(0xEE120205) else Color(0xEE030D20)
                                            )
                                        )
                                    )
                            )

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                // Top Row: Brand Header & Elegant Unboxed Green Online Status
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = if (isRevolt) "RE-VOLT ENTERPRISE" else "RELEC EDITION",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isRevolt) Color(0xFFFF4D4D) else accentColor,
                                            letterSpacing = 1.5.sp
                                        )
                                        Text(
                                            text = "Rattan Electricals",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black,
                                            color = Color.White
                                        )
                                    }

                                    // Elegant unboxed green Online indicator
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF22C55E))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Online",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF22C55E),
                                            letterSpacing = 0.5.sp
                                        )
                                    }
                                }

                                // Subtitle text (no boxes, purely clean typography)
                                Text(
                                    text = "Authorized Electrical Service & Operations Management",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color.White.copy(alpha = 0.7f),
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Quick Action Shortcut Buttons (New Job, Add Customer, Generate Invoice, Customer QR)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Quick Action 1: New Job
                Button(
                    onClick = { showQuickAddJob = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isRevolt) Color(0xFFEF4444).copy(alpha = 0.6f) else Color(0xFF60A5FA).copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("New Job", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Quick Action 2: Add Customer
                Button(
                    onClick = { showQuickAddCustomer = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLight) Color(0xFFF3E8FF) else Color(0xFF581C87).copy(alpha = 0.35f),
                        contentColor = if (isLight) Color(0xFF6B21A8) else Color(0xFFE9D5FF)
                    ),
                    border = BorderStroke(1.dp, if (isLight) Color(0xFFD8B4FE) else Color(0xFFA855F7).copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = if (isLight) Color(0xFF9333EA) else Color(0xFFC084FC), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Customer", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Quick Action 3: Generate Invoice
                Button(
                    onClick = { onSectionChange("Billing") },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLight) Color(0xFFECFDF5) else Color(0xFF064E3B).copy(alpha = 0.35f),
                        contentColor = if (isLight) Color(0xFF065F46) else Color(0xFFA7F3D0)
                    ),
                    border = BorderStroke(1.dp, if (isLight) Color(0xFFA7F3D0) else Color(0xFF10B981).copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = if (isLight) Color(0xFF059669) else Color(0xFF34D399), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Generate Invoice", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                // Quick Action 4: Customer QR
                Button(
                    onClick = { showQrDialog = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLight) Color(0xFFFFFBEB) else Color(0xFF78350F).copy(alpha = 0.35f),
                        contentColor = if (isLight) Color(0xFF92400E) else Color(0xFFFDE68A)
                    ),
                    border = BorderStroke(1.dp, if (isLight) Color(0xFFFDE68A) else Color(0xFFF59E0B).copy(alpha = 0.6f)),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = if (isLight) Color(0xFFD97706) else Color(0xFFFBBF24), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Customer QR", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }

        // 3. Analytics 4 Stats Cards Grid (2x2) in Glass-Morphism Style
        item {
            val stats = listOf(
                QuadStat(
                    title = "Total Customers",
                    value = "$totalCust",
                    changeText = if (totalCust > 0) "$totalCust Registered" else "Tap to Add",
                    isPositiveChange = totalCust > 0,
                    icon = Icons.Default.Person,
                    iconTint = Color(0xFF2563EB),
                    targetSection = "Customers",
                    isLowStock = false,
                    isNeutral = totalCust == 0,
                    badgeIcon = if (totalCust == 0) Icons.Default.Add else Icons.AutoMirrored.Filled.TrendingUp
                ),
                QuadStat(
                    title = "Monthly Revenue",
                    value = "₹${String.format("%,.0f", totalRevenue)}",
                    changeText = if (totalRevenue > 0) "Total Invoiced" else "Ready to Bill",
                    isPositiveChange = totalRevenue > 0,
                    icon = Icons.Default.Payments,
                    iconTint = Color(0xFF10B981),
                    targetSection = "Billing",
                    isLowStock = false,
                    isNeutral = totalRevenue == 0.0,
                    badgeIcon = if (totalRevenue == 0.0) Icons.Default.ReceiptLong else Icons.AutoMirrored.Filled.TrendingUp
                ),
                QuadStat(
                    title = "Pending Jobs",
                    value = "$pendingServ",
                    changeText = if (pendingServ > 0) "$pendingServ In Progress" else "Queue All Clear",
                    isPositiveChange = pendingServ == 0,
                    icon = Icons.Default.Schedule,
                    iconTint = Color(0xFFF59E0B),
                    targetSection = "Services",
                    isLowStock = false,
                    isNeutral = false,
                    badgeIcon = if (pendingServ == 0) Icons.Default.CheckCircle else Icons.Default.FlashOn
                ),
                QuadStat(
                    title = "Low Stock Alert",
                    value = "$lowStockCount",
                    changeText = if (lowStockCount > 0) "$lowStockCount Low Stock" else "Stock Optimal",
                    isPositiveChange = lowStockCount == 0,
                    icon = Icons.Default.Inventory2,
                    iconTint = if (lowStockCount > 0) Color(0xFFEF4444) else Color(0xFF8B5CF6),
                    targetSection = "Inventory",
                    isLowStock = lowStockCount > 0,
                    isNeutral = false,
                    badgeIcon = if (lowStockCount > 0) Icons.Default.WarningAmber else Icons.Default.CheckCircle
                )
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Section Title with Live indicator
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "BUSINESS METRICS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = "Real-time",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isLight) Color(0xFF059669) else Color(0xFF34D399)
                        )
                    }
                }

                // Top Row (2 cards)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        DashboardStatCard(stat = stats[0], accentColor = accentColor, onClick = { onSectionChange(stats[0].targetSection) })
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        DashboardStatCard(stat = stats[1], accentColor = accentColor, onClick = { onSectionChange(stats[1].targetSection) })
                    }
                }
                // Bottom Row (2 cards)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        DashboardStatCard(stat = stats[2], accentColor = accentColor, onClick = { onSectionChange(stats[2].targetSection) })
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        DashboardStatCard(stat = stats[3], accentColor = accentColor, onClick = { onSectionChange(stats[3].targetSection) })
                    }
                }
            }
        }

        // 4. Interactive Area Revenue Chart Card
        item {
            val chartBgGradient = if (isLight) {
                Brush.linearGradient(
                    listOf(
                        Color(0xFFF0F7FF),
                        Color(0xFFF8FAFC)
                    )
                )
            } else {
                Brush.linearGradient(
                    listOf(
                        Color(0xFF0B192C),
                        Color(0xFF0F172A)
                    )
                )
            }
            val chartBorder = if (isLight) Color(0xFFBFDBFE) else Color(0xFF1E3A8A).copy(alpha = 0.6f)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, chartBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(chartBgGradient)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Compute dynamic 7-day revenue progression purely from Room Database
                        val sdf = remember { SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US) }
                        val last7Days = remember {
                            (0..6).map { offset ->
                                val tempCal = java.util.Calendar.getInstance()
                                tempCal.add(java.util.Calendar.DAY_OF_YEAR, -6 + offset)
                                sdf.format(tempCal.time)
                            }
                        }
                        val points = remember(bills) {
                            val dailyRevenue = mutableListOf<Float>()
                            last7Days.forEach { dateStr ->
                                val billsTotal = bills.filter { it.date == dateStr }.sumOf { it.totalAmount }.toFloat()
                                dailyRevenue.add(billsTotal)
                            }
                            dailyRevenue
                        }
                        val weekRevenueTotal = points.sum()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Revenue Progression (This Week)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                                Text("Real-time aggregated sales and services billing logs", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (weekRevenueTotal > 0) NeonGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.AutoMirrored.Filled.TrendingUp, contentDescription = null, tint = if (weekRevenueTotal > 0) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (weekRevenueTotal > 0) "₹${String.format("%,.0f", weekRevenueTotal)}" else "₹0.00",
                                        color = if (weekRevenueTotal > 0) NeonGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Resolve theme-aware colors for Canvas
                        val gridColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        val nodeBgColor = MaterialTheme.colorScheme.surface
                        val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

                        val sdfDayName = remember { SimpleDateFormat("EEE", java.util.Locale.US) }
                        val sdfDateLabel = remember { SimpleDateFormat("dd MMM", java.util.Locale.US) }
                        val last7DaysNames = remember {
                            (0..6).map { offset ->
                                val tempCal = java.util.Calendar.getInstance()
                                tempCal.add(java.util.Calendar.DAY_OF_YEAR, -6 + offset)
                                sdfDayName.format(tempCal.time)
                            }
                        }
                        val last7DaysDates = remember {
                            (0..6).map { offset ->
                                val tempCal = java.util.Calendar.getInstance()
                                tempCal.add(java.util.Calendar.DAY_OF_YEAR, -6 + offset)
                                sdfDateLabel.format(tempCal.time)
                            }
                        }

                        var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

                        // Draw Revenue line chart on Canvas
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(115.dp)
                                .pointerInput(points) {
                                    detectTapGestures { offset ->
                                        val width = size.width
                                        val insetX = 14f * 2.5f
                                        val plotWidth = (width - insetX * 2f).coerceAtLeast(1f)
                                        val stepX = plotWidth / (points.size - 1).coerceAtLeast(1)
                                        var nearestIdx = 0
                                        var minDiff = Float.MAX_VALUE
                                        for (i in points.indices) {
                                            val x = insetX + i * stepX
                                            val diff = kotlin.math.abs(offset.x - x)
                                            if (diff < minDiff) {
                                                minDiff = diff
                                                nearestIdx = i
                                            }
                                        }
                                        selectedPointIndex = nearestIdx
                                    }
                                }
                        ) {
                            val rawMax = points.maxOrNull() ?: 0f
                            val maxPoint = if (rawMax > 0f) rawMax else 1000f
                            val width = size.width
                            val height = size.height
                            val insetX = 14.dp.toPx()
                            val insetY = 12.dp.toPx()
                            val plotWidth = (width - insetX * 2f).coerceAtLeast(1f)
                            val plotHeight = (height - insetY * 2f).coerceAtLeast(1f)
                            val stepX = plotWidth / (points.size - 1).coerceAtLeast(1)

                            // Draw Grid Lines
                            val lines = 4
                            for (i in 0..lines) {
                                val y = insetY + (plotHeight * i / lines)
                                drawLine(
                                    color = gridColor,
                                    start = Offset(insetX, y),
                                    end = Offset(width - insetX, y),
                                    strokeWidth = 1.dp.toPx()
                                )
                            }

                            // Draw selected cursor line
                            selectedPointIndex?.let { idx ->
                                val x = insetX + idx * stepX
                                drawLine(
                                    color = accentColor.copy(alpha = 0.5f),
                                    start = Offset(x, insetY),
                                    end = Offset(x, height - insetY),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                )
                            }

                            // Draw Line Path
                            val path = androidx.compose.ui.graphics.Path()
                            points.forEachIndexed { idx, value ->
                                val x = insetX + idx * stepX
                                val y = insetY + (plotHeight - (value / maxPoint * plotHeight))
                                if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
                            }

                            drawPath(
                                path = path,
                                color = accentColor,
                                style = Stroke(width = 3.dp.toPx())
                            )

                            // Fill under the curve
                            val fillPath = androidx.compose.ui.graphics.Path().apply {
                                addPath(path)
                                lineTo(insetX + (points.size - 1) * stepX, height - insetY)
                                lineTo(insetX, height - insetY)
                                close()
                            }
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    listOf(accentColor.copy(alpha = 0.35f), Color.Transparent)
                                )
                            )

                            // Draw Point circular nodes - completely within canvas bounds
                            points.forEachIndexed { idx, value ->
                                val x = insetX + idx * stepX
                                val y = insetY + (plotHeight - (value / maxPoint * plotHeight))
                                val isSelected = idx == selectedPointIndex
                                drawCircle(
                                    color = nodeBgColor,
                                    radius = if (isSelected) 7.dp.toPx() else 5.dp.toPx(),
                                    center = Offset(x, y)
                                )
                                drawCircle(
                                    color = if (isSelected) NeonGreen else accentColor,
                                    radius = if (isSelected) 4.5.dp.toPx() else 3.dp.toPx(),
                                    center = Offset(x, y)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            last7DaysNames.forEach { day ->
                                Text(day, color = labelColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        // Interactive details card showing X and Y values clearly on click
                        AnimatedVisibility(visible = selectedPointIndex != null) {
                            selectedPointIndex?.let { idx ->
                                val value = points[idx]
                                val dayLabel = last7DaysNames[idx]
                                val dateLabel = last7DaysDates[idx]
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.1f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                        .clickable { selectedPointIndex = null }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text("DATE & DAY", fontSize = 10.sp, color = labelColor, fontWeight = FontWeight.Bold)
                                            Text("$dayLabel, $dateLabel", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("DAILY REVENUE", fontSize = 10.sp, color = labelColor, fontWeight = FontWeight.Bold)
                                            Text("₹%,.2f".format(value), fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = NeonGreen)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. Recent Activity Stream
        item {
            val activityBgGradient = if (isLight) {
                Brush.linearGradient(
                    listOf(
                        Color(0xFFFAF5FF),
                        Color(0xFFFFFFFF)
                    )
                )
            } else {
                Brush.linearGradient(
                    listOf(
                        Color(0xFF131127),
                        Color(0xFF0F172A)
                    )
                )
            }
            val activityBorder = if (isLight) Color(0xFFE9D5FF) else Color(0xFF6366F1).copy(alpha = 0.35f)

            Card(
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, activityBorder, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(activityBgGradient)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Live Service & Sales Activity", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp)
                            TextButton(onClick = { onSectionChange("Services") }) {
                                Text("View All Jobs", fontSize = 11.sp, color = accentColor, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Real activities aggregated dynamically from Room Database (services & bills)
                        val realActivities = remember(services, bills) {
                            val list = mutableListOf<Triple<String, String, Color>>()
                            // Add real service jobs
                            services.take(3).forEach { req ->
                                val statusColor = when (req.status) {
                                    ServiceStatus.COMPLETED -> NeonGreen
                                    ServiceStatus.IN_PROGRESS -> accentColor
                                    else -> Color(0xFFF59E0B)
                                }
                                val workerInfo = req.assignedWorkerName?.let { " • Assigned to $it" } ?: ""
                                val statusText = req.status.name.replace("_", " ")
                                list.add(
                                    Triple(
                                        "${req.customerName} (${req.customerAddress})",
                                        "${req.problemDescription} [$statusText]$workerInfo",
                                        statusColor
                                    )
                                )
                            }
                            // Add real invoices/bills
                            bills.take(2).forEach { bill ->
                                val invNum = bill.invoiceNumber ?: "INV"
                                val mode = bill.paymentMethod ?: "Cash"
                                list.add(
                                    Triple(
                                        "Invoice #$invNum — ${bill.customerName}",
                                        "₹${String.format("%,.0f", bill.totalAmount)} • ${bill.date} • Paid via $mode",
                                        NeonGreen
                                    )
                                )
                            }
                            list
                        }

                        if (realActivities.isEmpty()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.HourglassEmpty,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.size(36.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No Live Activity Logged Yet",
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Log service requests or generate invoices to see live enterprise activity here.",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            realActivities.forEachIndexed { index, (title, desc, dotColor) ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(dotColor)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                        Text(desc, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                                    }
                                }
                                if (index < realActivities.size - 1) {
                                    HorizontalDivider(color = BorderSilver, modifier = Modifier.padding(vertical = 4.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Quick Add Job Dialog
    if (showQuickAddJob) {
        var cName by remember { mutableStateOf("") }
        var cPhone by remember { mutableStateOf("") }
        var cAddress by remember { mutableStateOf("") }
        var cProblem by remember { mutableStateOf("") }
        var cCategory by remember { mutableStateOf("DB Board Repair") }
        var cPriority by remember { mutableStateOf(Priority.HIGH) }

        AlertDialog(
            onDismissRequest = { showQuickAddJob = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Bolt, contentDescription = null, tint = accentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Create New Service Job", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = cName,
                        onValueChange = { cName = it },
                        label = { Text("Customer Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cPhone,
                        onValueChange = { cPhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cAddress,
                        onValueChange = { cAddress = it },
                        label = { Text("Address / Location") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = cProblem,
                        onValueChange = { cProblem = it },
                        label = { Text("Problem Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (cName.isNotBlank() && cPhone.isNotBlank()) {
                            val newReq = ServiceRequest(
                                customerName = cName.trim(),
                                customerPhone = cPhone.trim(),
                                customerAddress = if (cAddress.isNotBlank()) cAddress.trim() else "Mohali / Chandigarh",
                                problemDescription = if (cProblem.isNotBlank()) cProblem.trim() else "Electrical check required",
                                category = cCategory,
                                priority = cPriority
                            )
                            repository.addServiceRequest(newReq)
                            showQuickAddJob = false
                            Toast.makeText(context, "New job for $cName logged successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please enter customer name and phone", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Job Request", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickAddJob = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Quick Add Customer Dialog
    if (showQuickAddCustomer) {
        var custName by remember { mutableStateOf("") }
        var custPhone by remember { mutableStateOf("") }
        var custAddress by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showQuickAddCustomer = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = accentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add New Customer Record", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = custName,
                        onValueChange = { custName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = custPhone,
                        onValueChange = { custPhone = it },
                        label = { Text("Contact Phone") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = custAddress,
                        onValueChange = { custAddress = it },
                        label = { Text("Street / Sector Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (custName.isNotBlank() && custPhone.isNotBlank()) {
                            val newCust = Customer(
                                name = custName.trim(),
                                phone = custPhone.trim(),
                                address = if (custAddress.isNotBlank()) custAddress.trim() else "Chandigarh / Mohali"
                            )
                            repository.addCustomer(newCust)
                            showQuickAddCustomer = false
                            Toast.makeText(context, "Customer $custName added!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please fill name and phone", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Add Customer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickAddCustomer = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // QR Booking Hub Dialog
    if (showQrDialog) {
        val dynamicAppQrUrl by repository.customAppQrUrl.collectAsState()
        CustomerQrDeskDialog(
            onDismiss = { showQrDialog = false },
            onOpenCustomerPortal = { onRoleChange("Customer") },
            accentColor = accentColor,
            isDarkTheme = MaterialTheme.colorScheme.surface != Color.White,
            customUrl = dynamicAppQrUrl,
            onUpdateUrl = { newUrl ->
                repository.updateCustomAppQrUrl(newUrl)
            }
        )
    }
}

data class QuadStat(
    val title: String,
    val value: String,
    val changeText: String,
    val isPositiveChange: Boolean,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val iconTint: Color,
    val targetSection: String,
    val isLowStock: Boolean = false,
    val isNeutral: Boolean = false,
    val badgeIcon: androidx.compose.ui.graphics.vector.ImageVector? = null
)

@Composable
fun DashboardStatCard(
    stat: QuadStat,
    accentColor: Color,
    onClick: () -> Unit
) {
    val isLight = MaterialTheme.colorScheme.surface == Color.White
    var isPressed by remember { mutableStateOf(false) }
    val cardScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1.0f,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "statCardScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "pulseTransition")
    val warningDotAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "warningDotAlpha"
    )

    val surfaceBg = if (isLight) Color.White else Color(0xFF131B2E)
    val cardBorder = if (stat.isLowStock) {
        if (isLight) Color(0xFFFCA5A5) else Color(0xFF7F1D1D)
    } else {
        stat.iconTint.copy(alpha = if (isLight) 0.25f else 0.35f)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = surfaceBg),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isLight) 2.dp else 4.dp,
            pressedElevation = 1.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .border(1.dp, cardBorder, RoundedCornerShape(18.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            stat.iconTint.copy(alpha = if (isLight) 0.08f else 0.16f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // TOP ROW: Title on left, Icon Badge on right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stat.title,
                        color = if (isLight) Color(0xFF475569) else Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 6.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        stat.iconTint.copy(alpha = if (isLight) 0.18f else 0.28f),
                                        stat.iconTint.copy(alpha = if (isLight) 0.08f else 0.14f)
                                    )
                                )
                            )
                            .border(1.dp, stat.iconTint.copy(alpha = if (isLight) 0.35f else 0.50f), RoundedCornerShape(11.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = stat.icon,
                            contentDescription = stat.title,
                            tint = stat.iconTint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Stat Value (Big, high-contrast, extra bold)
                Text(
                    text = stat.value,
                    fontWeight = FontWeight.Black,
                    color = if (isLight) Color(0xFF0F172A) else Color.White,
                    fontSize = 24.sp,
                    letterSpacing = (-0.5).sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                // BOTTOM ROW: Clean pill badge + subtle nav chevron
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (stat.isLowStock) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFEF4444).copy(alpha = if (isLight) 0.12f else 0.20f))
                                .border(0.8.dp, Color(0xFFEF4444).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 7.dp, vertical = 3.5.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEF4444).copy(alpha = warningDotAlpha))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "Reorder Needed",
                                color = if (isLight) Color(0xFFDC2626) else Color(0xFFF87171),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    } else if (stat.isNeutral) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(stat.iconTint.copy(alpha = if (isLight) 0.10f else 0.18f))
                                .border(0.8.dp, stat.iconTint.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 7.dp, vertical = 3.5.dp)
                        ) {
                            Icon(
                                imageVector = stat.badgeIcon ?: Icons.Default.Info,
                                contentDescription = null,
                                tint = stat.iconTint,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stat.changeText,
                                color = stat.iconTint,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        val changeBgColor = stat.iconTint.copy(alpha = if (isLight) 0.10f else 0.18f)
                        val changeTextColor = stat.iconTint

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(changeBgColor)
                                .border(0.8.dp, changeTextColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 7.dp, vertical = 3.5.dp)
                        ) {
                            Icon(
                                imageVector = stat.badgeIcon ?: (if (stat.isPositiveChange) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown),
                                contentDescription = null,
                                tint = changeTextColor,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stat.changeText,
                                color = changeTextColor,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Subtle interactive chevron
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = stat.iconTint.copy(alpha = 0.55f),
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CustomerGlassCard(
    cust: Customer,
    customerJobs: List<ServiceRequest>,
    completedJobs: Int,
    activeJobs: Int,
    initials: String,
    accentColor: Color,
    isLight: Boolean,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val cardScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "custCardScale"
    )

    val surfaceBg = if (isLight) Color.White else Color(0xFF131B2E)
    val borderColor = if (isLight) Color(0xFFE2E8F0) else Color(0xFF334155).copy(alpha = 0.7f)

    Card(
        colors = CardDefaults.cardColors(containerColor = surfaceBg),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isLight) 1.5.dp else 3.dp,
            pressedElevation = 0.5.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Top Row: Avatar Initials + Name + Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    // Crisp Avatar Badge
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                            .border(1.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = initials,
                            color = Color.White,
                            fontWeight = FontWeight.Black,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = cust.name,
                            fontWeight = FontWeight.Bold,
                            color = if (isLight) Color(0xFF0F172A) else Color.White,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = accentColor, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatPhoneNumber(cust.phone),
                                fontSize = 12.sp,
                                color = if (isLight) Color(0xFF64748B) else Color(0xFFCBD5E1),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Professional Action Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Sleek Direct Call Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isLight) Color(0xFFDCFCE7) else Color(0xFF064E3B).copy(alpha = 0.65f)
                            )
                            .border(
                                1.dp,
                                if (isLight) Color(0xFF86EFAC) else Color(0xFF10B981).copy(alpha = 0.5f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(onClick = onCallClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Call Customer",
                            tint = if (isLight) Color(0xFF15803D) else Color(0xFF4ADE80),
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    // Sleek Direct WhatsApp Button
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (isLight) Color(0xFFF0FDF4) else Color(0xFF022C22).copy(alpha = 0.65f)
                            )
                            .border(
                                1.dp,
                                Color(0xFF25D366).copy(alpha = 0.45f),
                                RoundedCornerShape(8.dp)
                            )
                            .clickable(onClick = onWhatsAppClick),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_whatsapp),
                            contentDescription = "WhatsApp Customer",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    // More Actions Menu (Edit & Delete)
                    var showCustomerMenu by remember { mutableStateOf(false) }
                    Box {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isLight) Color(0xFFF1F5F9) else Color(0xFF1E293B)
                                )
                                .border(
                                    1.dp,
                                    if (isLight) Color(0xFFCBD5E1) else Color(0xFF334155),
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { showCustomerMenu = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Customer Actions",
                                tint = if (isLight) Color(0xFF475569) else Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showCustomerMenu,
                            onDismissRequest = { showCustomerMenu = false },
                            modifier = Modifier
                                .background(if (isLight) Color.White else Color(0xFF0F172A))
                                .border(1.dp, if (isLight) Color(0xFFE2E8F0) else Color(0xFF334155), RoundedCornerShape(12.dp))
                        ) {
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = if (isLight) Color(0xFF475569) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                text = {
                                    Text(
                                        "Edit Details",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = if (isLight) Color(0xFF0F172A) else Color.White
                                    )
                                },
                                onClick = {
                                    showCustomerMenu = false
                                    onEditClick()
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = null,
                                        tint = NeonRed,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                text = {
                                    Text(
                                        "Delete Client",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = NeonRed
                                    )
                                },
                                onClick = {
                                    showCustomerMenu = false
                                    onDeleteClick()
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Address Row
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, contentDescription = null, tint = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8), modifier = Modifier.size(13.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = cust.address.ifBlank { "Chandigarh / Mohali" },
                    fontSize = 12.sp,
                    color = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = if (isLight) Color(0xFFF1F5F9) else Color(0xFF1E293B).copy(alpha = 0.8f))
            Spacer(modifier = Modifier.height(8.dp))

            // Bottom Badges: Jobs Count & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Jobs Count Chip
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isLight) Color(0xFFF1F5F9) else Color(0xFF1E293B))
                        .border(0.8.dp, if (isLight) Color(0xFFCBD5E1) else Color(0xFF334155), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Work, contentDescription = null, tint = accentColor, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${customerJobs.size} Jobs Logged",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isLight) Color(0xFF334155) else Color(0xFFCBD5E1)
                        )
                    }
                }

                if (activeJobs > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(NeonYellow.copy(alpha = if (isLight) 0.15f else 0.20f))
                            .border(0.8.dp, NeonYellow.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(NeonYellow))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$activeJobs Active",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLight) Color(0xFFB45309) else NeonYellow
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF10B981).copy(alpha = if (isLight) 0.12f else 0.18f))
                            .border(0.8.dp, Color(0xFF10B981).copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = if (isLight) Color(0xFF059669) else Color(0xFF34D399), modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "All Clear",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLight) Color(0xFF059669) else Color(0xFF34D399)
                            )
                        }
                    }
                }
            }
        }
    }
}

// --- MODULE 2: CUSTOMERS SCREEN ---
@Composable
fun CustomersScreen(repository: ReLectRepository, accentColor: Color) {
    val customers by repository.customers.collectAsState()
    val services by repository.services.collectAsState()
    var search by remember { mutableStateOf("") }
    var selectedFilterTab by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }
    var customerToEdit by remember { mutableStateOf<Customer?>(null) }
    var customerToDelete by remember { mutableStateOf<Customer?>(null) }

    val isLight = MaterialTheme.colorScheme.surface == Color.White
    val textColor = if (isLight) Color(0xFF0F172A) else Color.White
    val grayText = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8)
    val lightGrayText = if (isLight) Color(0xFF334155) else Color(0xFFCBD5E1)
    val cardBg = if (isLight) Color.White else Color(0xFF0F172A)

    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newAddress by remember { mutableStateOf("") }

    // Dialer & WhatsApp confirmation states
    var phoneToDial by remember { mutableStateOf<String?>(null) }
    var dialConfirmCustomerName by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    val filtered = remember(customers, search, selectedFilterTab, services) {
        val base = customers.filter {
            it.name.contains(search, true) || it.phone.contains(search) || it.address.contains(search, true)
        }
        when (selectedFilterTab) {
            "Active" -> base.filter { c -> services.any { it.customerPhone == c.phone && it.status != ServiceStatus.COMPLETED } }
            "Recent" -> base.takeLast(10).reversed()
            "Most Jobs" -> base.sortedByDescending { c -> services.count { it.customerPhone == c.phone } }
            else -> base
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Header with Badge & Add Customer Button (Cleanly Aligned)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Customers Master",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        letterSpacing = (-0.3).sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.12f))
                            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${customers.size} Total",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }
                Text(
                    "Directory of verified client accounts and contact profiles",
                    fontSize = 12.sp,
                    color = grayText,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                shape = RoundedCornerShape(10.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }

        // Quick Stats Summary Boxes - Cleanly Aligned with equal heights & identical padding
        val activeClientsCount = customers.count { c -> services.any { it.customerPhone == c.phone && it.status != ServiceStatus.COMPLETED } }
        val clearClientsCount = (customers.size - activeClientsCount).coerceAtLeast(0)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                Triple("REGISTERED", "${customers.size}", accentColor),
                Triple("ACTIVE JOBS", "$activeClientsCount", if (isLight) Color(0xFFD97706) else NeonYellow),
                Triple("ALL CLEAR", "$clearClientsCount", if (isLight) Color(0xFF16A34A) else NeonGreen)
            ).forEach { (label, count, highlightColor) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isLight) Color(0xFFF8FAFC) else Color(0xFF1E293B))
                        .border(1.dp, if (isLight) Color(0xFFE2E8F0) else Color(0xFF334155), RoundedCornerShape(12.dp))
                        .padding(horizontal = 6.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = highlightColor,
                            maxLines = 1,
                            letterSpacing = 0.3.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = count,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = textColor
                        )
                    }
                }
            }
        }

        // 2. Search Bar with Live Clear Icon
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Search by customer name, phone number, address...", fontSize = 12.sp, color = grayText) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = accentColor) },
            trailingIcon = {
                if (search.isNotEmpty()) {
                    IconButton(onClick = { search = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = grayText, modifier = Modifier.size(18.dp))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedBorderColor = accentColor,
                unfocusedBorderColor = if (isLight) Color(0xFFE2E8F0) else Color(0xFF334155),
                focusedContainerColor = if (isLight) Color(0xFFF8FAFC) else Color(0xFF1E293B),
                unfocusedContainerColor = if (isLight) Color(0xFFF8FAFC) else Color(0xFF1E293B)
            )
        )

        // 3. Filter Tabs (All | Active | Recent | Most Jobs)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val tabs = listOf("All", "Active", "Recent", "Most Jobs")
            tabs.forEach { tab ->
                val isSelected = selectedFilterTab == tab
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSelected) accentColor else if (isLight) Color(0xFFF1F5F9) else Color(0xFF1E293B))
                        .border(
                            1.dp,
                            if (isSelected) accentColor else if (isLight) Color(0xFFCBD5E1) else Color(0xFF334155),
                            RoundedCornerShape(20.dp)
                        )
                        .clickable { selectedFilterTab = tab }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        tab,
                        color = if (isSelected) Color.White else textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp
                    )
                }
            }
        }

        // 4. Customer List / Empty State
        if (filtered.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 20.dp)
                    .border(1.dp, BorderSilver, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.PeopleOutline, contentDescription = null, tint = accentColor, modifier = Modifier.size(48.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("No Customers Found", fontWeight = FontWeight.Bold, color = textColor, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        if (search.isNotBlank()) "No records matched '$search'" else "Start building your enterprise client list",
                        color = grayText,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Add Customer Now", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filtered) { cust ->
                    val customerJobs = services.filter { it.customerPhone == cust.phone }
                    val completedJobs = customerJobs.count { it.status == ServiceStatus.COMPLETED }
                    val activeJobs = customerJobs.size - completedJobs

                    // Initials generator
                    val initials = cust.name.trim().split(" ")
                        .filter { it.isNotBlank() }
                        .mapNotNull { it.firstOrNull()?.toString() }
                        .take(2)
                        .joinToString("")
                        .uppercase()
                        .ifEmpty { "RE" }

                    CustomerGlassCard(
                        cust = cust,
                        customerJobs = customerJobs,
                        completedJobs = completedJobs,
                        activeJobs = activeJobs,
                        initials = initials,
                        accentColor = accentColor,
                        isLight = isLight,
                        onCallClick = {
                            dialConfirmCustomerName = cust.name
                            phoneToDial = cust.phone
                        },
                        onWhatsAppClick = {
                            try {
                                val clean = cust.phone.replace(Regex("[^0-9]"), "")
                                val uri = android.net.Uri.parse("https://api.whatsapp.com/send?phone=91$clean&text=Hello%20${cust.name},%20Greetings%20from%20Rattan%20Electricals!")
                                val waIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                context.startActivity(waIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "WhatsApp unavailable", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onEditClick = { customerToEdit = cust },
                        onDeleteClick = { customerToDelete = cust }
                    )
                }
            }
        }
    }

    // Dialing confirmation dialog
    if (phoneToDial != null) {
        AlertDialog(
            onDismissRequest = { phoneToDial = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp),
            title = { Text("Call Customer?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Do you want to open the phone dialer to call $dialConfirmCustomerName at ${formatPhoneNumber(phoneToDial ?: "")}?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                data = android.net.Uri.parse("tel:${phoneToDial}")
                            }
                            context.startActivity(dialIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Dialer unavailable: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                        phoneToDial = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Call Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { phoneToDial = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Delete confirmation dialog
    if (customerToDelete != null) {
        val cust = customerToDelete!!
        AlertDialog(
            onDismissRequest = { customerToDelete = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp),
            title = { Text("Delete Customer Record?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to remove '${cust.name}' from Rattan Electricals customer master database?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteCustomer(cust.id)
                        customerToDelete = null
                        Toast.makeText(context, "Customer deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToDelete = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Edit Customer Dialog
    if (customerToEdit != null) {
        val cust = customerToEdit!!
        var eName by remember { mutableStateOf(cust.name) }
        var ePhone by remember { mutableStateOf(cust.phone) }
        var eAddress by remember { mutableStateOf(cust.address) }

        AlertDialog(
            onDismissRequest = { customerToEdit = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp),
            title = { Text("Edit Customer Profile", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = eName,
                        onValueChange = { eName = it },
                        label = { Text("Customer Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = ePhone,
                        onValueChange = { ePhone = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = eAddress,
                        onValueChange = { eAddress = it },
                        label = { Text("Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (eName.isNotBlank() && ePhone.isNotBlank()) {
                            repository.updateCustomer(cust.copy(name = eName.trim(), phone = ePhone.trim(), address = eAddress.trim()))
                            customerToEdit = null
                            Toast.makeText(context, "Customer profile updated!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { customerToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Register New Customer Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, tint = accentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Register New Customer", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Customer Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                    )
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("Phone Number (10 digits)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                    )
                    OutlinedTextField(
                        value = newAddress,
                        onValueChange = { newAddress = it },
                        label = { Text("Street Address / Sector") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanedPhone = cleanRawPhone(newPhone)
                        if (newName.isBlank()) {
                            Toast.makeText(context, "Please enter customer name!", Toast.LENGTH_SHORT).show()
                        } else if (cleanedPhone.length != 10) {
                            Toast.makeText(context, "Please enter a valid 10-digit phone number!", Toast.LENGTH_SHORT).show()
                        } else {
                            repository.addCustomer(Customer(name = newName.trim(), phone = cleanedPhone, address = newAddress.trim()))
                            newName = ""
                            newPhone = ""
                            newAddress = ""
                            showAddDialog = false
                            Toast.makeText(context, "Customer registered successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Register Customer", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun ServiceRequestGlassCard(
    req: ServiceRequest,
    accentColor: Color,
    isLight: Boolean,
    onStatusChange: (ServiceStatus) -> Unit,
    onCallClick: () -> Unit,
    onWhatsAppClick: () -> Unit,
    onAssignClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val cardScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "srvCardScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "srvPulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "srvPulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "srvPulseAlpha"
    )

    val prioColor = when (req.priority) {
        Priority.LOW -> Color(0xFF64748B)
        Priority.MEDIUM -> Color(0xFFF59E0B)
        Priority.HIGH -> Color(0xFFEA580C)
        Priority.EMERGENCY -> NeonRed
    }

    val statusColor = when (req.status) {
        ServiceStatus.CREATED -> ElectricOrange
        ServiceStatus.ASSIGNED -> ElectricBlue
        ServiceStatus.IN_PROGRESS -> NeonYellow
        ServiceStatus.COMPLETED -> NeonGreen
        ServiceStatus.CLOSED -> Color(0xFF64748B)
    }

    // Theme-adaptive glass gradient background
    val cardGradient = if (isLight) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.96f),
                Color(0xFFF8FAFC).copy(alpha = 0.92f),
                Color(0xFFEFF6FF).copy(alpha = 0.85f)
            ),
            start = Offset(0f, 0f),
            end = Offset(400f, 400f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0A182E).copy(alpha = 0.94f),
                Color(0xFF092535).copy(alpha = 0.92f),
                Color(0xFF063B47).copy(alpha = 0.88f)
            ),
            start = Offset(0f, 0f),
            end = Offset(400f, 400f)
        )
    }

    // Subtle border glow with cyan-teal glass shimmer
    val glassBorderGlow = if (isLight) {
        Brush.linearGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.45f),
                Color(0xFF38BDF8).copy(alpha = 0.35f),
                Color(0xFFE2E8F0)
            ),
            start = Offset(0f, 0f),
            end = Offset(300f, 300f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF38BDF8).copy(alpha = 0.50f),
                Color(0xFF14B8A6).copy(alpha = 0.38f),
                Color(0xFF0284C7).copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.15f)
            ),
            start = Offset(0f, 0f),
            end = Offset(300f, 300f)
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isLight) 3.dp else 6.dp,
            pressedElevation = 1.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .border(1.2.dp, glassBorderGlow, RoundedCornerShape(18.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardGradient)
        ) {
            if (!isLight) {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    drawCircle(
                        color = Color(0xFF0EA5E9).copy(alpha = 0.08f),
                        radius = 45.dp.toPx(),
                        center = Offset(size.width * 0.88f, size.height * 0.2f)
                    )
                    drawCircle(
                        color = Color(0xFF14B8A6).copy(alpha = 0.06f),
                        radius = 35.dp.toPx(),
                        center = Offset(size.width * 0.12f, size.height * 0.8f)
                    )
                    drawCircle(
                        color = Color(0xFF38BDF8).copy(alpha = 0.25f * pulseAlpha),
                        radius = 2.dp.toPx(),
                        center = Offset(size.width * 0.8f, size.height * 0.7f)
                    )
                }
            }

            Column(modifier = Modifier.padding(14.dp)) {
                // Top Row: Category + Priority + Status Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f, fill = false).padding(end = 6.dp)
                    ) {
                        // Category Tag
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor.copy(alpha = if (isLight) 0.12f else 0.20f))
                                .border(0.8.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.5.dp)
                        ) {
                            Text(
                                text = req.category,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isLight) accentColor else Color(0xFF67E8F9),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }

                        // Priority Pill
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(prioColor.copy(alpha = if (isLight) 0.14f else 0.22f))
                                .border(0.8.dp, prioColor.copy(alpha = 0.50f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (req.priority == Priority.EMERGENCY || req.priority == Priority.HIGH) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(prioColor)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                }
                                Text(
                                    text = req.priority.label,
                                    color = if (isLight && req.priority == Priority.MEDIUM) Color(0xFFB45309) else prioColor,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    // Status Dropdown Button with live pulsing dot
                    var showStatusDropdown by remember { mutableStateOf(false) }
                    Box {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(statusColor.copy(alpha = if (isLight) 0.14f else 0.22f))
                                .border(0.8.dp, statusColor.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                .clickable { showStatusDropdown = true }
                                .padding(horizontal = 9.dp, vertical = 4.5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = req.status.label,
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isLight && req.status == ServiceStatus.IN_PROGRESS) Color(0xFFB45309) else statusColor,
                                    maxLines = 1
                                )
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = null,
                                    tint = if (isLight && req.status == ServiceStatus.IN_PROGRESS) Color(0xFFB45309) else statusColor,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showStatusDropdown,
                            onDismissRequest = { showStatusDropdown = false },
                            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                        ) {
                            ServiceStatus.values().forEach { st ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            st.label,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    },
                                    onClick = {
                                        onStatusChange(st)
                                        showStatusDropdown = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Customer Details & Quick Calls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                        Text(
                            text = req.customerName,
                            fontWeight = FontWeight.Bold,
                            color = if (isLight) Color(0xFF0F172A) else Color.White,
                            fontSize = 15.5.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Phone, contentDescription = null, tint = accentColor, modifier = Modifier.size(11.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = formatPhoneNumber(req.customerPhone),
                                fontSize = 12.sp,
                                color = if (isLight) Color(0xFF475569) else Color(0xFF94A3B8),
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Quick Contact Action Row: Direct Call & Direct WhatsApp
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Sleek Direct Call Button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isLight) Color(0xFFDCFCE7) else Color(0xFF064E3B).copy(alpha = 0.65f)
                                )
                                .border(
                                    1.dp,
                                    if (isLight) Color(0xFF86EFAC) else Color(0xFF10B981).copy(alpha = 0.5f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable(onClick = onCallClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Call,
                                contentDescription = "Call Customer",
                                tint = if (isLight) Color(0xFF15803D) else Color(0xFF4ADE80),
                                modifier = Modifier.size(17.dp)
                            )
                        }

                        // Sleek Direct WhatsApp Button
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isLight) Color(0xFFF0FDF4) else Color(0xFF022C22).copy(alpha = 0.65f)
                                )
                                .border(
                                    1.dp,
                                    Color(0xFF25D366).copy(alpha = 0.45f),
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable(onClick = onWhatsAppClick),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_whatsapp),
                                contentDescription = "WhatsApp Customer",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(9.dp))

                // Problem Box - Frosted Inner Container
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isLight) Color(0xFFF1F5F9).copy(alpha = 0.85f) else Color(0xFF071322).copy(alpha = 0.75f))
                        .border(0.8.dp, if (isLight) Color(0xFFE2E8F0) else Color(0xFF1E3A8A).copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(10.dp)
                ) {
                    Column {
                        Text(
                            text = "Problem:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = req.problemDescription,
                            color = if (isLight) Color(0xFF0F172A) else Color(0xFFF8FAFC),
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Site Address
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8), modifier = Modifier.size(13.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = req.customerAddress,
                        color = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8),
                        fontSize = 11.5.sp,
                        maxLines = 2,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = if (isLight) Color(0xFFE2E8F0) else Color(0xFF1E293B).copy(alpha = 0.8f))
                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Assignee & Action Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Icon(
                            Icons.Default.Engineering,
                            contentDescription = null,
                            tint = if (req.assignedWorkerName != null) accentColor else ElectricOrange,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (req.assignedWorkerName != null) "Assigned: ${req.assignedWorkerName}" else "Unassigned Technician",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (req.assignedWorkerName != null) {
                                    if (isLight) Color(0xFF0F172A) else Color.White
                                } else ElectricOrange,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = req.preferredDateTime,
                                fontSize = 10.sp,
                                color = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Compact Action Row: Primary "Assign" / "Reassign" Button + "⋯ More" Menu (Edit Details, Delete Request)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = onAssignClick,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor.copy(alpha = if (isLight) 0.15f else 0.25f),
                                contentColor = if (isLight) accentColor else Color(0xFF67E8F9)
                            ),
                            border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = if (req.assignedWorkerName != null) "Reassign" else "Assign",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Compact "⋯ More" Overflow Menu
                        var showServiceCardMenu by remember { mutableStateOf(false) }
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isLight) Color(0xFFF1F5F9) else Color(0xFF1E293B).copy(alpha = 0.6f)
                                    )
                                    .border(0.8.dp, if (isLight) Color(0xFFCBD5E1) else Color(0xFF334155), RoundedCornerShape(8.dp))
                                    .clickable { showServiceCardMenu = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Service Actions",
                                    tint = if (isLight) Color(0xFF475569) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(17.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showServiceCardMenu,
                                onDismissRequest = { showServiceCardMenu = false },
                                modifier = Modifier
                                    .background(if (isLight) Color.White else Color(0xFF0F172A))
                                    .border(1.dp, if (isLight) Color(0xFFE2E8F0) else Color(0xFF334155), RoundedCornerShape(12.dp))
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = null,
                                            tint = if (isLight) Color(0xFF475569) else Color(0xFF94A3B8),
                                            modifier = Modifier.size(17.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            "Edit Details",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = if (isLight) Color(0xFF0F172A) else Color.White
                                        )
                                    },
                                    onClick = {
                                        showServiceCardMenu = false
                                        onEditClick()
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.DeleteOutline,
                                            contentDescription = null,
                                            tint = NeonRed,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            "Delete Request",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = NeonRed
                                        )
                                    },
                                    onClick = {
                                        showServiceCardMenu = false
                                        onDeleteClick()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- MODULE 3: SERVICES WORKFLOW SCREEN ---
@Composable
fun ServicesScreen(repository: ReLectRepository, accentColor: Color) {
    val services by repository.services.collectAsState()
    val workers by repository.workers.collectAsState()

    val isLight = MaterialTheme.colorScheme.surface == Color.White
    val textColor = if (isLight) Color(0xFF0F172A) else Color.White
    val grayText = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8)
    val lightGrayText = if (isLight) Color(0xFF334155) else Color(0xFFCBD5E1)
    val cardBg = if (isLight) Color.White else Color(0xFF0F172A)

    var statusFilter by remember { mutableStateOf<ServiceStatus?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var showAssignModal by remember { mutableStateOf<ServiceRequest?>(null) }
    var jobToDelete by remember { mutableStateOf<ServiceRequest?>(null) }
    var jobToEdit by remember { mutableStateOf<ServiceRequest?>(null) }

    // Manual Job Logging States
    var showAddDialog by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var newAddress by remember { mutableStateOf("") }
    var newProblem by remember { mutableStateOf("") }
    var newPriority by remember { mutableStateOf(Priority.MEDIUM) }
    var newCategory by remember { mutableStateOf("Wiring Check") }

    val context = LocalContext.current

    val filtered = remember(services, statusFilter, searchQuery) {
        services.filter { req ->
            val matchesFilter = statusFilter == null || req.status == statusFilter
            val matchesSearch = searchQuery.isBlank() ||
                req.customerName.contains(searchQuery, true) ||
                req.customerPhone.contains(searchQuery) ||
                req.problemDescription.contains(searchQuery, true) ||
                req.category.contains(searchQuery, true) ||
                (req.assignedWorkerName?.contains(searchQuery, true) == true)
            matchesFilter && matchesSearch
        }
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // 1. Header with Live Count & Action (Consistent styling across all screens)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Service Requests",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        letterSpacing = (-0.3).sp
                    )
                    val pendingCount = services.count { it.status != ServiceStatus.COMPLETED }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentColor.copy(alpha = 0.12f))
                            .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "$pendingCount Active",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = accentColor
                        )
                    }
                }
                Text(
                    text = "Track, assign, & manage electrical jobs",
                    fontSize = 12.sp,
                    color = grayText,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 7.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("New Job", fontWeight = FontWeight.Bold, fontSize = 11.5.sp)
            }
        }

        // 2. Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by customer, technician, problem description...", fontSize = 12.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = accentColor) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = grayText, modifier = Modifier.size(18.dp))
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                unfocusedBorderColor = BorderSilver,
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface
            )
        )

        // 3. Status filter pills with counts
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val allCount = services.size
            val isAllSelected = statusFilter == null
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isAllSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { statusFilter = null }
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("All", color = if (isAllSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("($allCount)", color = if (isAllSelected) Color.White.copy(alpha = 0.8f) else grayText, fontSize = 10.sp)
                }
            }

            ServiceStatus.values().forEach { stat ->
                val isSel = statusFilter == stat
                val statCount = services.count { it.status == stat }
                val statColor = when (stat) {
                    ServiceStatus.CREATED -> ElectricOrange
                    ServiceStatus.ASSIGNED -> ElectricBlue
                    ServiceStatus.IN_PROGRESS -> NeonYellow
                    ServiceStatus.COMPLETED -> NeonGreen
                    ServiceStatus.CLOSED -> Color(0xFF64748B)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(if (isSel) statColor else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { statusFilter = stat }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            stat.label,
                            color = if (isSel) (if (stat == ServiceStatus.IN_PROGRESS) Color(0xFF0F172A) else Color.White) else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "($statCount)",
                            color = if (isSel) (if (stat == ServiceStatus.IN_PROGRESS) Color(0xFF0F172A).copy(alpha = 0.8f) else Color.White.copy(alpha = 0.8f)) else grayText,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        }

        // 4. Job List / Empty State
        if (filtered.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
                    .border(1.dp, BorderSilver, RoundedCornerShape(18.dp)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Handyman,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "No Service Requests Found",
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        if (searchQuery.isNotBlank()) "No jobs matching '$searchQuery'" else "All scheduled requests are up to date.",
                        color = grayText,
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = { showAddDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Log New Service Job", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(filtered) { req ->
                    ServiceRequestGlassCard(
                        req = req,
                        accentColor = accentColor,
                        isLight = isLight,
                        onStatusChange = { newStatus ->
                            repository.updateServiceRequest(req.copy(status = newStatus))
                        },
                        onCallClick = {
                            try {
                                val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                    data = android.net.Uri.parse("tel:${req.customerPhone}")
                                }
                                context.startActivity(dialIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Dialer unavailable", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onWhatsAppClick = {
                            try {
                                val clean = req.customerPhone.replace(Regex("[^0-9]"), "")
                                val uri = android.net.Uri.parse("https://api.whatsapp.com/send?phone=91$clean&text=Hello%20${req.customerName},%20regarding%20your%20electrical%20service%20request%20(${req.category}):")
                                val waIntent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                context.startActivity(waIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "WhatsApp unavailable", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onAssignClick = { showAssignModal = req },
                        onEditClick = { jobToEdit = req },
                        onDeleteClick = { jobToDelete = req }
                    )
                }
            }
        }
    }

    // Assign Worker Modal
    if (showAssignModal != null) {
        val req = showAssignModal!!
        AlertDialog(
            onDismissRequest = { showAssignModal = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Engineering, contentDescription = null, tint = accentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Assign Field Electrician", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select technician for '${req.category}' job:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    workers.forEach { worker ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    repository.updateServiceRequest(
                                        req.copy(
                                            assignedWorkerId = worker.id,
                                            assignedWorkerName = worker.name,
                                            status = ServiceStatus.ASSIGNED
                                        )
                                    )
                                    repository.updateWorker(worker.copy(status = WorkerStatus.ASSIGNED_NOT_STARTED))
                                    showAssignModal = null
                                    Toast.makeText(context, "Job assigned to ${worker.name}!", Toast.LENGTH_SHORT).show()
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(accentColor.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(worker.name.take(1), fontWeight = FontWeight.Bold, color = accentColor)
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(worker.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                                        Text(worker.designation, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }

                                val (statusText, statDotColor) = when (worker.status) {
                                    WorkerStatus.WORKING -> "Active Job" to NeonGreen
                                    WorkerStatus.ASSIGNED_NOT_STARTED -> "Assigned" to NeonYellow
                                    WorkerStatus.IDLE -> "Available" to ElectricBlue
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(statDotColor))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(statusText, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = statDotColor)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAssignModal = null }) {
                    Text("Close")
                }
            }
        )
    }

    // Delete Job Confirmation Dialog
    if (jobToDelete != null) {
        val req = jobToDelete!!
        AlertDialog(
            onDismissRequest = { jobToDelete = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(18.dp),
            title = { Text("Delete Service Job?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Are you sure you want to delete the job request for '${req.customerName}' (${req.category})?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteServiceRequest(req.id)
                        jobToDelete = null
                        Toast.makeText(context, "Service job deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { jobToDelete = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Edit Service Job Dialog
    if (jobToEdit != null) {
        val req = jobToEdit!!
        val categories = listOf("Wiring Check", "AC Installation / Repair", "Fuse / MCB Repair", "Appliance Maintenance", "Solar & Inverter", "Generator & Heavy Power")
        var editName by remember(req.id) { mutableStateOf(req.customerName) }
        var editPhone by remember(req.id) { mutableStateOf(req.customerPhone) }
        var editAddress by remember(req.id) { mutableStateOf(req.customerAddress) }
        var editProblem by remember(req.id) { mutableStateOf(req.problemDescription) }
        var editCategory by remember(req.id) { mutableStateOf(req.category) }
        var editPriority by remember(req.id) { mutableStateOf(req.priority) }

        AlertDialog(
            onDismissRequest = { jobToEdit = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = accentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Edit Job Details", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Customer Name *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { if (it.length <= 10 && it.all { char -> char.isDigit() }) editPhone = it },
                        label = { Text("Customer Phone (10 Digits) *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = editAddress,
                        onValueChange = { editAddress = it },
                        label = { Text("Site / Home Address") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 2,
                        shape = RoundedCornerShape(10.dp)
                    )
                    OutlinedTextField(
                        value = editProblem,
                        onValueChange = { editProblem = it },
                        label = { Text("Problem Description *") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false,
                        maxLines = 3,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Text("Service Category", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(categories) { cat ->
                            val isSelected = editCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { editCategory = cat },
                                label = { Text(cat, fontSize = 10.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accentColor.copy(alpha = 0.2f),
                                    selectedLabelColor = accentColor
                                )
                            )
                        }
                    }

                    Text("Priority Level", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Priority.values().forEach { pri ->
                            val isSelected = editPriority == pri
                            val chipColor = when (pri) {
                                Priority.LOW -> Color(0xFF64748B)
                                Priority.MEDIUM -> Color(0xFFF59E0B)
                                Priority.HIGH -> Color(0xFFEA580C)
                                Priority.EMERGENCY -> NeonRed
                            }
                            FilterChip(
                                selected = isSelected,
                                onClick = { editPriority = pri },
                                label = { Text(pri.name, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = chipColor.copy(alpha = 0.2f),
                                    selectedLabelColor = chipColor
                                ),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanedPhone = editPhone.filter { it.isDigit() }
                        if (editName.isBlank()) {
                            Toast.makeText(context, "Please enter customer name!", Toast.LENGTH_SHORT).show()
                        } else if (cleanedPhone.length != 10) {
                            Toast.makeText(context, "Please enter a valid 10-digit phone number!", Toast.LENGTH_SHORT).show()
                        } else if (editProblem.isBlank()) {
                            Toast.makeText(context, "Please describe the problem!", Toast.LENGTH_SHORT).show()
                        } else {
                            repository.updateServiceRequest(
                                req.copy(
                                    customerName = editName.trim(),
                                    customerPhone = cleanedPhone,
                                    customerAddress = editAddress.ifBlank { "Shop Walk-in" }.trim(),
                                    problemDescription = editProblem.trim(),
                                    category = editCategory.ifBlank { "Wiring Check" },
                                    priority = editPriority
                                )
                            )
                            jobToEdit = null
                            Toast.makeText(context, "Job details updated successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Save Changes", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { jobToEdit = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Log New Service Job Dialog
    if (showAddDialog) {
        val categories = listOf("Wiring Check", "AC Installation / Repair", "Fuse / MCB Repair", "Appliance Maintenance", "Solar & Inverter", "Generator & Heavy Power")

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AddBusiness, contentDescription = null, tint = accentColor)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Log New Service Job", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text("Customer Full Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                    )
                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("Phone Number (10 digits)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                    )
                    OutlinedTextField(
                        value = newAddress,
                        onValueChange = { newAddress = it },
                        label = { Text("Site / Sector Address") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                    )
                    OutlinedTextField(
                        value = newProblem,
                        onValueChange = { newProblem = it },
                        label = { Text("Problem Description / Work Required") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                    )

                    // Priority Selector
                    Text("Select Job Priority:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Priority.values().forEach { prio ->
                            val active = newPriority == prio
                            val color = when (prio) {
                                Priority.LOW -> Color(0xFF64748B)
                                Priority.MEDIUM -> Color(0xFFF59E0B)
                                Priority.HIGH -> Color(0xFFEA580C)
                                Priority.EMERGENCY -> NeonRed
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (active) color else MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable { newPriority = prio }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    prio.label,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (active) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Category Selector
                    Text("Select Service Category:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        categories.chunked(2).forEach { rowCats ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowCats.forEach { cat ->
                                    val isSelected = newCategory == cat
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isSelected) accentColor else MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable { newCategory = cat }
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            cat,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanedPhone = newPhone.filter { it.isDigit() }
                        if (newName.isBlank()) {
                            Toast.makeText(context, "Please enter customer name!", Toast.LENGTH_SHORT).show()
                        } else if (cleanedPhone.length != 10) {
                            Toast.makeText(context, "Please enter a valid 10-digit phone number!", Toast.LENGTH_SHORT).show()
                        } else if (newProblem.isBlank()) {
                            Toast.makeText(context, "Please describe the problem!", Toast.LENGTH_SHORT).show()
                        } else {
                            repository.addServiceRequest(
                                ServiceRequest(
                                    customerName = newName.trim(),
                                    customerPhone = cleanedPhone,
                                    customerAddress = newAddress.ifBlank { "Shop Walk-in" }.trim(),
                                    problemDescription = newProblem.trim(),
                                    category = newCategory.ifBlank { "Wiring Check" },
                                    priority = newPriority,
                                    preferredDateTime = "Immediate / Asap"
                                )
                            )
                            newName = ""
                            newPhone = ""
                            newAddress = ""
                            newProblem = ""
                            newPriority = Priority.MEDIUM
                            newCategory = "Wiring Check"
                            showAddDialog = false
                            Toast.makeText(context, "Service job logged successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Register Job", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

@Composable
fun WorkerAttendanceCalendar(attendance: List<AttendanceRecord>, accentColor: Color) {
    val context = LocalContext.current
    val isLight = MaterialTheme.colorScheme.surface == Color.White
    val textColor = if (isLight) Color(0xFF111827) else Color.White
    val grayText = if (isLight) Color(0xFF4B5563) else Color.Gray

    var calendarState by remember { mutableStateOf(java.util.Calendar.getInstance()) }
    val displayYear = calendarState.get(java.util.Calendar.YEAR)
    val displayMonth = calendarState.get(java.util.Calendar.MONTH) // 0-indexed

    val monthFormatter = remember { java.text.SimpleDateFormat("MMMM yyyy", java.util.Locale.getDefault()) }
    val monthName = remember(calendarState) {
        monthFormatter.format(calendarState.time)
    }

    val firstDayOfWeek = remember(calendarState) {
        val cal = java.util.Calendar.getInstance()
        cal.time = calendarState.time
        cal.set(java.util.Calendar.DAY_OF_MONTH, 1)
        cal.get(java.util.Calendar.DAY_OF_WEEK) // Sunday = 1, Monday = 2...
    }
    val maxDays = remember(calendarState) {
        val cal = java.util.Calendar.getInstance()
        cal.time = calendarState.time
        cal.getActualMaximum(java.util.Calendar.DAY_OF_MONTH)
    }

    val daysOfWeek = listOf("Su", "Mo", "Tu", "We", "Th", "Fr", "Sa")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0F172A).copy(alpha = 0.85f), RoundedCornerShape(12.dp))
            .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        val newCal = java.util.Calendar.getInstance()
                        newCal.time = calendarState.time
                        newCal.add(java.util.Calendar.MONTH, -1)
                        calendarState = newCal
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Prev Month", tint = accentColor)
                }
                
                Text(
                    text = monthName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                IconButton(
                    onClick = {
                        val newCal = java.util.Calendar.getInstance()
                        newCal.time = calendarState.time
                        newCal.add(java.util.Calendar.MONTH, 1)
                        calendarState = newCal
                    },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next Month", tint = accentColor)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.25f))
                            .border(1.dp, accentColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Today", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(NeonYellow))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Early Leave", fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            daysOfWeek.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        val emptyCellsBefore = firstDayOfWeek - 1
        val totalCells = emptyCellsBefore + maxDays
        val rowsCount = (totalCells + 6) / 7

        for (r in 0 until rowsCount) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 3.dp)
            ) {
                for (c in 0 until 7) {
                    val cellIndex = r * 7 + c
                    val day = cellIndex - emptyCellsBefore + 1

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        if (day in 1..maxDays) {
                            val dateStr = String.format(java.util.Locale.US, "%04d-%02d-%02d", displayYear, displayMonth + 1, day)
                            val matchedRecords = attendance.filter { it.date == dateStr }
                            val isPresent = matchedRecords.isNotEmpty()
                            val isEarlyLeave = isPresent && matchedRecords.any { 
                                it.clockOutTime?.contains("Early Leave") == true || it.clockOutTime?.contains("jldi") == true 
                            }

                            val todayCal = remember { java.util.Calendar.getInstance() }
                            val isToday = displayYear == todayCal.get(java.util.Calendar.YEAR) &&
                                          displayMonth == todayCal.get(java.util.Calendar.MONTH) &&
                                          day == todayCal.get(java.util.Calendar.DAY_OF_MONTH)

                            val cellBgColor = when {
                                isEarlyLeave -> NeonYellow
                                isToday -> accentColor.copy(alpha = 0.25f)
                                else -> Color.Transparent
                            }
                            val cellTextColor = when {
                                isEarlyLeave -> Color.Black
                                isToday -> Color.White
                                isPresent -> Color.White
                                else -> Color.White.copy(alpha = 0.35f)
                            }

                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(cellBgColor)
                                    .then(
                                        if (isToday) {
                                            Modifier.border(1.5.dp, accentColor, CircleShape)
                                        } else {
                                            Modifier
                                        }
                                    )
                                    .clickable(enabled = isPresent) {
                                        val recordText = matchedRecords.joinToString("\n") { 
                                            "In: ${it.clockInTime}${if (!it.clockOutTime.isNullOrBlank()) " | Out: ${it.clockOutTime}" else ""}" 
                                        }
                                        Toast.makeText(context, "Date: $dateStr\n$recordText", Toast.LENGTH_LONG).show()
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = cellTextColor
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WorkerGlassCard(
    worker: Worker,
    accentColor: Color,
    isLight: Boolean,
    isTodayClockedIn: Boolean,
    isTodayCompleted: Boolean,
    isExpanded: Boolean,
    onExpandToggle: () -> Unit,
    onPhoneClick: () -> Unit,
    onClockActionClick: () -> Unit,
    onViewCalendarClick: () -> Unit
) {
    var isPressed by remember { mutableStateOf(false) }
    val cardScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "workerCardScale"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "workerPulseTransition")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "workerPulseScale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "workerPulseAlpha"
    )

    val statusColor = when (worker.status) {
        WorkerStatus.WORKING -> NeonGreen
        WorkerStatus.ASSIGNED_NOT_STARTED -> NeonYellow
        WorkerStatus.IDLE -> NeonRed
    }

    val cardGradient = if (isLight) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.96f),
                Color(0xFFF8FAFC).copy(alpha = 0.92f),
                Color(0xFFEFF6FF).copy(alpha = 0.85f)
            ),
            start = Offset(0f, 0f),
            end = Offset(400f, 400f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0A182E).copy(alpha = 0.94f),
                Color(0xFF092535).copy(alpha = 0.92f),
                Color(0xFF063B47).copy(alpha = 0.88f)
            ),
            start = Offset(0f, 0f),
            end = Offset(400f, 400f)
        )
    }

    val glassBorderGlow = if (isLight) {
        Brush.linearGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.45f),
                Color(0xFF38BDF8).copy(alpha = 0.35f),
                Color(0xFFE2E8F0)
            ),
            start = Offset(0f, 0f),
            end = Offset(300f, 300f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF38BDF8).copy(alpha = 0.50f),
                Color(0xFF14B8A6).copy(alpha = 0.38f),
                Color(0xFF0284C7).copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.15f)
            ),
            start = Offset(0f, 0f),
            end = Offset(300f, 300f)
        )
    }

    val initials = worker.name.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifBlank { "T" }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isLight) 3.dp else 6.dp,
            pressedElevation = 1.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .border(1.2.dp, glassBorderGlow, RoundedCornerShape(18.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardGradient)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Top Row: Avatar + Info + Work Status Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Frosted Tech Avatar
                        Box(
                            modifier = Modifier.size(46.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size((46 * pulseScale).dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = if (isLight) 0.10f * pulseAlpha else 0.16f * pulseAlpha))
                            )
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(accentColor, Color(0xFF0284C7))
                                        )
                                    )
                                    .border(1.2.dp, Color.White.copy(alpha = 0.35f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initials,
                                    color = Color.White,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f).padding(end = 6.dp)) {
                            Text(
                                text = worker.name,
                                fontWeight = FontWeight.Bold,
                                color = if (isLight) Color(0xFF0F172A) else Color.White,
                                fontSize = 15.5.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                text = worker.designation,
                                fontSize = 12.sp,
                                color = accentColor,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 2.dp).clickable { onPhoneClick() }
                            ) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = accentColor, modifier = Modifier.size(11.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = formatPhoneNumber(worker.phone),
                                    fontSize = 12.sp,
                                    color = if (isLight) Color(0xFF475569) else Color(0xFF94A3B8),
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = "Sal: ₹${String.format("%,.0f", worker.salary)}/mo • Joined: ${worker.joiningDate}",
                                fontSize = 10.5.sp,
                                color = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Status Pill + 3-dots Overflow Menu
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(statusColor.copy(alpha = if (isLight) 0.14f else 0.22f))
                                .border(0.8.dp, statusColor.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                                .padding(horizontal = 9.dp, vertical = 4.5.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(statusColor)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = worker.status.label,
                                    color = if (isLight && worker.status == WorkerStatus.ASSIGNED_NOT_STARTED) Color(0xFFB45309) else statusColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.5.sp
                                )
                            }
                        }

                        val workerContext = LocalContext.current
                        var showWorkerMenu by remember { mutableStateOf(false) }
                        Box {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isLight) Color(0xFFF1F5F9) else Color(0xFF1E293B).copy(alpha = 0.6f)
                                    )
                                    .border(0.8.dp, if (isLight) Color(0xFFCBD5E1) else Color(0xFF334155), RoundedCornerShape(8.dp))
                                    .clickable { showWorkerMenu = true },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MoreVert,
                                    contentDescription = "More Technician Actions",
                                    tint = if (isLight) Color(0xFF475569) else Color(0xFF94A3B8),
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            DropdownMenu(
                                expanded = showWorkerMenu,
                                onDismissRequest = { showWorkerMenu = false },
                                modifier = Modifier
                                    .background(if (isLight) Color.White else Color(0xFF0F172A))
                                    .border(1.dp, if (isLight) Color(0xFFE2E8F0) else Color(0xFF334155), RoundedCornerShape(12.dp))
                            ) {
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Call,
                                            contentDescription = null,
                                            tint = NeonGreen,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            "Call Technician",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = if (isLight) Color(0xFF0F172A) else Color.White
                                        )
                                    },
                                    onClick = {
                                        showWorkerMenu = false
                                        onPhoneClick()
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_whatsapp),
                                            contentDescription = null,
                                            tint = Color.Unspecified,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            "WhatsApp Technician",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp,
                                            color = if (isLight) Color(0xFF0F172A) else Color.White
                                        )
                                    },
                                    onClick = {
                                        showWorkerMenu = false
                                        val clean = worker.phone.replace(Regex("[^0-9]"), "").takeLast(10)
                                        val uri = android.net.Uri.parse("https://api.whatsapp.com/send?phone=91$clean&text=Hello%20${worker.name},%20Greetings%20from%20Rattan%20Electricals:")
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                        try {
                                            workerContext.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(workerContext, "WhatsApp unavailable", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                                DropdownMenuItem(
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = accentColor,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    },
                                    text = {
                                        Text(
                                            "Monthly Calendar",
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = if (isLight) Color(0xFF0F172A) else Color.White
                                        )
                                    },
                                    onClick = {
                                        showWorkerMenu = false
                                        onViewCalendarClick()
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = if (isLight) Color(0xFFE2E8F0) else Color(0xFF1E293B).copy(alpha = 0.8f))
                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Row: Activity Timeline Trigger & Check In/Out Action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onExpandToggle() }
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null,
                            tint = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isExpanded) "Hide Timeline" else "Activity Timeline",
                            fontSize = 11.5.sp,
                            color = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8),
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Button(
                        onClick = onClockActionClick,
                        enabled = !isTodayCompleted,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isTodayClockedIn) NeonRed.copy(alpha = 0.9f) else if (isTodayCompleted) Color.Gray else NeonGreen.copy(alpha = 0.9f),
                            contentColor = Color.Black,
                            disabledContainerColor = if (isLight) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                            disabledContentColor = if (isLight) Color(0xFF94A3B8) else Color.Gray
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 5.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Icon(
                            imageVector = if (isTodayClockedIn) Icons.AutoMirrored.Filled.DirectionsRun else if (isTodayCompleted) Icons.Default.CheckCircle else Icons.AutoMirrored.Filled.Login,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = if (isTodayCompleted) (if (isLight) Color(0xFF94A3B8) else Color.Gray) else Color.Black
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = if (isTodayClockedIn) "Check Out" else if (isTodayCompleted) "Completed" else "Check In",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isTodayCompleted) (if (isLight) Color(0xFF94A3B8) else Color.Gray) else Color.Black
                        )
                    }
                }

                if (isExpanded) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = if (isLight) Color(0xFFE2E8F0) else Color(0xFF1E293B).copy(alpha = 0.8f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = onViewCalendarClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor.copy(alpha = if (isLight) 0.12f else 0.20f),
                            contentColor = if (isLight) accentColor else Color(0xFF67E8F9)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(0.8.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Event, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("View Attendance Calendar", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// --- MODULE 4: WORKERS SCREEN ---
@Composable
fun WorkersScreen(repository: ReLectRepository, accentColor: Color) {
    val workers by repository.workers.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    // Early Departure Dialog States
    var showEarlyDepartureDialog by remember { mutableStateOf(false) }
    var selectedWorkerForDeparture by remember { mutableStateOf<Worker?>(null) }
    var departureReasonText by remember { mutableStateOf("Aaj jldi chla gya") }
    var departureRecordToUpdate by remember { mutableStateOf<AttendanceRecord?>(null) }

    var selectedWorkerForCalendar by remember { mutableStateOf<Worker?>(null) }

    val isLight = MaterialTheme.colorScheme.surface == Color.White
    val textColor = if (isLight) Color(0xFF111827) else Color.White
    val grayText = if (isLight) Color(0xFF4B5563) else Color.Gray
    val lightGrayText = if (isLight) Color(0xFF374151) else Color.LightGray

    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var designation by remember { mutableStateOf("Electrician") }

    // Dialer confirmation states
    var phoneToDial by remember { mutableStateOf<String?>(null) }
    var dialConfirmWorkerName by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text("Technicians Board", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, letterSpacing = (-0.3).sp)
                Text("Staff rosters, salary allocations and daily status", fontSize = 12.sp, color = grayText, fontWeight = FontWeight.Medium)
            }

            Button(
                onClick = { showAddDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Hire Tech", fontWeight = FontWeight.Bold, maxLines = 1)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.weight(1f)) {
            items(workers) { worker ->
                var isExpanded by remember { mutableStateOf(false) }
                val attendanceRecords by repository.attendance.collectAsState()
                val workerAttendance = remember(attendanceRecords, worker.id) {
                    attendanceRecords.filter { it.workerId == worker.id }
                }
                
                val sdfDate = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
                val sdfTime = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
                val todayDate = sdfDate.format(Date())
                
                val todayRecord = remember(workerAttendance, todayDate) {
                    workerAttendance.find { it.date == todayDate }
                }
                val isTodayClockedIn = todayRecord != null && todayRecord.isClockedIn
                val isTodayCompleted = todayRecord != null && !todayRecord.isClockedIn

                WorkerGlassCard(
                    worker = worker,
                    accentColor = accentColor,
                    isLight = isLight,
                    isTodayClockedIn = isTodayClockedIn,
                    isTodayCompleted = isTodayCompleted,
                    isExpanded = isExpanded,
                    onExpandToggle = { isExpanded = !isExpanded },
                    onPhoneClick = {
                        dialConfirmWorkerName = worker.name
                        phoneToDial = worker.phone
                    },
                    onClockActionClick = {
                        val timeNow = sdfTime.format(Date())
                        if (isTodayClockedIn) {
                            selectedWorkerForDeparture = worker
                            departureRecordToUpdate = todayRecord
                            departureReasonText = "Duty Completed"
                            showEarlyDepartureDialog = true
                        } else if (!isTodayCompleted) {
                            val rec = AttendanceRecord(
                                workerId = worker.id,
                                workerName = worker.name,
                                date = todayDate,
                                clockInTime = timeNow,
                                clockOutTime = "",
                                isClockedIn = true
                            )
                            repository.addAttendanceRecord(rec)
                            repository.updateWorker(worker.copy(status = WorkerStatus.WORKING))
                            Toast.makeText(context, "${worker.name} checked in at $timeNow!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onViewCalendarClick = { selectedWorkerForCalendar = worker }
                )
            }
        }
    }

    // Dialing confirmation dialog
    if (phoneToDial != null) {
        AlertDialog(
            onDismissRequest = { phoneToDial = null },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Call Tech Staff?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Do you want to open the phone dialer to call $dialConfirmWorkerName at ${formatPhoneNumber(phoneToDial ?: "")}?",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val dialIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                data = android.net.Uri.parse("tel:${phoneToDial}")
                            }
                            context.startActivity(dialIntent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Dialer unavailable: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                        }
                        phoneToDial = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black)
                ) {
                    Text("Call Now", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { phoneToDial = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showEarlyDepartureDialog) {
        val worker = selectedWorkerForDeparture
        val record = departureRecordToUpdate
        val sdfTime = remember { SimpleDateFormat("hh:mm a", Locale.getDefault()) }
        AlertDialog(
            onDismissRequest = { showEarlyDepartureDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.DirectionsRun, contentDescription = null, tint = NeonYellow, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Early Sign-out / Departure", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Registering early checkout for ${worker?.name ?: "Technician"}.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    
                    OutlinedTextField(
                        value = departureReasonText,
                        onValueChange = { departureReasonText = it },
                        label = { Text("Reason for early checkout / leave") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                    )
                    
                    Text("Quick Presets:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        listOf("Aaj jldi chla gya", "Achanak Kaam", "Sick Leave").forEach { preset ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .border(1.dp, if (departureReasonText == preset) accentColor else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { departureReasonText = preset }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(preset, fontSize = 11.sp, color = if (departureReasonText == preset) accentColor else Color.LightGray)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val timeNow = sdfTime.format(Date())
                        record?.let {
                            repository.updateAttendanceRecord(it.copy(
                                clockOutTime = "$timeNow (Early Leave: $departureReasonText)",
                                isClockedIn = false
                            ))
                        }
                        worker?.let {
                            repository.updateWorker(it.copy(status = WorkerStatus.IDLE))
                        }
                        Toast.makeText(context, "${worker?.name} signed out early: \"$departureReasonText\"!", Toast.LENGTH_SHORT).show()
                        showEarlyDepartureDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonYellow, contentColor = Color.Black)
                ) {
                    Text("Confirm Departure", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEarlyDepartureDialog = false }) {
                    Text("Cancel", color = Color.White)
                }
            }
        )
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Hire New Electrician", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Staff Full Name") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Contact Number (10 digits)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                    )
                    OutlinedTextField(
                        value = salary,
                        onValueChange = { salary = it },
                        label = { Text("Monthly Salary (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanedPhone = cleanRawPhone(phone)
                        if (name.isBlank()) {
                            Toast.makeText(context, "Please enter staff name!", Toast.LENGTH_SHORT).show()
                        } else if (cleanedPhone.length != 10) {
                            Toast.makeText(context, "Please enter a valid 10-digit phone number!", Toast.LENGTH_SHORT).show()
                        } else {
                            repository.addWorker(
                                Worker(
                                    name = name,
                                    phone = cleanedPhone,
                                    salary = salary.toDoubleOrNull() ?: 15000.0,
                                    designation = designation
                                )
                            )
                            name = ""
                            phone = ""
                            salary = ""
                            showAddDialog = false
                            Toast.makeText(context, "New staff registered successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black)
                ) {
                    Text("Hire", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (selectedWorkerForCalendar != null) {
        val worker = selectedWorkerForCalendar!!
        val attendanceRecords by repository.attendance.collectAsState()
        val workerAttendance = remember(attendanceRecords, worker.id) {
            attendanceRecords.filter { it.workerId == worker.id }
        }
        androidx.compose.ui.window.Dialog(onDismissRequest = { selectedWorkerForCalendar = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${worker.name}'s Attendance",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = worker.designation,
                                fontSize = 11.sp,
                                color = accentColor
                            )
                        }
                        IconButton(onClick = { selectedWorkerForCalendar = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    WorkerAttendanceCalendar(workerAttendance, accentColor)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { selectedWorkerForCalendar = null },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Helper functions to save images to the app's persistent storage so they can be retrieved after app relaunch.
private fun saveImageToPersistentStorage(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val fileName = "invoice_gallery_${System.currentTimeMillis()}.jpg"
        val destFile = java.io.File(context.filesDir, fileName)
        context.contentResolver.openInputStream(uri)?.use { input ->
            java.io.FileOutputStream(destFile).use { output ->
                input.copyTo(output)
            }
        }
        android.net.Uri.fromFile(destFile).toString()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

private fun saveBitmapToPersistentStorage(context: android.content.Context, bitmap: android.graphics.Bitmap): String? {
    return try {
        val fileName = "invoice_camera_${System.currentTimeMillis()}.jpg"
        val destFile = java.io.File(context.filesDir, fileName)
        java.io.FileOutputStream(destFile).use { out ->
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
            out.flush()
        }
        android.net.Uri.fromFile(destFile).toString()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
fun InventoryGlassCard(
    item: InventoryItem,
    accentColor: Color,
    isLight: Boolean
) {
    var isPressed by remember { mutableStateOf(false) }
    val cardScale by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1.0f,
        animationSpec = tween(durationMillis = 140, easing = FastOutSlowInEasing),
        label = "inventoryCardScale"
    )

    val isLowStock = item.stockLevel <= item.minAlertLevel
    val isNearLowStock = item.stockLevel <= item.minAlertLevel + 10

    val stockStatusColor = when {
        isLowStock -> NeonRed
        isNearLowStock -> NeonYellow
        else -> NeonGreen
    }

    val cardGradient = if (isLight) {
        Brush.linearGradient(
            colors = listOf(
                Color.White.copy(alpha = 0.96f),
                Color(0xFFF8FAFC).copy(alpha = 0.92f),
                Color(0xFFEFF6FF).copy(alpha = 0.85f)
            ),
            start = Offset(0f, 0f),
            end = Offset(400f, 400f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF0A182E).copy(alpha = 0.94f),
                Color(0xFF092535).copy(alpha = 0.92f),
                Color(0xFF063B47).copy(alpha = 0.88f)
            ),
            start = Offset(0f, 0f),
            end = Offset(400f, 400f)
        )
    }

    val glassBorderGlow = if (isLight) {
        Brush.linearGradient(
            colors = listOf(
                accentColor.copy(alpha = 0.40f),
                Color(0xFF38BDF8).copy(alpha = 0.30f),
                Color(0xFFE2E8F0)
            ),
            start = Offset(0f, 0f),
            end = Offset(300f, 300f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF38BDF8).copy(alpha = 0.45f),
                Color(0xFF14B8A6).copy(alpha = 0.35f),
                Color(0xFF0284C7).copy(alpha = 0.20f),
                Color.White.copy(alpha = 0.12f)
            ),
            start = Offset(0f, 0f),
            end = Offset(300f, 300f)
        )
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isLight) 2.5.dp else 5.dp,
            pressedElevation = 1.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale)
            .border(1.2.dp, glassBorderGlow, RoundedCornerShape(16.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(cardGradient)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(accentColor.copy(alpha = if (isLight) 0.15f else 0.25f), Color(0xFF0284C7).copy(alpha = 0.2f))
                                )
                            )
                            .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Inventory2,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = item.name,
                            fontWeight = FontWeight.Bold,
                            color = if (isLight) Color(0xFF0F172A) else Color.White,
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Category: ${item.category} • Supplier: ${item.supplierName}",
                            fontSize = 11.sp,
                            color = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Est. Sale Cost: ₹${String.format("%,.2f", item.price)}",
                            fontSize = 12.sp,
                            color = if (isLight) Color(0xFF0284C7) else accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(stockStatusColor.copy(alpha = if (isLight) 0.14f else 0.22f))
                            .border(0.8.dp, stockStatusColor.copy(alpha = 0.55f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(stockStatusColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = "${item.stockLevel} units",
                                color = if (isLight && stockStatusColor == NeonYellow) Color(0xFFB45309) else stockStatusColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp
                            )
                        }
                    }
                    if (isLowStock) {
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "LOW STOCK",
                            color = NeonRed,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

// --- MODULE 5: INVENTORY SCREEN WITH VOICE TRANSLATION & SCANNER ---
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun InventoryScreen(repository: ReLectRepository, accentColor: Color) {
    val inventory by repository.inventory.collectAsState()
    val purchases by repository.purchases.collectAsState()
    val bills by repository.bills.collectAsState()

    val isLight = MaterialTheme.colorScheme.surface == Color.White
    val textColor = if (isLight) Color(0xFF111827) else Color.White
    val grayText = if (isLight) Color(0xFF4B5563) else Color.Gray
    val lightGrayText = if (isLight) Color(0xFF374151) else Color.LightGray

    var activeTab by remember { mutableStateOf(0) } // 0 = Catalog, 1 = Purchase History, 2 = Bills Scanner
    var searchItem by remember { mutableStateOf("") }
    var showQuickAddForm by remember { mutableStateOf(false) }

    // Voice Confirmation Dialog states
    var showVoiceConfirmDialog by remember { mutableStateOf(false) }
    var voiceConfirmInputText by remember { mutableStateOf("") }
    var parsedVoiceResult by remember { mutableStateOf<ReLectRepository.ParsedVoiceData?>(null) }

    // Quick Purchase Form states
    var pItemName by remember { mutableStateOf("") }
    var pQty by remember { mutableStateOf("") }
    var pCost by remember { mutableStateOf("") }
    var pSupplier by remember { mutableStateOf("") }

    // Voice Console active listening indicator
    var isListeningVoice by remember { mutableStateOf(false) }
    var voiceInputText by remember { mutableStateOf("") }
    var isProcessingVoice by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val context = LocalContext.current

    var scannedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var scannedPhotoUri by remember { mutableStateOf<String?>(null) }
    var isProcessingImage by remember { mutableStateOf(false) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: android.graphics.Bitmap? ->
        if (bitmap != null) {
            scannedBitmap = bitmap
            isProcessingImage = true
            scope.launch {
                try {
                    val visionResult = GeminiClient.analyzeVisualObjectOrInvoice(bitmap)
                    pItemName = visionResult.title
                    pQty = visionResult.quantity.toString()
                    pCost = if (visionResult.estimatedUnitPrice > 0.0) visionResult.estimatedUnitPrice.toString() else visionResult.estimatedTotalPrice.toString()
                    pSupplier = visionResult.brand
                    
                    // Save to persistent storage
                    val savedUri = saveBitmapToPersistentStorage(context, bitmap)
                    scannedPhotoUri = savedUri
                    
                    showQuickAddForm = true
                    val successMsg = if (visionResult.isInvoice) {
                        "✓ Invoice & Bill Details Extracted Successfully!"
                    } else {
                        "✓ AI Identified: ${visionResult.title} (${visionResult.brand})"
                    }
                    Toast.makeText(context, successMsg, Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error scanning: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessingImage = false
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(context, "Camera permission is required to identify objects or scan bills!", Toast.LENGTH_LONG).show()
        }
    }

    val checkAndLaunchCamera = {
        val cameraPermission = android.Manifest.permission.CAMERA
        val hasPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            cameraPermission
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            cameraLauncher.launch(null)
        } else {
            permissionLauncher.launch(cameraPermission)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            isProcessingImage = true
            scope.launch {
                try {
                    val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        val source = android.graphics.ImageDecoder.createSource(context.contentResolver, uri)
                        android.graphics.ImageDecoder.decodeBitmap(source)
                    } else {
                        @Suppress("DEPRECATION")
                        android.provider.MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    }
                    
                    scannedBitmap = bitmap
                    val visionResult = GeminiClient.analyzeVisualObjectOrInvoice(bitmap)
                    pItemName = visionResult.title
                    pQty = visionResult.quantity.toString()
                    pCost = if (visionResult.estimatedUnitPrice > 0.0) visionResult.estimatedUnitPrice.toString() else visionResult.estimatedTotalPrice.toString()
                    pSupplier = visionResult.brand
                    
                    // Save to persistent storage
                    val savedUri = saveImageToPersistentStorage(context, uri)
                    scannedPhotoUri = savedUri
                    
                    showQuickAddForm = true
                    val successMsg = if (visionResult.isInvoice) {
                        "✓ Invoice & Bill Details Extracted Successfully!"
                    } else {
                        "✓ AI Identified: ${visionResult.title} (${visionResult.brand})"
                    }
                    Toast.makeText(context, successMsg, Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Error parsing invoice: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessingImage = false
                }
            }
        }
    }

    val voiceLauncher = rememberSpeechRecognizerLauncher(
        onResult = { text ->
            voiceInputText = text
            // Automatically parse the captured real-time speech using Gemini
            if (text.isNotBlank()) {
                isProcessingVoice = true
                scope.launch {
                    try {
                        val parsed = GeminiClient.parseVoiceInput(text)
                        voiceConfirmInputText = text
                        parsedVoiceResult = parsed
                        showVoiceConfirmDialog = true
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isProcessingVoice = false
                        voiceInputText = ""
                    }
                }
            }
        },
        onStateChange = { listening, error ->
            isListeningVoice = listening
            if (error != null) {
                Toast.makeText(context, "Voice command error: $error", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(
                    "Stock & Parts Inventory",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textColor,
                    letterSpacing = (-0.3).sp
                )
                Text(
                    "Track cables, switches, ceiling fans & appliances",
                    fontSize = 12.sp,
                    color = grayText,
                    fontWeight = FontWeight.Medium
                )
            }

            Button(
                onClick = { showQuickAddForm = true },
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
            ) {
                Icon(Icons.Default.FlashOn, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Quick Buy", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
        }

        // Voice Translation Assistant Bar with Dark/Light Mode Glass
        val triggerParsing: (String) -> Unit = { text ->
            if (text.isNotBlank()) {
                isProcessingVoice = true
                scope.launch {
                    try {
                        val parsed = GeminiClient.parseVoiceInput(text)
                        voiceConfirmInputText = text
                        parsedVoiceResult = parsed
                        showVoiceConfirmDialog = true
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isProcessingVoice = false
                        voiceInputText = ""
                    }
                }
            }
        }

        // Modern AI Voice Stock Assistant Card
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isLight) Color.White else Color(0xFF0F172A)
            ),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (isLight) Color(0xFFE2E8F0) else Color(0xFF1E293B)),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isLight) 1.5.dp else 3.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                // Header row inside card
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        Text(
                            "AI Voice Stock Logger",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isLight) Color(0xFFF1F5F9) else Color(0xFF1E293B))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                "Hindi • English • Punjabi",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = grayText
                            )
                        }

                        IconButton(
                            onClick = {
                                val samplePhrases = listOf(
                                    "Orient Ceiling Fan purchased 5 pieces from Orient supplier for 10500",
                                    "ਪੰਜ ਸੌ ਇਕਵੰਜਾ ਰੁਪਏ ਦੇ ਪੰਜ ਸਵਿੱਚ ਐਂਕਰ ਬ੍ਰਾਂਡ ਦੇ ਖਰੀਦੇ ਨੇ",
                                    "दस एल्युमिनियम वायर खरीदे हैं पचास रुपये के हिसाब से"
                                )
                                val selectedPhrase = samplePhrases.random()
                                voiceInputText = selectedPhrase
                                triggerParsing(selectedPhrase)
                            },
                            enabled = !isProcessingVoice && !isListeningVoice,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Icons.Default.Casino,
                                contentDescription = "Simulate Phrase",
                                tint = grayText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Input Controls: Mic Action Button + Text Input Field
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val micActive = isListeningVoice
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(CircleShape)
                            .background(
                                if (micActive) NeonRed
                                else if (isLight) accentColor.copy(alpha = 0.12f)
                                else accentColor.copy(alpha = 0.20f)
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (micActive) NeonRed else accentColor.copy(alpha = 0.45f),
                                shape = CircleShape
                            )
                            .clickable(enabled = !isProcessingVoice) {
                                if (isListeningVoice) {
                                    voiceLauncher.stop()
                                } else {
                                    voiceLauncher()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (isProcessingVoice) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = accentColor,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (micActive) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = if (micActive) "Stop Recording" else "Speak to Add Stock",
                                tint = if (micActive) Color.White else accentColor,
                                modifier = Modifier
                                    .size(22.dp)
                                    .then(if (micActive) Modifier.pulsingAnimation() else Modifier)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = voiceInputText,
                        onValueChange = { voiceInputText = it },
                        placeholder = {
                            Text(
                                if (isListeningVoice) "Listening... speak item & quantity"
                                else if (isProcessingVoice) "Gemini NLP parsing entry..."
                                else "Speak or type stock entry...",
                                fontSize = 12.5.sp,
                                color = grayText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        enabled = !isProcessingVoice,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (isLight) Color(0xFFF8FAFC) else Color(0xFF070B14),
                            unfocusedContainerColor = if (isLight) Color(0xFFF8FAFC) else Color(0xFF070B14),
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = if (isLight) Color(0xFFE2E8F0) else Color(0xFF334155),
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        ),
                        trailingIcon = {
                            if (voiceInputText.isNotBlank() && !isProcessingVoice) {
                                IconButton(
                                    onClick = { triggerParsing(voiceInputText) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "Parse Entry",
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }

        // Segmented Pill Sub-Tabs Switcher (No awkward 2-line wraps)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(if (isLight) Color(0xFFE2E8F0).copy(alpha = 0.7f) else Color(0xFF1E293B))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf(
                "Catalog",
                "Purchases",
                "Bills Scanner"
            ).forEachIndexed { index, title ->
                val isSelected = activeTab == index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (isSelected) {
                                if (isLight) Color.White else accentColor
                            } else Color.Transparent
                        )
                        .clickable { activeTab = index }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) {
                            if (isLight) accentColor else Color.White
                        } else grayText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        when (activeTab) {
            0 -> {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    // Stock Catalog list with refined search
                    OutlinedTextField(
                        value = searchItem,
                        onValueChange = { searchItem = it },
                        placeholder = { Text("Search wire, switch, mcb, pump...", fontSize = 13.sp, color = grayText) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search",
                                tint = grayText,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (searchItem.isNotEmpty()) {
                                IconButton(onClick = { searchItem = "" }, modifier = Modifier.size(30.dp)) {
                                    Icon(
                                        Icons.Default.Clear,
                                        contentDescription = "Clear",
                                        tint = grayText,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (isLight) Color.White else Color(0xFF0F172A),
                            unfocusedContainerColor = if (isLight) Color.White else Color(0xFF0F172A),
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = if (isLight) Color(0xFFE2E8F0) else Color(0xFF1E293B),
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor
                        )
                    )

                    val catalogFiltered = inventory.filter { it.name.contains(searchItem, true) }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                        items(catalogFiltered) { item ->
                            InventoryGlassCard(
                                item = item,
                                accentColor = accentColor,
                                isLight = isLight
                            )
                        }
                    }
                }
            }

            1 -> {
                Column(modifier = Modifier.weight(1f)) {
                    // Purchase list with exports
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { Toast.makeText(context, "Inventory CSV Report Downloaded Successfully", Toast.LENGTH_SHORT).show() }) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp), tint = accentColor)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export CSV", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        TextButton(onClick = { exportAndShareInventoryPdf(context, inventory) }) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(14.dp), tint = accentColor)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export PDF", color = accentColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                        items(purchases) { record ->
                            var photoExpanded by remember { mutableStateOf(false) }
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isLight) Color.White else Color(0xFF0F172A)
                                ),
                                shape = RoundedCornerShape(14.dp),
                                border = BorderStroke(1.dp, if (isLight) Color(0xFFE2E8F0) else Color(0xFF1E293B)),
                                elevation = CardDefaults.cardElevation(defaultElevation = if (isLight) 1.5.dp else 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable(enabled = !record.photoUri.isNullOrBlank()) {
                                        photoExpanded = !photoExpanded
                                    }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                record.itemName,
                                                fontWeight = FontWeight.Bold,
                                                color = textColor,
                                                fontSize = 14.5.sp
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                "Qty: ${record.quantity} units • Supplier: ${record.supplierName}",
                                                fontSize = 11.5.sp,
                                                color = grayText
                                            )
                                            if (!record.photoUri.isNullOrBlank()) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = Icons.Default.PhotoCamera,
                                                        contentDescription = null,
                                                        tint = accentColor,
                                                        modifier = Modifier.size(12.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(4.dp))
                                                    Text(
                                                        text = if (photoExpanded) "Tap to hide invoice photo" else "Tap to view invoice photo",
                                                        fontSize = 10.5.sp,
                                                        color = accentColor,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                "₹${String.format("%,.2f", record.cost)}",
                                                color = if (isLight) Color(0xFF16A34A) else NeonGreen,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 14.sp
                                            )
                                            Text(record.date, fontSize = 10.sp, color = grayText)
                                        }
                                    }

                                    if (photoExpanded && !record.photoUri.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        AsyncImage(
                                            model = record.photoUri,
                                            contentDescription = "Invoice Photo",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(1.dp, if (isLight) Color(0xFFE2E8F0) else Color.DarkGray, RoundedCornerShape(10.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            2 -> {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLight) Color.White else Color(0xFF0F172A)
                    ),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, if (isLight) Color(0xFFE2E8F0) else Color(0xFF1E293B)),
                    elevation = CardDefaults.cardElevation(defaultElevation = if (isLight) 1.5.dp else 2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isProcessingImage) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Black.copy(alpha = 0.3f))
                                    .border(1.dp, accentColor, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    CircularProgressIndicator(color = accentColor)
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("AI Scan Processing...", fontWeight = FontWeight.Bold, color = textColor)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Gemini is reading bill details...", fontSize = 11.sp, color = grayText)
                                }
                            }
                        } else if (scannedBitmap != null) {
                            val bmp = scannedBitmap
                            if (bmp != null) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(1.dp, if (isLight) Color(0xFFE2E8F0) else Color(0xFF1E293B), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "Scanned Bill Preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(Color.Black.copy(alpha = 0.4f)),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = { checkAndLaunchCamera() },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = if (isLight) Color(0xFFE2E8F0) else Color.DarkGray,
                                                    contentColor = if (isLight) Color(0xFF0F172A) else Color.White
                                                ),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Retake", fontSize = 11.sp)
                                            }
                                            Button(
                                                onClick = {
                                                    isProcessingImage = true
                                                    scope.launch {
                                                        try {
                                                            val parsed = GeminiClient.parseInvoiceImage(bmp)
                                                            pItemName = parsed.item
                                                            pQty = parsed.quantity.toString()
                                                            pCost = parsed.amount.toString()
                                                            pSupplier = parsed.supplier
                                                            showQuickAddForm = true
                                                            Toast.makeText(context, "Invoice Parsed Successfully!", Toast.LENGTH_SHORT).show()
                                                        } catch (e: Exception) {
                                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                                        } finally {
                                                            isProcessingImage = false
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                                                shape = RoundedCornerShape(8.dp)
                                            ) {
                                                Text("Analyze Again", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = accentColor, modifier = Modifier.size(36.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            "Smart Supplier Invoice Scanner",
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Scan paper bills to automatically parse items, rates, and suppliers into ERP records in real-time.",
                            fontSize = 12.sp,
                            color = grayText,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = { checkAndLaunchCamera() },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Take Photo", fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { galleryLauncher.launch("image/*") },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isLight) Color(0xFFF1F5F9) else Color(0xFF1E293B),
                                    contentColor = if (isLight) Color(0xFF0F172A) else Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Upload Bill", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showVoiceConfirmDialog) {
        val parsed = parsedVoiceResult
        AlertDialog(
            onDismissRequest = { showVoiceConfirmDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Hearing, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Confirm Extracted Ledger Entry", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("You Spoke:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .border(1.dp, BorderSilver.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text("\"$voiceConfirmInputText\"", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    }

                    Text("Extracted ERP Structured Details:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

                    if (parsed != null) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isLight) Color(0xFFF1F5F9) else Color(0xFF0F172A)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (isLight) Color(0xFFE2E8F0) else Color(0xFF1E293B)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Item:", fontSize = 12.sp, color = grayText)
                                    Text(parsed.item, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Qty:", fontSize = 12.sp, color = grayText)
                                    Text("${parsed.quantity} Pcs", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Rate/Cost:", fontSize = 12.sp, color = grayText)
                                    Text("₹%,.2f".format(parsed.amount.toDouble()), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (isLight) Color(0xFF16A34A) else NeonGreen)
                                }
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text("Supplier:", fontSize = 12.sp, color = grayText)
                                    Text(parsed.supplier.ifBlank { "Local Wholesaler" }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                                }
                            }
                        }
                    }
                    
                    Text("Select 'Save Instantly' to save directly to system records, or 'Edit Fields' to open the detailed editor form first.", fontSize = 11.sp, color = grayText)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (parsed != null && parsed.item.isNotBlank()) {
                            repository.addPurchaseRecord(
                                PurchaseRecord(
                                    itemName = parsed.item,
                                    quantity = parsed.quantity,
                                    supplierName = parsed.supplier.ifBlank { "Local Wholesaler" },
                                    cost = parsed.amount.toDouble()
                                )
                            )
                            Toast.makeText(context, "Direct Ledger Purchase Saved: ${parsed.item}!", Toast.LENGTH_SHORT).show()
                        }
                        showVoiceConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isLight) Color(0xFF16A34A) else NeonGreen,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save Instantly", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    TextButton(
                        onClick = {
                            if (parsed != null) {
                                pItemName = parsed.item
                                pQty = parsed.quantity.toString()
                                pCost = parsed.amount.toString()
                                pSupplier = parsed.supplier
                                showQuickAddForm = true
                            }
                            showVoiceConfirmDialog = false
                        }
                    ) {
                        Text("Edit Fields", color = accentColor, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    TextButton(onClick = { showVoiceConfirmDialog = false }) {
                        Text("Cancel", color = grayText)
                    }
                }
            }
        )
    }

    if (showQuickAddForm) {
        AlertDialog(
            onDismissRequest = { showQuickAddForm = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Quick Stock Purchase Ledger Entry", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (scannedBitmap != null) {
                        Text("Scanned Bill Photo:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = grayText)
                        Image(
                            bitmap = scannedBitmap!!.asImageBitmap(),
                            contentDescription = "Attached Invoice Preview",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, accentColor, RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    OutlinedTextField(
                        value = pItemName,
                        onValueChange = { pItemName = it },
                        label = { Text("Item Name") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                    )
                    OutlinedTextField(
                        value = pQty,
                        onValueChange = { pQty = it },
                        label = { Text("Purchased Quantity") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                    )
                    OutlinedTextField(
                        value = pCost,
                        onValueChange = { pCost = it },
                        label = { Text("Total Bill Cost (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                    )
                    OutlinedTextField(
                        value = pSupplier,
                        onValueChange = { pSupplier = it },
                        label = { Text("Supplier Name") },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val qtyVal = pQty.toIntOrNull() ?: 1
                        val costVal = pCost.toDoubleOrNull() ?: 500.0
                        if (pItemName.isNotBlank()) {
                            repository.addPurchaseRecord(
                                PurchaseRecord(
                                    itemName = pItemName,
                                    quantity = qtyVal,
                                    supplierName = pSupplier.ifBlank { "Local Wholesaler" },
                                    cost = costVal,
                                    photoUri = scannedPhotoUri
                                )
                            )
                            pItemName = ""
                            pQty = ""
                            pCost = ""
                            pSupplier = ""
                            scannedPhotoUri = null
                            scannedBitmap = null
                            showQuickAddForm = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Add Ledger Record", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickAddForm = false }) {
                    Text("Cancel", color = grayText)
                }
            }
        )
    }
}

// Helper pulsing animation for mic
@Composable
fun Modifier.pulsingAnimation(): Modifier {
    val transition = rememberInfiniteTransition(label = "pulsing")
    val alpha by transition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1000), repeatMode = RepeatMode.Reverse),
        label = "alpha"
    )
    return this.alpha(alpha)
}

class SpeechRecognitionHandle(
    val start: () -> Unit,
    val stop: () -> Unit,
    val toggle: () -> Unit = {}
) : () -> Unit {
    override fun invoke() = toggle()
}

@Composable
fun rememberSpeechRecognizerLauncher(
    onResult: (String) -> Unit,
    onStateChange: (listening: Boolean, errorMsg: String?) -> Unit
): SpeechRecognitionHandle {
    val context = androidx.compose.ui.platform.LocalContext.current
    var speechRecognizer by remember { mutableStateOf<android.speech.SpeechRecognizer?>(null) }
    var isCurrentlyListening by remember { mutableStateOf(false) }
    
    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isCurrentlyListening = true
            onStateChange(true, null)
            val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                val defaultLang = java.util.Locale.getDefault().toLanguageTag()
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, if (defaultLang.startsWith("hi", ignoreCase = true) || defaultLang.startsWith("pa", ignoreCase = true)) defaultLang else "hi-IN")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN", "pa-IN", "en-US", "hi", "pa"))
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
                putExtra(android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000L)
                putExtra(android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
            }
            try {
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                isCurrentlyListening = false
                onStateChange(false, e.localizedMessage)
            }
        } else {
            isCurrentlyListening = false
            onStateChange(false, "Microphone permission denied")
            android.widget.Toast.makeText(context, "Microphone permission is required for voice commands!", android.widget.Toast.LENGTH_LONG).show()
        }
    }

    DisposableEffect(Unit) {
        val isAvailable = android.speech.SpeechRecognizer.isRecognitionAvailable(context)
        if (isAvailable) {
            val sr = android.speech.SpeechRecognizer.createSpeechRecognizer(context)
            sr.setRecognitionListener(object : android.speech.RecognitionListener {
                override fun onReadyForSpeech(params: android.os.Bundle?) {
                    isCurrentlyListening = true
                }
                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {
                    // Do not prematurely dismiss before results are processed
                }
                override fun onError(error: Int) {
                    isCurrentlyListening = false
                    val message = when (error) {
                        android.speech.SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                        android.speech.SpeechRecognizer.ERROR_CLIENT -> null
                        android.speech.SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                        android.speech.SpeechRecognizer.ERROR_NETWORK -> "Network error"
                        android.speech.SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                        android.speech.SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected. Please speak closer to mic."
                        android.speech.SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service busy"
                        android.speech.SpeechRecognizer.ERROR_SERVER -> "Server error"
                        android.speech.SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech heard"
                        else -> "Speech recognition error ($error)"
                    }
                    if (message != null) {
                        onStateChange(false, message)
                    } else {
                        onStateChange(false, null)
                    }
                }
                override fun onResults(results: android.os.Bundle?) {
                    isCurrentlyListening = false
                    val matches = results?.getStringArrayList(android.speech.SpeechRecognizer.RESULTS_RECOGNITION)
                    val text = matches?.firstOrNull { it.isNotBlank() } ?: ""
                    if (text.isNotBlank()) {
                        onResult(text)
                    }
                    onStateChange(false, null)
                }
                override fun onPartialResults(partialResults: android.os.Bundle?) {}
                override fun onEvent(eventType: Int, params: android.os.Bundle?) {}
            })
            speechRecognizer = sr
        }
        onDispose {
            try {
                speechRecognizer?.cancel()
                speechRecognizer?.destroy()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val stopAction: () -> Unit = {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        isCurrentlyListening = false
        onStateChange(false, null)
    }

    val startAction: () -> Unit = {
        val hasMicPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (hasMicPermission) {
            isCurrentlyListening = true
            onStateChange(true, null)
            val intent = android.content.Intent(android.speech.RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_MODEL, android.speech.RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("hi-IN", "en-IN", "pa-IN", "en-US"))
                putExtra(android.speech.RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                putExtra(android.speech.RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(android.speech.RecognizerIntent.EXTRA_MAX_RESULTS, 5)
                putExtra(android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500L)
                putExtra(android.speech.RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1200L)
            }
            try {
                speechRecognizer?.startListening(intent)
            } catch (e: Exception) {
                isCurrentlyListening = false
                onStateChange(false, e.localizedMessage)
            }
        } else {
            permissionLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
        }
    }

    val toggleAction: () -> Unit = {
        if (isCurrentlyListening) {
            stopAction()
        } else {
            startAction()
        }
    }

    return remember(speechRecognizer, isCurrentlyListening) {
        SpeechRecognitionHandle(
            start = startAction,
            stop = stopAction,
            toggle = toggleAction
        )
    }
}

@Composable
fun CameraPreviewView(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraProviderFuture = remember { androidx.camera.lifecycle.ProcessCameraProvider.getInstance(context) }

    androidx.compose.ui.viewinterop.AndroidView(
        factory = { ctx ->
            val previewView = androidx.camera.view.PreviewView(ctx).apply {
                scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER
            }
            val executor = androidx.core.content.ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = androidx.camera.core.Preview.Builder().build().apply {
                        setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, executor)
            previewView
        },
        modifier = modifier
    )
}

fun exportAndShareInventoryPdf(context: android.content.Context, items: List<InventoryItem>) {
    val pdfDocument = android.graphics.pdf.PdfDocument()
    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 10f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
    }
    val boldPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 11f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val headerPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        textSize = 18f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    // Title banner
    canvas.drawRect(20f, 20f, 575f, 80f, android.graphics.Paint().apply { color = android.graphics.Color.rgb(11, 25, 44) })
    canvas.drawText("RATTAN ELECTRICALS - STOCK INVENTORY LEDGER", 35f, 55f, headerPaint)

    var y = 110f
    canvas.drawText("Generated on: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}", 35f, y, textPaint)
    y += 20f

    // Table Header
    canvas.drawRect(35f, y, 560f, y + 20f, android.graphics.Paint().apply { color = android.graphics.Color.rgb(230, 230, 230) })
    canvas.drawText("Item Name", 40f, y + 14f, boldPaint)
    canvas.drawText("Category", 280f, y + 14f, boldPaint)
    canvas.drawText("Stock Level", 420f, y + 14f, boldPaint)
    canvas.drawText("Est Price", 490f, y + 14f, boldPaint)
    y += 25f

    items.forEach { item ->
        if (y > 780f) {
            return@forEach
        }
        canvas.drawText(item.name, 40f, y + 12f, textPaint)
        canvas.drawText(item.category, 280f, y + 12f, textPaint)
        val isLow = item.stockLevel <= item.minAlertLevel
        val stockColor = if (isLow) android.graphics.Color.RED else android.graphics.Color.BLACK
        val stockPaint = android.graphics.Paint(textPaint).apply { color = stockColor; typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD) }
        canvas.drawText("${item.stockLevel} units ${if (isLow) "[LOW]" else ""}", 420f, y + 12f, stockPaint)
        canvas.drawText(String.format("₹%.2f", item.price), 490f, y + 12f, textPaint)
        y += 18f
    }

    pdfDocument.finishPage(page)

    val filename = "RE_Inventory_Stock_${System.currentTimeMillis()}.pdf"
    var outputStream: java.io.OutputStream? = null
    var uri: android.net.Uri? = null
    try {
        val resolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
        }
        val contentUri = if (android.os.Build.VERSION.SDK_INT >= 29) {
            android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            android.net.Uri.parse("content://media/external/file")
        }
        uri = resolver.insert(contentUri, contentValues)
        if (uri != null) {
            outputStream = resolver.openOutputStream(uri)
            if (outputStream != null) {
                pdfDocument.writeTo(outputStream)
                android.widget.Toast.makeText(context, "PDF Report Saved to Downloads!", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "PDF report failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
    } finally {
        try { outputStream?.close() } catch (ex: java.lang.Exception) {}
        pdfDocument.close()
    }

    if (uri != null) {
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Rattan Electricals Inventory Ledger")
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Inventory PDF via:"))
    }
}

fun sendBillToCustomerWhatsApp(context: android.content.Context, bill: Bill, onDispatched: () -> Unit = {}) {
    val cleanPhone = bill.customerPhone.replace(Regex("[^0-9]"), "")
    val phoneWithCountry = if (cleanPhone.length == 10) "91$cleanPhone" else cleanPhone

    val itemsList = bill.items.mapIndexed { idx, itm ->
        "• ${itm.name} (x${itm.qty}) - ₹${String.format("%.2f", itm.total)}"
    }.joinToString("\n")

    val message = buildString {
        appendLine("⚡ *${bill.storeName ?: "RATTAN ELECTRICALS"}* ⚡")
        appendLine("*TAX INVOICE / CASH MEMO*")
        appendLine("━━━━━━━━━━━━━━━━━━━━━")
        appendLine("📄 *Invoice No:* ${bill.invoiceNumber ?: "INV-${bill.id.takeLast(6).uppercase()}"}")
        appendLine("📅 *Date:* ${bill.date}")
        appendLine("👤 *Customer:* ${bill.customerName}")
        appendLine("📞 *Phone:* ${bill.customerPhone}")
        appendLine("━━━━━━━━━━━━━━━━━━━━━")
        appendLine("*BILL ITEMS:*")
        if (itemsList.isNotBlank()) {
            appendLine(itemsList)
        } else {
            appendLine("• Service & Repair Labor Only")
        }
        if (bill.laborCharges > 0) {
            appendLine("🔧 *Labor / Service:* ₹${String.format("%.2f", bill.laborCharges)}")
        }
        if (bill.discountAmount > 0) {
            appendLine("🎁 *Discount:* -₹${String.format("%.2f", bill.discountAmount)}")
        }
        if (bill.gstPercent > 0) {
            appendLine("🏛️ *GST (${String.format("%.1f", bill.gstPercent)}%):* Included")
        }
        appendLine("━━━━━━━━━━━━━━━━━━━━━")
        appendLine("💰 *GRAND TOTAL:* ₹${String.format("%.2f", bill.totalAmount)}")
        appendLine("💳 *Payment Status:* ${bill.paymentStatus.uppercase()} (${bill.paymentMethod ?: "Cash"})")
        if (bill.balanceDue > 0) {
            appendLine("⚠️ *Balance Due:* ₹${String.format("%.2f", bill.balanceDue)}")
        }
        appendLine("━━━━━━━━━━━━━━━━━━━━━")
        appendLine("🙏 *Thank you for choosing Rattan Electricals!*")
        appendLine("📞 Helpline: +91 98120 45612 / 01662-234567")
        appendLine("📍 Hisar Road, Barwala, Haryana")
    }

    try {
        val uri = android.net.Uri.parse("https://api.whatsapp.com/send?phone=$phoneWithCountry&text=${android.net.Uri.encode(message)}")
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
        onDispatched()
    } catch (e: Exception) {
        val sendIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, message)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(android.content.Intent.createChooser(sendIntent, "Send Bill via"))
        onDispatched()
    }
}

fun sendBillToCustomerSms(context: android.content.Context, bill: Bill, onDispatched: () -> Unit = {}) {
    val cleanPhone = bill.customerPhone.replace(Regex("[^0-9]"), "")
    val message = "Rattan Electricals Invoice: Total Rs.${String.format("%.2f", bill.totalAmount)}, Status: ${bill.paymentStatus}${if (bill.balanceDue > 0) ", Due: Rs.${String.format("%.2f", bill.balanceDue)}" else ""}. Customer: ${bill.customerName}. Thank you! Helpline: 9812045612"
    try {
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("sms:$cleanPhone")).apply {
            putExtra("sms_body", message)
            addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        onDispatched()
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Could not open SMS: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
    }
}

fun exportAndShareBill(context: android.content.Context, bill: Bill) {
    val pdfDocument = android.graphics.pdf.PdfDocument()
    // Standard A4 size: 595 x 842 points
    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 11f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
    }
    
    val boldPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 11f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    val headerPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(11, 25, 44)
        textSize = 20f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    val accentPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(0, 173, 181) // Beautiful Teal
        textSize = 14f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    var y = 120f

    // Draw Shop Logo Header Banner
    val bannerPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(11, 25, 44)
    }
    canvas.drawRect(20f, 20f, 575f, 90f, bannerPaint)

    headerPaint.color = android.graphics.Color.WHITE
    canvas.drawText("RATTAN ELECTRICALS", 35f, 55f, headerPaint)
    
    val subtitlePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(0, 173, 181)
        textSize = 11f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
    }
    canvas.drawText("Premium Repair Retails, Spares & Enterprise ERP Portal", 35f, 75f, subtitlePaint)

    // Bill details
    canvas.drawText("TAX INVOICE / BILL OF SUPPLY", 35f, y, accentPaint)
    y += 24f
    
    canvas.drawText("Invoice ID: RE-BILL-${System.currentTimeMillis() % 100000}", 35f, y, boldPaint)
    y += 18f
    canvas.drawText("Date: ${bill.date}", 35f, y, textPaint)
    y += 18f
    canvas.drawText("Customer: ${bill.customerName}", 35f, y, textPaint)
    y += 18f
    canvas.drawText("Phone: +91 ${bill.customerPhone}", 35f, y, textPaint)
    y += 30f

    // Draw Table Header
    val tableHeaderPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(240, 240, 240)
    }
    canvas.drawRect(35f, y - 18f, 560f, y + 8f, tableHeaderPaint)
    
    canvas.drawText("Item Name", 45f, y, boldPaint)
    canvas.drawText("Qty", 350f, y, boldPaint)
    canvas.drawText("Price (INR)", 420f, y, boldPaint)
    canvas.drawText("Total (INR)", 490f, y, boldPaint)
    
    y += 24f

    // Draw Items
    bill.items.forEach { item ->
        canvas.drawText(item.name, 45f, y, textPaint)
        canvas.drawText(item.qty.toString(), 350f, y, textPaint)
        canvas.drawText(String.format("%.2f", item.rate), 420f, y, textPaint)
        canvas.drawText(String.format("%.2f", item.total), 490f, y, textPaint)
        y += 22f
    }

    y += 10f
    canvas.drawLine(35f, y, 560f, y, textPaint)
    y += 20f

    // Financial calculations
    val subtotalParts = bill.items.sumOf { item -> item.total }
    val labor = bill.laborCharges
    val grossTotal = subtotalParts + labor
    val discount = bill.discountAmount
    val taxableAmount = maxOf(0.0, grossTotal - discount)
    val gstTax = taxableAmount * (bill.gstPercent / 100.0)
    val grandTotal = bill.totalAmount

    canvas.drawText("Subtotal Parts:", 330f, y, textPaint)
    canvas.drawText(String.format("₹%.2f", subtotalParts), 490f, y, boldPaint)
    y += 18f

    canvas.drawText("Labor / Service Charges:", 330f, y, textPaint)
    canvas.drawText(String.format("₹%.2f", labor), 490f, y, boldPaint)
    y += 18f

    if (discount > 0.0) {
        canvas.drawText("Discount Applied:", 330f, y, textPaint)
        canvas.drawText(String.format("-₹%.2f", discount), 490f, y, boldPaint)
        y += 18f
    }

    if (bill.gstPercent > 0.0) {
        canvas.drawText("Integrated GST (${String.format("%.1f", bill.gstPercent)}%):", 330f, y, textPaint)
        canvas.drawText(String.format("₹%.2f", gstTax), 490f, y, boldPaint)
    } else {
        canvas.drawText("GST (0% - Tax Exempt):", 330f, y, textPaint)
        canvas.drawText("₹0.00", 490f, y, boldPaint)
    }
    y += 24f

    val totalBannerPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(224, 247, 250)
    }
    canvas.drawRect(320f, y - 18f, 560f, y + 8f, totalBannerPaint)
    
    val grandTotalPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(11, 25, 44)
        textSize = 12f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    canvas.drawText("GRAND TOTAL (INR):", 330f, y, grandTotalPaint)
    canvas.drawText(String.format("₹%.2f", grandTotal), 490f, y, grandTotalPaint)
    
    y += 45f
    
    // Terms & Footer
    val footerPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.GRAY
        textSize = 9f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
    }
    canvas.drawText("Terms & Conditions:", 35f, y, boldPaint)
    y += 14f
    canvas.drawText("1. Goods once sold will not be returned or exchanged.", 35f, y, footerPaint)
    y += 12f
    canvas.drawText("2. All disputes are subject to local jurisdiction only.", 35f, y, footerPaint)
    y += 12f
    canvas.drawText("3. Electrical items carry warranty as per manufacturers policy only.", 35f, y, footerPaint)
    
    y += 40f
    val centerFooterPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(11, 25, 44)
        textSize = 11f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    canvas.drawText("Thank you for choosing Rattan Electricals!", 160f, y, centerFooterPaint)

    // Finish the page
    pdfDocument.finishPage(page)

    // Save the PDF document
    val filename = "RE_Invoice_${System.currentTimeMillis()}.pdf"
    var outputStream: java.io.OutputStream? = null
    var uri: android.net.Uri? = null
    
    try {
        val resolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
        }
        val contentUri = if (android.os.Build.VERSION.SDK_INT >= 29) {
            android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            android.net.Uri.parse("content://media/external/file")
        }
        uri = resolver.insert(contentUri, contentValues)
        if (uri != null) {
            outputStream = resolver.openOutputStream(uri)
            if (outputStream != null) {
                pdfDocument.writeTo(outputStream)
                android.widget.Toast.makeText(context, "PDF Invoice Saved to Downloads!", android.widget.Toast.LENGTH_LONG).show()
            }
        }
    } catch (e: Exception) {
        android.widget.Toast.makeText(context, "PDF saving failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
    } finally {
        try { outputStream?.close() } catch (ex: java.lang.Exception) {}
        pdfDocument.close()
    }

    // Share the PDF
    if (uri != null) {
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Rattan Electricals Tax Invoice Ledger")
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Save or Send Invoice PDF via:"))
    }
}

// --- MODULE 6: BILLING INVOICE GENERATOR ---
@Composable
fun BillingScreen(repository: ReLectRepository, accentColor: Color) {
    val customers by repository.customers.collectAsState()
    val inventory by repository.inventory.collectAsState()
    val bills by repository.bills.collectAsState()

    var activeBillingTab by remember { mutableStateOf(0) } // 0: New Invoice, 1: Past Invoices

    // Customer & Contact Details - Full user control (no dialog closing while typing!)
    var customerNameInput by remember { mutableStateOf("") }
    var customerPhoneInput by remember { mutableStateOf("") }
    var showCustomerPickerDialog by remember { mutableStateOf(false) }
    var customerPickerSearch by remember { mutableStateOf("") }

    // Labor Fee Input & Presets
    var laborChargesInput by remember { mutableStateOf("500") }

    // Billing list
    val selectedItems = remember { mutableStateListOf<BillItem>() }

    // Dialogs for Adding Items
    var showInventoryPickerDialog by remember { mutableStateOf(false) }
    var inventoryPickerSearch by remember { mutableStateOf("") }
    var showCustomItemDialog by remember { mutableStateOf(false) }
    var customItemName by remember { mutableStateOf("") }
    var customItemQty by remember { mutableStateOf("1") }
    var customItemRate by remember { mutableStateOf("") }

    // Tax Control (Fixed, Removed, or Variable Tax Rate)
    var isTaxEnabled by remember { mutableStateOf(true) }
    var selectedTaxPreset by remember { mutableStateOf("18") } // "0", "5", "12", "18", "28", "CUSTOM"
    var customTaxRateInput by remember { mutableStateOf("18") }

    // Discount Control (Flat Amount or Percentage)
    var isDiscountEnabled by remember { mutableStateOf(false) }
    var discountMode by remember { mutableStateOf("AMOUNT") } // "AMOUNT" (₹) or "PERCENT" (%)
    var discountInput by remember { mutableStateOf("0") }

    // Invoice Options Action Dialog
    var generatedBillForDialog by remember { mutableStateOf<Bill?>(null) }
    var showInvoiceOptionsDialog by remember { mutableStateOf(false) }

    // Payment & Communication Tracking
    var paymentStatus by remember { mutableStateOf("Paid") } // "Paid", "Partial", "Pending / Due"
    var paymentMethod by remember { mutableStateOf("Cash") } // "Cash", "UPI / GPay", "Card", "Due (Khata)"
    var amountPaidInput by remember { mutableStateOf("") }
    var autoSendWhatsApp by remember { mutableStateOf(true) }

    // Search and Filters for Past Invoices
    var invoiceSearchQuery by remember { mutableStateOf("") }
    var invoiceStatusFilter by remember { mutableStateOf("All") } // "All", "Paid", "Due", "Dispatched", "Pending Dispatch"

    val context = LocalContext.current

    val isLight = MaterialTheme.colorScheme.surface == Color.White
    val textColor = if (isLight) Color(0xFF0F172A) else Color.White
    val grayText = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8)
    val cardBg = if (isLight) Color.White else Color(0xFF0F172A)

    // Financial Calculations
    val subTotalParts = selectedItems.sumOf { it.total }
    val laborVal = laborChargesInput.toDoubleOrNull() ?: 0.0
    val grossTotal = subTotalParts + laborVal

    val discountVal = discountInput.toDoubleOrNull() ?: 0.0
    val effectiveDiscountAmount = if (isDiscountEnabled && discountVal > 0.0) {
        if (discountMode == "PERCENT") {
            (grossTotal * (discountVal / 100.0)).coerceIn(0.0, grossTotal)
        } else {
            discountVal.coerceIn(0.0, grossTotal)
        }
    } else {
        0.0
    }

    val taxableAmount = maxOf(0.0, grossTotal - effectiveDiscountAmount)

    val effectiveGstPercent: Double = if (!isTaxEnabled) {
        0.0
    } else {
        if (selectedTaxPreset == "CUSTOM") {
            customTaxRateInput.toDoubleOrNull() ?: 0.0
        } else {
            selectedTaxPreset.toDoubleOrNull() ?: 0.0
        }
    }

    val gstAmount = taxableAmount * (effectiveGstPercent / 100.0)
    val grandTotal = taxableAmount + gstAmount

    val amountPaidVal = amountPaidInput.toDoubleOrNull() ?: 0.0
    val calculatedBalanceDue = when (paymentStatus) {
        "Paid" -> 0.0
        "Pending / Due" -> grandTotal
        "Partial" -> maxOf(0.0, grandTotal - amountPaidVal)
        else -> 0.0
    }

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Tax Invoice Billing Engine", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, letterSpacing = (-0.3).sp)
                Text("GST compliant invoicing with full tax, discount & customer control", fontSize = 12.sp, color = grayText, fontWeight = FontWeight.Medium)
            }
        }

        // Sub-tabs: New Invoice / Past Invoices
        TabRow(
            selectedTabIndex = activeBillingTab,
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = accentColor,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
        ) {
            Tab(
                selected = activeBillingTab == 0,
                onClick = { activeBillingTab = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AddCircleOutline, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Create Tax Invoice", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
            Tab(
                selected = activeBillingTab == 1,
                onClick = { activeBillingTab = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Invoice History (${bills.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            )
        }

        if (activeBillingTab == 0) {
            // Scrollable invoice creation form (prevents keyboard clipping & screen hiding)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1. Customer & Contact Details Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSilver, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("1. Customer & Labor Details", fontWeight = FontWeight.Bold, color = textColor, fontSize = 13.sp)
                            
                            // Button to pick existing customer from dedicated dialog
                            OutlinedButton(
                                onClick = {
                                    customerPickerSearch = ""
                                    showCustomerPickerDialog = true
                                },
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                border = BorderStroke(1.dp, accentColor)
                            ) {
                                Icon(Icons.Default.PersonSearch, contentDescription = null, tint = accentColor, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Pick Saved Client", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = accentColor)
                            }
                        }

                        // Full Control Text Input: Customer Name (Never closes or interrupts typing!)
                        OutlinedTextField(
                            value = customerNameInput,
                            onValueChange = { customerNameInput = it },
                            placeholder = { Text("Enter full customer name (e.g. Rajesh Kumar)", fontSize = 12.sp) },
                            label = { Text("Customer / Client Name *") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = BorderSilver
                            ),
                            trailingIcon = {
                                if (customerNameInput.isNotBlank()) {
                                    IconButton(onClick = { customerNameInput = "" }) {
                                        Icon(Icons.Default.Close, contentDescription = "Clear", tint = grayText, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        )

                        // Inline non-blocking suggestion chips (does not overlap or cover lower screen)
                        if (customerNameInput.trim().length >= 2) {
                            val matched = customers.filter {
                                it.name.contains(customerNameInput, ignoreCase = true) || it.phone.contains(customerNameInput)
                            }.take(3)
                            if (matched.isNotEmpty() && !matched.any { it.name.equals(customerNameInput, ignoreCase = true) }) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Suggested:", fontSize = 10.sp, color = grayText)
                                    matched.forEach { cust ->
                                        SuggestionChip(
                                            onClick = {
                                                customerNameInput = cust.name
                                                customerPhoneInput = cust.phone
                                            },
                                            label = {
                                                Text("${cust.name} (${cust.phone.takeLast(4)})", fontSize = 10.5.sp, fontWeight = FontWeight.Medium)
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            // Customer Phone
                            OutlinedTextField(
                                value = customerPhoneInput,
                                onValueChange = { customerPhoneInput = it },
                                placeholder = { Text("10-digit mobile number", fontSize = 12.sp) },
                                label = { Text("Phone Number") },
                                modifier = Modifier.weight(1.3f),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = BorderSilver
                                )
                            )

                            // Labor Fee
                            OutlinedTextField(
                                value = laborChargesInput,
                                onValueChange = { laborChargesInput = it },
                                label = { Text("Labor Fee (₹)") },
                                modifier = Modifier.weight(0.9f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    unfocusedBorderColor = BorderSilver
                                )
                            )
                        }

                        // Quick Labor Presets
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Quick Labor:", fontSize = 10.sp, color = grayText)
                            listOf("0", "200", "350", "500", "750", "1000").forEach { preset ->
                                val isSelected = laborChargesInput == preset
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { laborChargesInput = preset },
                                    label = { Text(if (preset == "0") "₹0 (Free)" else "₹$preset", fontSize = 10.sp) },
                                    shape = RoundedCornerShape(8.dp)
                                )
                            }
                        }
                    }
                }

                // 2. Materials & Parts Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSilver, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 6.dp)
                            ) {
                                Text(
                                    text = "2. Materials & Parts",
                                    fontWeight = FontWeight.Bold,
                                    color = textColor,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text("${selectedItems.size} items added", fontSize = 10.sp, color = grayText)
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        customItemName = ""
                                        customItemQty = "1"
                                        customItemRate = ""
                                        showCustomItemDialog = true
                                    },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp),
                                    border = BorderStroke(1.dp, grayText.copy(alpha = 0.4f))
                                ) {
                                    Icon(Icons.Default.EditNote, contentDescription = null, modifier = Modifier.size(13.dp), tint = textColor)
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Custom", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                                }

                                Button(
                                    onClick = {
                                        inventoryPickerSearch = ""
                                        showInventoryPickerDialog = true
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                    modifier = Modifier.height(30.dp)
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(13.dp))
                                    Spacer(modifier = Modifier.width(2.dp))
                                    Text("Add Part", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        if (selectedItems.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.ShoppingCartCheckout, contentDescription = null, tint = grayText, modifier = Modifier.size(32.dp))
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text("No parts added yet.", fontSize = 11.sp, color = grayText, fontWeight = FontWeight.SemiBold)
                                    Text("Tap '+ Add Part' or '+ Custom Item' to add line items.", fontSize = 10.sp, color = grayText)
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                selectedItems.forEachIndexed { index, item ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                            .border(0.5.dp, BorderSilver, RoundedCornerShape(10.dp))
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(item.name, fontWeight = FontWeight.Bold, color = textColor, fontSize = 12.5.sp)
                                            Text("₹${String.format("%.2f", item.rate)} each  •  Total: ₹${String.format("%,.2f", item.total)}", fontSize = 10.5.sp, color = grayText)
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            // Decrement button
                                            IconButton(
                                                onClick = {
                                                    if (item.qty > 1) {
                                                        selectedItems[index] = item.copy(qty = item.qty - 1, total = (item.qty - 1) * item.rate)
                                                    } else {
                                                        selectedItems.removeAt(index)
                                                    }
                                                },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = textColor, modifier = Modifier.size(14.dp))
                                            }

                                            Text("${item.qty}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)

                                            // Increment button
                                            IconButton(
                                                onClick = {
                                                    selectedItems[index] = item.copy(qty = item.qty + 1, total = (item.qty + 1) * item.rate)
                                                },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = textColor, modifier = Modifier.size(14.dp))
                                            }

                                            // Delete item
                                            IconButton(
                                                onClick = { selectedItems.removeAt(index) },
                                                modifier = Modifier.size(26.dp)
                                            ) {
                                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = NeonRed, modifier = Modifier.size(15.dp))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. Tax & GST Controls Card (User has 100% control to fix, remove, or variable tax)
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSilver, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("3. Tax & GST Controls", fontWeight = FontWeight.Bold, color = textColor, fontSize = 13.sp)
                                Text(
                                    if (isTaxEnabled) "GST Rate: ${String.format("%.1f", effectiveGstPercent)}% (₹${String.format("%.2f", gstAmount)})" else "Tax Removed / 0% GST (Tax Exempt)",
                                    fontSize = 10.5.sp,
                                    color = if (isTaxEnabled) accentColor else NeonGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (isTaxEnabled) "GST ON" else "GST OFF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isTaxEnabled) accentColor else grayText)
                                Spacer(modifier = Modifier.width(6.dp))
                                Switch(
                                    checked = isTaxEnabled,
                                    onCheckedChange = { isTaxEnabled = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = accentColor)
                                )
                            }
                        }

                        if (!isTaxEnabled) {
                            // Tax Removed Badge
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonGreen.copy(alpha = 0.1f))
                                    .border(0.8.dp, NeonGreen.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("0% GST applied — Generates Tax-Free / Cash Invoice without tax added.", fontSize = 11.sp, color = NeonGreen, fontWeight = FontWeight.Medium)
                                }
                            }
                        } else {
                            // Tax Presets & Variable Tax Control
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Select Fixed Slab or Variable Rate:", fontSize = 11.sp, color = grayText)
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf("0", "5", "12", "18", "28", "CUSTOM").forEach { slab ->
                                        val isSelected = selectedTaxPreset == slab
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = { selectedTaxPreset = slab },
                                            label = {
                                                Text(
                                                    if (slab == "CUSTOM") "Custom %" else if (slab == "0") "0% (Exempt)" else "$slab% GST",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                    }
                                }

                                // If Custom selected or variable rate needed
                                if (selectedTaxPreset == "CUSTOM") {
                                    OutlinedTextField(
                                        value = customTaxRateInput,
                                        onValueChange = { customTaxRateInput = it },
                                        label = { Text("Variable GST Rate (%)") },
                                        placeholder = { Text("e.g. 7.5 or 15", fontSize = 12.sp) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                        modifier = Modifier.fillMaxWidth(),
                                        singleLine = true,
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, unfocusedBorderColor = BorderSilver)
                                    )
                                }

                                // Detailed tax breakup
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("CGST (${String.format("%.1f", effectiveGstPercent / 2.0)}%): ₹${String.format("%.2f", gstAmount / 2.0)}", fontSize = 10.5.sp, color = textColor)
                                    Text("SGST (${String.format("%.1f", effectiveGstPercent / 2.0)}%): ₹${String.format("%.2f", gstAmount / 2.0)}", fontSize = 10.5.sp, color = textColor)
                                }
                            }
                        }
                    }
                }

                // 4. Discount Controls Card (User has full control to add/remove discount)
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSilver, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("4. Discount & Promotional Offer", fontWeight = FontWeight.Bold, color = textColor, fontSize = 13.sp)
                                Text(
                                    if (isDiscountEnabled && effectiveDiscountAmount > 0) "Discount: -₹${String.format("%.2f", effectiveDiscountAmount)}" else "No discount applied",
                                    fontSize = 10.5.sp,
                                    color = if (isDiscountEnabled && effectiveDiscountAmount > 0) NeonGreen else grayText,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(if (isDiscountEnabled) "DISCOUNT ON" else "OFF", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (isDiscountEnabled) NeonGreen else grayText)
                                Spacer(modifier = Modifier.width(6.dp))
                                Switch(
                                    checked = isDiscountEnabled,
                                    onCheckedChange = { isDiscountEnabled = it },
                                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = NeonGreen)
                                )
                            }
                        }

                        if (isDiscountEnabled) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // Discount Mode Selector (₹ Flat or % Percent)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { discountMode = "AMOUNT" },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (discountMode == "AMOUNT") NeonGreen else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (discountMode == "AMOUNT") Color.Black else textColor
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Text("₹ Flat Amount", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }

                                    Button(
                                        onClick = { discountMode = "PERCENT" },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (discountMode == "PERCENT") NeonGreen else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (discountMode == "PERCENT") Color.Black else textColor
                                        ),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.weight(1f),
                                        contentPadding = PaddingValues(vertical = 4.dp)
                                    ) {
                                        Text("% Percentage", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                // Discount Input Field
                                OutlinedTextField(
                                    value = discountInput,
                                    onValueChange = { discountInput = it },
                                    label = { Text(if (discountMode == "AMOUNT") "Discount Value in ₹" else "Discount Value in %") },
                                    placeholder = { Text(if (discountMode == "AMOUNT") "e.g. 150" else "e.g. 10", fontSize = 12.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    shape = RoundedCornerShape(10.dp),
                                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = NeonGreen, unfocusedBorderColor = BorderSilver)
                                )

                                // Quick Presets for Discount
                                Row(
                                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (discountMode == "AMOUNT") {
                                        listOf("50", "100", "200", "500").forEach { p ->
                                            SuggestionChip(
                                                onClick = { discountInput = p },
                                                label = { Text("₹$p off", fontSize = 10.sp) },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }
                                    } else {
                                        listOf("5", "10", "15", "20").forEach { p ->
                                            SuggestionChip(
                                                onClick = { discountInput = p },
                                                label = { Text("$p% off", fontSize = 10.sp) },
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 5. Payment Method & Customer WhatsApp Dispatch Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSilver, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("5. Payment Status & Customer Communication", fontWeight = FontWeight.Bold, color = textColor, fontSize = 13.sp)

                        // Payment Status selector: Paid / Partial / Pending
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(
                                "Paid" to "Full Paid",
                                "Partial" to "Partial Advance",
                                "Pending / Due" to "Due / Khata"
                            ).forEach { (statusKey, label) ->
                                val isSelected = paymentStatus == statusKey
                                val activeColor = when (statusKey) {
                                    "Paid" -> NeonGreen
                                    "Partial" -> Color(0xFFF59E0B)
                                    else -> Color(0xFFEF4444)
                                }
                                OutlinedButton(
                                    onClick = {
                                        paymentStatus = statusKey
                                        if (statusKey == "Paid") {
                                            amountPaidInput = ""
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        containerColor = if (isSelected) activeColor.copy(alpha = 0.15f) else Color.Transparent,
                                        contentColor = if (isSelected) activeColor else grayText
                                    ),
                                    border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, if (isSelected) activeColor else BorderSilver)
                                ) {
                                    Text(label, fontSize = 10.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, maxLines = 1)
                                }
                            }
                        }

                        // Payment Mode selector & optional Partial Amount input
                        if (paymentStatus != "Pending / Due") {
                            Row(
                                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Cash", "UPI / GPay", "Card", "Cheque / Bank").forEach { mode ->
                                    val isSelected = paymentMethod == mode
                                    FilterChip(
                                        selected = isSelected,
                                        onClick = { paymentMethod = mode },
                                        label = { Text(mode, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                }
                            }
                        }

                        if (paymentStatus == "Partial") {
                            OutlinedTextField(
                                value = amountPaidInput,
                                onValueChange = { amountPaidInput = it },
                                label = { Text("Amount Received Now (₹)") },
                                placeholder = { Text("Enter received payment") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFFF59E0B),
                                    unfocusedBorderColor = BorderSilver
                                )
                            )
                            if (calculatedBalanceDue > 0) {
                                Text(
                                    "Remaining Balance Due (Khata): ₹${String.format("%,.2f", calculatedBalanceDue)}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFF59E0B)
                                )
                            }
                        } else if (paymentStatus == "Pending / Due") {
                            Text(
                                "Total invoice amount ₹${String.format("%,.2f", grandTotal)} will be tracked as Due / Khata under customer profile.",
                                fontSize = 11.sp,
                                color = Color(0xFFEF4444),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        HorizontalDivider(color = BorderSilver)

                        // WhatsApp Dispatch Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Send, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Auto-Send Bill to Customer", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = textColor)
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    if (customerPhoneInput.isNotBlank()) "Dispatches itemized invoice & totals to ${customerPhoneInput.trim()} via WhatsApp" else "Enter customer phone above to dispatch via WhatsApp & SMS",
                                    fontSize = 10.5.sp,
                                    color = grayText
                                )
                            }
                            Switch(
                                checked = autoSendWhatsApp,
                                onCheckedChange = { autoSendWhatsApp = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = NeonGreen, checkedTrackColor = NeonGreen.copy(alpha = 0.3f))
                            )
                        }
                    }
                }

                // 6. Financial Summary Card & Generate Action
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderSilver, RoundedCornerShape(16.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Financial Breakdown", fontWeight = FontWeight.Bold, color = textColor, fontSize = 13.sp)

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal (Parts & Materials):", color = grayText, fontSize = 11.sp)
                            Text("₹${String.format("%,.2f", subTotalParts)}", color = textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Labor & Service Fee:", color = grayText, fontSize = 11.sp)
                            Text("₹${String.format("%,.2f", laborVal)}", color = textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Gross Total:", color = grayText, fontSize = 11.sp)
                            Text("₹${String.format("%,.2f", grossTotal)}", color = textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        if (isDiscountEnabled && effectiveDiscountAmount > 0.0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Discount Deduction:", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Text("-₹${String.format("%,.2f", effectiveDiscountAmount)}", color = NeonGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Net Taxable Amount:", color = grayText, fontSize = 11.sp)
                            Text("₹${String.format("%,.2f", taxableAmount)}", color = textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(
                                if (effectiveGstPercent > 0) "Integrated GST (${String.format("%.1f", effectiveGstPercent)}%):" else "GST Tax (0% - Exempt):",
                                color = grayText,
                                fontSize = 11.sp
                            )
                            Text("₹${String.format("%,.2f", gstAmount)}", color = textColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        if (calculatedBalanceDue > 0.0) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Balance Due (Unpaid / Khata):", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("₹${String.format("%,.2f", calculatedBalanceDue)}", color = Color(0xFFEF4444), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        HorizontalDivider(color = BorderSilver, modifier = Modifier.padding(vertical = 4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("GRAND TOTAL (INR)", fontWeight = FontWeight.Bold, color = grayText, fontSize = 10.sp)
                                Text("₹${String.format("%,.2f", grandTotal)}", fontWeight = FontWeight.Black, color = accentColor, fontSize = 20.sp)
                            }

                            Button(
                                onClick = {
                                    val finalCustName = customerNameInput.trim()
                                    if (finalCustName.isBlank()) {
                                        Toast.makeText(context, "Please enter customer name!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (selectedItems.isEmpty() && laborVal <= 0.0) {
                                        Toast.makeText(context, "Please add parts or labor charges!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }

                                    val finalPhone = customerPhoneInput.trim().ifEmpty { "9812045612" }
                                    val nowFormatted = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                                    val bill = Bill(
                                        customerName = finalCustName,
                                        customerPhone = finalPhone,
                                        items = selectedItems.toList(),
                                        laborCharges = laborVal,
                                        gstPercent = effectiveGstPercent,
                                        discountAmount = effectiveDiscountAmount,
                                        totalAmount = grandTotal,
                                        date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()),
                                        paymentStatus = paymentStatus,
                                        paymentMethod = if (paymentStatus == "Pending / Due") "Due (Khata)" else paymentMethod,
                                        balanceDue = calculatedBalanceDue,
                                        sentToCustomerAt = if (autoSendWhatsApp) nowFormatted else null,
                                        sentVia = if (autoSendWhatsApp) "WhatsApp" else null
                                    )
                                    repository.addBill(bill)
                                    selectedItems.clear()
                                    generatedBillForDialog = bill
                                    showInvoiceOptionsDialog = true
                                    Toast.makeText(context, "Tax Invoice Generated Successfully!", Toast.LENGTH_SHORT).show()

                                    if (autoSendWhatsApp) {
                                        sendBillToCustomerWhatsApp(context, bill) {
                                            // intent initiated
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                                shape = RoundedCornerShape(12.dp),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
                            ) {
                                Icon(Icons.Default.Receipt, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Generate Invoice", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        } else {
            // Past Invoices List View with Search & Status Filters
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Search field
                OutlinedTextField(
                    value = invoiceSearchQuery,
                    onValueChange = { invoiceSearchQuery = it },
                    placeholder = { Text("Search by customer name, phone, or bill ID...", fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = grayText, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (invoiceSearchQuery.isNotBlank()) {
                            IconButton(onClick = { invoiceSearchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = grayText, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = BorderSilver
                    )
                )

                val paidCount = remember(bills) { bills.count { it.paymentStatus.equals("Paid", ignoreCase = true) } }
                val dueCount = remember(bills) { bills.count { !it.paymentStatus.equals("Paid", ignoreCase = true) || it.balanceDue > 0 } }
                val dispatchedCount = remember(bills) { bills.count { it.sentToCustomerAt != null } }
                val pendingDispatchCount = remember(bills) { bills.count { it.sentToCustomerAt == null } }

                // Quick Status & Dispatch Filters
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        "All" to "All (${bills.size})",
                        "Paid" to "Paid ($paidCount)",
                        "Due" to "Due / Khata ($dueCount)",
                        "Dispatched" to "WhatsApp Sent ($dispatchedCount)",
                        "Pending Dispatch" to "Not Sent Yet ($pendingDispatchCount)"
                    ).forEach { (filterKey, label) ->
                        val isSelected = invoiceStatusFilter == filterKey
                        FilterChip(
                            selected = isSelected,
                            onClick = { invoiceStatusFilter = filterKey },
                            label = { Text(label, fontSize = 10.5.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                val filteredBills = remember(bills, invoiceSearchQuery, invoiceStatusFilter) {
                    bills.filter { b ->
                        val query = invoiceSearchQuery.trim().lowercase()
                        val matchesSearch = query.isBlank() ||
                            b.customerName.lowercase().contains(query) ||
                            b.customerPhone.contains(query) ||
                            b.id.lowercase().contains(query) ||
                            (b.invoiceNumber?.lowercase()?.contains(query) == true)

                        val matchesFilter = when (invoiceStatusFilter) {
                            "Paid" -> b.paymentStatus.equals("Paid", ignoreCase = true)
                            "Due" -> !b.paymentStatus.equals("Paid", ignoreCase = true) || b.balanceDue > 0
                            "Dispatched" -> b.sentToCustomerAt != null
                            "Pending Dispatch" -> b.sentToCustomerAt == null
                            else -> true
                        }
                        matchesSearch && matchesFilter
                    }
                }

                if (filteredBills.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp)
                            .border(1.dp, BorderSilver, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = accentColor, modifier = Modifier.size(44.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                if (bills.isEmpty()) "No Invoices Generated Yet" else "No matching invoices found",
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                fontSize = 15.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                if (bills.isEmpty()) "Switch to 'Create Tax Invoice' to generate your first invoice." else "Try adjusting search query or clearing filter chips.",
                                fontSize = 11.sp,
                                color = grayText
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 16.dp)
                    ) {
                        items(filteredBills) { b ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = cardBg),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BorderSilver, RoundedCornerShape(16.dp))
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    // Row 1: Customer Name, Phone & Amount
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(b.customerName, fontWeight = FontWeight.Bold, color = textColor, fontSize = 15.sp)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(formatPhoneNumber(b.customerPhone), fontSize = 11.sp, color = grayText)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Icon(
                                                    Icons.Default.Phone,
                                                    contentDescription = "Call Customer",
                                                    tint = accentColor,
                                                    modifier = Modifier
                                                        .size(13.dp)
                                                        .clickable {
                                                            try {
                                                                val callIntent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                                                    data = android.net.Uri.parse("tel:${b.customerPhone}")
                                                                }
                                                                context.startActivity(callIntent)
                                                            } catch (_: Exception) {}
                                                        }
                                                )
                                            }
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("₹${String.format("%,.2f", b.totalAmount)}", fontWeight = FontWeight.Black, color = NeonGreen, fontSize = 16.sp)
                                            Text(b.date, fontSize = 10.sp, color = grayText)
                                        }
                                    }

                                    // Row 2: Status tags for GST, Discount & Payment Status
                                    Row(
                                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        // Payment Status Tag
                                        val isPaid = b.paymentStatus.equals("Paid", ignoreCase = true)
                                        val isPartial = b.paymentStatus.equals("Partial", ignoreCase = true)
                                        val statusBg = if (isPaid) NeonGreen.copy(alpha = 0.12f) else if (isPartial) Color(0xFFF59E0B).copy(alpha = 0.12f) else Color(0xFFEF4444).copy(alpha = 0.12f)
                                        val statusColor = if (isPaid) NeonGreen else if (isPartial) Color(0xFFF59E0B) else Color(0xFFEF4444)
                                        val statusText = if (isPaid) "PAID (${b.paymentMethod ?: "Cash"})" else if (isPartial) "PARTIAL (Due: ₹${String.format("%.0f", b.balanceDue)})" else "UNPAID KHATA (Due: ₹${String.format("%.0f", b.balanceDue.takeIf { it > 0 } ?: b.totalAmount)})"

                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(statusBg)
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(statusText, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, color = statusColor)
                                        }

                                        // GST Tag
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (b.gstPercent > 0) accentColor.copy(alpha = 0.12f) else NeonGreen.copy(alpha = 0.12f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                if (b.gstPercent > 0) "GST: ${String.format("%.1f", b.gstPercent)}%" else "Exempt (0%)",
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (b.gstPercent > 0) accentColor else NeonGreen
                                            )
                                        }

                                        if (b.discountAmount > 0.0) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(accentColor.copy(alpha = 0.12f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    "Disc: -₹${String.format("%.0f", b.discountAmount)}",
                                                    fontSize = 9.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = accentColor
                                                )
                                            }
                                        }
                                    }

                                    // Row 3: Items summary
                                    Text("Items: ${b.items.joinToString { "${it.name} (${it.qty})" }}", fontSize = 11.sp, color = grayText, maxLines = 1)

                                    // Row 4: Dispatch Log Entry Box
                                    val isDispatched = b.sentToCustomerAt != null
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isDispatched) NeonGreen.copy(alpha = 0.08f) else Color(0xFFF59E0B).copy(alpha = 0.08f)
                                        ),
                                        border = BorderStroke(0.8.dp, if (isDispatched) NeonGreen.copy(alpha = 0.3f) else Color(0xFFF59E0B).copy(alpha = 0.3f)),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    if (isDispatched) Icons.Default.CheckCircle else Icons.Default.AccessTime,
                                                    contentDescription = null,
                                                    tint = if (isDispatched) NeonGreen else Color(0xFFF59E0B),
                                                    modifier = Modifier.size(15.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    if (isDispatched) "Sent to customer: ${b.sentToCustomerAt} via ${b.sentVia ?: "WhatsApp"}" else "Bill not dispatched to customer yet",
                                                    fontSize = 10.5.sp,
                                                    fontWeight = FontWeight.Medium,
                                                    color = textColor,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }

                                            TextButton(
                                                onClick = {
                                                    sendBillToCustomerWhatsApp(context, b) {
                                                        val nowStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                                                        repository.updateBill(b.copy(sentToCustomerAt = nowStr, sentVia = "WhatsApp"))
                                                        Toast.makeText(context, "Bill dispatched to customer on WhatsApp & entry recorded!", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Text(if (isDispatched) "Resend" else "Send Now", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = BorderSilver)

                                    // Row 5: Action Buttons (WhatsApp, SMS, PDF, Options)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.End,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // WhatsApp Dispatch Button
                                        Button(
                                            onClick = {
                                                sendBillToCustomerWhatsApp(context, b) {
                                                    val nowStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                                                    repository.updateBill(b.copy(sentToCustomerAt = nowStr, sentVia = "WhatsApp"))
                                                    Toast.makeText(context, "WhatsApp dispatch recorded in database!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonGreen.copy(alpha = 0.15f), contentColor = NeonGreen),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("WhatsApp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // SMS Dispatch Button
                                        Button(
                                            onClick = {
                                                sendBillToCustomerSms(context, b) {
                                                    val nowStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                                                    repository.updateBill(b.copy(sentToCustomerAt = nowStr, sentVia = "SMS"))
                                                    Toast.makeText(context, "SMS dispatch recorded in database!", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = accentColor.copy(alpha = 0.15f), contentColor = accentColor),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("SMS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        // Share PDF Button
                                        Button(
                                            onClick = {
                                                exportAndShareBill(context, b)
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = textColor),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                            modifier = Modifier.height(32.dp)
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(13.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }

                                        Spacer(modifier = Modifier.width(6.dp))

                                        var showInvoiceMenu by remember { mutableStateOf(false) }
                                        Box {
                                            IconButton(
                                                onClick = { showInvoiceMenu = true },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(
                                                        if (isLight) Color(0xFFF1F5F9) else Color(0xFF1E293B).copy(alpha = 0.6f)
                                                    )
                                                    .border(0.8.dp, if (isLight) Color(0xFFCBD5E1) else Color(0xFF334155), RoundedCornerShape(8.dp))
                                            ) {
                                                Icon(
                                                    Icons.Default.MoreVert,
                                                    contentDescription = "More Invoice Actions",
                                                    tint = if (isLight) Color(0xFF475569) else Color(0xFF94A3B8),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }

                                            DropdownMenu(
                                                expanded = showInvoiceMenu,
                                                onDismissRequest = { showInvoiceMenu = false },
                                                modifier = Modifier
                                                    .background(if (isLight) Color.White else Color(0xFF0F172A))
                                                    .border(1.dp, if (isLight) Color(0xFFE2E8F0) else Color(0xFF334155), RoundedCornerShape(12.dp))
                                            ) {
                                                DropdownMenuItem(
                                                    leadingIcon = {
                                                        Icon(Icons.Default.Send, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(17.dp))
                                                    },
                                                    text = {
                                                        Text("Send on WhatsApp", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = textColor)
                                                    },
                                                    onClick = {
                                                        showInvoiceMenu = false
                                                        sendBillToCustomerWhatsApp(context, b) {
                                                            val nowStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                                                            repository.updateBill(b.copy(sentToCustomerAt = nowStr, sentVia = "WhatsApp"))
                                                        }
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    leadingIcon = {
                                                        Icon(Icons.Default.Chat, contentDescription = null, tint = accentColor, modifier = Modifier.size(17.dp))
                                                    },
                                                    text = {
                                                        Text("Send on SMS", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = textColor)
                                                    },
                                                    onClick = {
                                                        showInvoiceMenu = false
                                                        sendBillToCustomerSms(context, b) {
                                                            val nowStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                                                            repository.updateBill(b.copy(sentToCustomerAt = nowStr, sentVia = "SMS"))
                                                        }
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Default.TableChart,
                                                            contentDescription = null,
                                                            tint = NeonGreen,
                                                            modifier = Modifier.size(17.dp)
                                                        )
                                                    },
                                                    text = {
                                                        Text(
                                                            "Export CSV / Excel",
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 13.sp,
                                                            color = if (isLight) Color(0xFF0F172A) else Color.White
                                                        )
                                                    },
                                                    onClick = {
                                                        showInvoiceMenu = false
                                                        exportAndShareBillExcel(context, b)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.AutoMirrored.Filled.OpenInNew,
                                                            contentDescription = null,
                                                            tint = accentColor,
                                                            modifier = Modifier.size(17.dp)
                                                        )
                                                    },
                                                    text = {
                                                        Text(
                                                            "View in Drive / PDF",
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 13.sp,
                                                            color = if (isLight) Color(0xFF0F172A) else Color.White
                                                        )
                                                    },
                                                    onClick = {
                                                        showInvoiceMenu = false
                                                        exportAndOpenInDrive(context, b)
                                                    }
                                                )
                                                DropdownMenuItem(
                                                    leadingIcon = {
                                                        Icon(
                                                            Icons.Default.Delete,
                                                            contentDescription = null,
                                                            tint = Color(0xFFEF4444),
                                                            modifier = Modifier.size(17.dp)
                                                        )
                                                    },
                                                    text = {
                                                        Text(
                                                            "Delete Invoice",
                                                            fontWeight = FontWeight.SemiBold,
                                                            fontSize = 13.sp,
                                                            color = Color(0xFFEF4444)
                                                        )
                                                    },
                                                    onClick = {
                                                        showInvoiceMenu = false
                                                        repository.deleteBill(b.id)
                                                        Toast.makeText(context, "Invoice deleted", Toast.LENGTH_SHORT).show()
                                                    }
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
    }

    // Modal 1: Select Customer Dialog (Full search, non-blocking, zero overlapping of base screen)
    if (showCustomerPickerDialog) {
        AlertDialog(
            onDismissRequest = { showCustomerPickerDialog = false },
            containerColor = if (isLight) Color.White else Color(0xFF0F172A),
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PersonSearch, contentDescription = null, tint = accentColor, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Saved Client", fontWeight = FontWeight.Bold, color = textColor, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customerPickerSearch,
                        onValueChange = { customerPickerSearch = it },
                        placeholder = { Text("Search by name or phone...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = grayText) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, unfocusedBorderColor = BorderSilver)
                    )

                    val matchedCusts = customers.filter {
                        it.name.contains(customerPickerSearch, ignoreCase = true) || it.phone.contains(customerPickerSearch)
                    }

                    if (matchedCusts.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                            Text("No matching customers found", fontSize = 12.sp, color = grayText)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(matchedCusts) { cust ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable {
                                            customerNameInput = cust.name
                                            customerPhoneInput = cust.phone
                                            showCustomerPickerDialog = false
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(cust.name, fontWeight = FontWeight.Bold, color = textColor, fontSize = 13.sp)
                                        Text(formatPhoneNumber(cust.phone), fontSize = 10.5.sp, color = grayText)
                                    }
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showCustomerPickerDialog = false }) {
                    Text("Close", color = grayText, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    // Modal 2: Inventory Part Picker Dialog
    if (showInventoryPickerDialog) {
        AlertDialog(
            onDismissRequest = { showInventoryPickerDialog = false },
            containerColor = if (isLight) Color.White else Color(0xFF0F172A),
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Inventory2, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Parts from Inventory", fontWeight = FontWeight.Bold, color = textColor, fontSize = 16.sp)
                }
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = inventoryPickerSearch,
                        onValueChange = { inventoryPickerSearch = it },
                        placeholder = { Text("Search spare parts or materials...", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = grayText) },
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = ElectricBlue, unfocusedBorderColor = BorderSilver)
                    )

                    val matchedItems = inventory.filter {
                        it.name.contains(inventoryPickerSearch, ignoreCase = true) || it.category.contains(inventoryPickerSearch, ignoreCase = true)
                    }

                    if (matchedItems.isEmpty()) {
                        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
                            Text("No inventory items found", fontSize = 12.sp, color = grayText)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 280.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(matchedItems) { inv ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .clickable {
                                            val existing = selectedItems.find { it.name == inv.name }
                                            if (existing != null) {
                                                val idx = selectedItems.indexOf(existing)
                                                selectedItems[idx] = existing.copy(qty = existing.qty + 1, total = (existing.qty + 1) * inv.price)
                                            } else {
                                                selectedItems.add(BillItem(inv.name, 1, inv.price, inv.price))
                                            }
                                            Toast.makeText(context, "Added ${inv.name}", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(inv.name, fontWeight = FontWeight.Bold, color = textColor, fontSize = 12.5.sp)
                                        Text("${inv.category} • Stock: ${inv.stockLevel}", fontSize = 10.sp, color = grayText)
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("₹${inv.price}", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(Icons.Default.AddCircle, contentDescription = "Add", tint = ElectricBlue, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { showInventoryPickerDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Text("Done", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Modal 3: Custom Item Addition Dialog
    if (showCustomItemDialog) {
        AlertDialog(
            onDismissRequest = { showCustomItemDialog = false },
            containerColor = if (isLight) Color.White else Color(0xFF0F172A),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text("Add Custom Line Item", fontWeight = FontWeight.Bold, color = textColor, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customItemName,
                        onValueChange = { customItemName = it },
                        label = { Text("Item / Service Description *") },
                        placeholder = { Text("e.g. Copper Fitting or Repair Charge") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, unfocusedBorderColor = BorderSilver)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = customItemQty,
                            onValueChange = { customItemQty = it },
                            label = { Text("Quantity") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, unfocusedBorderColor = BorderSilver)
                        )

                        OutlinedTextField(
                            value = customItemRate,
                            onValueChange = { customItemRate = it },
                            label = { Text("Rate / Price (₹) *") },
                            placeholder = { Text("e.g. 250") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.weight(1.3f),
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, unfocusedBorderColor = BorderSilver)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val name = customItemName.trim()
                        val qty = customItemQty.toIntOrNull() ?: 1
                        val rate = customItemRate.toDoubleOrNull() ?: 0.0

                        if (name.isBlank()) {
                            Toast.makeText(context, "Please enter item name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (rate <= 0.0) {
                            Toast.makeText(context, "Please enter a valid price", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        selectedItems.add(BillItem(name, qty, rate, qty * rate))
                        showCustomItemDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Add Item", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomItemDialog = false }) {
                    Text("Cancel", color = grayText)
                }
            }
        )
    }

    if (showInvoiceOptionsDialog && generatedBillForDialog != null) {
        val bill = generatedBillForDialog!!
        AlertDialog(
            onDismissRequest = { showInvoiceOptionsDialog = false },
            containerColor = if (isLight) Color.White else Color(0xFF0F172A),
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(20.dp)),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.AutoMirrored.Filled.ReceiptLong, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Invoice Dispatch & Actions", fontWeight = FontWeight.Bold, color = textColor, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Invoice for ${bill.customerName} (₹${String.format("%.2f", bill.totalAmount)}) generated! Send to customer phone ${bill.customerPhone} or export file:",
                        fontSize = 11.5.sp,
                        color = textColor
                    )

                    // Option 1: WhatsApp Bill Dispatch to Customer Phone
                    Card(
                        colors = CardDefaults.cardColors(containerColor = NeonGreen.copy(alpha = 0.12f)),
                        border = BorderStroke(1.2.dp, NeonGreen.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                sendBillToCustomerWhatsApp(context, bill) {
                                    val nowStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                                    repository.updateBill(bill.copy(sentToCustomerAt = nowStr, sentVia = "WhatsApp"))
                                    Toast.makeText(context, "Dispatched on WhatsApp & recorded in database!", Toast.LENGTH_SHORT).show()
                                }
                            }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(NeonGreen.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Send, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Send Bill to Customer on WhatsApp", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = textColor)
                                Text("Itemized bill, totals & store info to +91 ${bill.customerPhone}", fontSize = 10.sp, color = grayText)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(14.dp))
                        }
                    }

                    // Option 2: SMS Bill Dispatch to Customer Phone
                    Card(
                        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.08f)),
                        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                sendBillToCustomerSms(context, bill) {
                                    val nowStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date())
                                    repository.updateBill(bill.copy(sentToCustomerAt = nowStr, sentVia = "SMS"))
                                    Toast.makeText(context, "Dispatched via SMS & recorded in database!", Toast.LENGTH_SHORT).show()
                                }
                            }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(accentColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Chat, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Send Bill via SMS Text", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = textColor)
                                Text("Delivers summary SMS to +91 ${bill.customerPhone}", fontSize = 10.sp, color = grayText)
                            }
                            Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                        }
                    }
                    
                    // Option 3: PDF Download & Share
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, BorderSilver),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                exportAndShareBill(context, bill)
                            }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Download, contentDescription = null, tint = accentColor)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Download & Share PDF", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                                Text("Saves to device downloads and opens share chooser", fontSize = 10.sp, color = grayText)
                            }
                        }
                    }
                    
                    // Option 4: PDF View / Drive
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, BorderSilver),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                exportAndOpenInDrive(context, bill)
                            }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, tint = NeonGreen)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Open with PDF Viewer / Drive", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                                Text("Instantly open invoice in system PDF reader or Drive", fontSize = 10.sp, color = grayText)
                            }
                        }
                    }
                    
                    // Option 5: Excel CSV Download
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        border = BorderStroke(1.dp, BorderSilver),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                exportAndShareBillExcel(context, bill)
                            }
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TableChart, contentDescription = null, tint = NeonYellow)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Download Excel Sheet (CSV)", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = textColor)
                                Text("Saves as Excel spreadsheet table format", fontSize = 10.sp, color = grayText)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showInvoiceOptionsDialog = false }) {
                    Text("Close", color = accentColor, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

// --- MODULE 7: EXPENSES MODULE SCREEN ---
@Composable
fun ExpensesScreen(repository: ReLectRepository, accentColor: Color) {
    val expenses by repository.expenses.collectAsState()
    var expCategory by remember { mutableStateOf("Tea & Refreshments") }
    var expTitle by remember { mutableStateOf("") }
    var expAmount by remember { mutableStateOf("") }

    val isLight = MaterialTheme.colorScheme.surface == Color.White
    val textColor = if (isLight) Color(0xFF0F172A) else Color.White
    val grayText = if (isLight) Color(0xFF475569) else Color(0xFFCBD5E1)
    val subText = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8)
    val cardBg = if (isLight) Color.White else Color(0xFF1E293B)
    val cardBorder = if (isLight) Color(0xFFE2E8F0) else Color(0xFF334155)
    val inputBg = if (isLight) Color(0xFFF8FAFC) else Color(0xFF0F172A)

    var voiceInputText by remember { mutableStateOf("") }
    var isListeningVoice by remember { mutableStateOf(false) }
    var isProcessingVoice by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val categories = listOf(
        "Tea & Refreshments",
        "Travel & Fuel",
        "Shop Maintenance",
        "Wiring & Cables",
        "Tools & Hardware",
        "Miscellaneous"
    )

    fun getCategoryIcon(cat: String): ImageVector = when {
        cat.contains("Tea", ignoreCase = true) || cat.contains("Refreshment", ignoreCase = true) || cat.contains("Snack", ignoreCase = true) || cat.contains("Chai", ignoreCase = true) -> Icons.Default.LocalCafe
        cat.contains("Travel", ignoreCase = true) || cat.contains("Fuel", ignoreCase = true) || cat.contains("Petrol", ignoreCase = true) -> Icons.Default.LocalGasStation
        cat.contains("Maintenance", ignoreCase = true) || cat.contains("Shop", ignoreCase = true) || cat.contains("Cleaning", ignoreCase = true) -> Icons.Default.CleaningServices
        cat.contains("Cable", ignoreCase = true) || cat.contains("Wire", ignoreCase = true) || cat.contains("Wiring", ignoreCase = true) -> Icons.Default.Bolt
        cat.contains("Tool", ignoreCase = true) || cat.contains("Hardware", ignoreCase = true) -> Icons.Default.Build
        else -> Icons.Default.Receipt
    }

    val context = LocalContext.current

    val voiceLauncher = rememberSpeechRecognizerLauncher(
        onResult = { text ->
            voiceInputText = text
            if (text.isNotBlank()) {
                isProcessingVoice = true
                scope.launch {
                    try {
                        val parsed = GeminiClient.parseVoiceInput(text)
                        expTitle = parsed.item
                        expAmount = parsed.amount.toString()
                        
                        val matchedCat = categories.find { 
                            it.equals(parsed.category, ignoreCase = true) || 
                            parsed.category.contains(it, ignoreCase = true) ||
                            it.contains(parsed.category, ignoreCase = true)
                        }
                        if (matchedCat != null) {
                            expCategory = matchedCat
                        }
                        Toast.makeText(context, "Voice parsed: \"$text\"", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isProcessingVoice = false
                        voiceInputText = ""
                    }
                }
            }
        },
        onStateChange = { listening, error ->
            isListeningVoice = listening
            if (error != null) {
                Toast.makeText(context, "Voice status: $error", Toast.LENGTH_SHORT).show()
            }
        }
    )

    val todayTotal = expenses.sumOf { it.amount }
    val isRevolt = accentColor == Color(0xFFEF4444)
    val badgeColor = if (isRevolt) Color(0xFFEF4444) else NeonRed

    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Full-width Header with clear title and subtitle
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "Daily Expenditures Board",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = textColor,
                letterSpacing = (-0.3).sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "Track minor daily shop overheads & cash outlays",
                fontSize = 12.sp,
                color = grayText,
                fontWeight = FontWeight.Medium
            )
        }

        // Full-width Amount Overview Card perfectly aligned according to the whole heading and screen layout
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(badgeColor.copy(alpha = 0.12f))
                            .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PriceChange,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Today's Total Outflow",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(1.dp))
                        Text(
                            text = if (expenses.isEmpty()) "No spends recorded today" else "${expenses.size} expenses tracked today",
                            fontSize = 11.sp,
                            color = grayText
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = badgeColor.copy(alpha = 0.09f),
                    border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Today:",
                            color = badgeColor,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp
                        )
                        Text(
                            text = "₹${String.format("%,.0f", todayTotal)}",
                            color = badgeColor,
                            fontWeight = FontWeight.Black,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        // Real-time NLP voice parser with Gemini AI
        val triggerExpenseParsing: (String) -> Unit = { text ->
            if (text.isNotBlank()) {
                isProcessingVoice = true
                scope.launch {
                    try {
                        val parsed = GeminiClient.parseVoiceInput(text)
                        expTitle = parsed.item
                        expAmount = parsed.amount.toString()
                        
                        val matchedCat = categories.find { 
                            it.equals(parsed.category, ignoreCase = true) || 
                            parsed.category.contains(it, ignoreCase = true) ||
                            it.contains(parsed.category, ignoreCase = true)
                        }
                        if (matchedCat != null) {
                            expCategory = matchedCat
                        }
                        Toast.makeText(context, "Voice parsed successfully via Gemini AI!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isProcessingVoice = false
                        voiceInputText = ""
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (isProcessingVoice) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = accentColor,
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(
                        onClick = {
                            voiceLauncher.toggle()
                        },
                        enabled = !isProcessingVoice,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isListeningVoice) Icons.Default.StopCircle else Icons.Default.Mic,
                            contentDescription = if (isListeningVoice) "Stop Mic" else "Start Mic",
                            tint = if (isListeningVoice) NeonRed else accentColor,
                            modifier = Modifier
                                .size(24.dp)
                                .then(if (isListeningVoice) Modifier.pulsingAnimation() else Modifier)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = voiceInputText,
                    onValueChange = { voiceInputText = it },
                    placeholder = { Text(if (isProcessingVoice) "Processing voice..." else "Speak or type (e.g. Chai 50 rs)", fontSize = 12.sp, color = grayText) },
                    enabled = !isProcessingVoice,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = cardBorder,
                        focusedContainerColor = inputBg,
                        unfocusedContainerColor = inputBg
                    ),
                    trailingIcon = {
                        if (voiceInputText.isNotBlank() && !isProcessingVoice) {
                            IconButton(onClick = {
                                triggerExpenseParsing(voiceInputText)
                            }) {
                                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Parse", tint = accentColor)
                            }
                        }
                    }
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Mic toggle button (Tap to Start / Tap to Stop & Pause)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = {
                            voiceLauncher.toggle()
                        },
                        enabled = !isProcessingVoice,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isListeningVoice) NeonRed else accentColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = if (isListeningVoice) Icons.Default.Stop else Icons.Default.Mic,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isListeningVoice) "Stop Mic" else "Tap Mic", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = {
                            val samplePhrases = listOf(
                                "Paid 150 rupees for staff tea and snacks today",
                                "ਪੰਜ ਸੌ ਰੁਪਏ ਪੈਟਰੋਲ ਲਈ ਦਿੱਤੇ ਨੇ",
                                "दुकान की सफाई और झाड़ू के लिए दो सौ रुपये खर्च किए"
                            )
                            val selectedPhrase = samplePhrases.random()
                            voiceInputText = selectedPhrase
                            triggerExpenseParsing(selectedPhrase)
                        },
                        enabled = !isProcessingVoice && !isListeningVoice,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(Icons.Default.Casino, contentDescription = "Simulate Phrase", tint = accentColor.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Quick Entry Card with Clean Category Chips Selector
        Card(
            colors = CardDefaults.cardColors(containerColor = cardBg),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Record Daily Cash Spend", fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.5.sp)

                // Category Chips Selector
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Select Spend Category", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = grayText)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { cat ->
                            val isSelected = expCategory == cat
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) accentColor else inputBg)
                                    .border(
                                        1.dp,
                                        if (isSelected) accentColor else cardBorder,
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { expCategory = cat }
                                    .padding(horizontal = 12.dp, vertical = 7.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = getCategoryIcon(cat),
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else accentColor,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        cat,
                                        fontSize = 11.5.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else textColor
                                    )
                                }
                            }
                        }
                    }
                }

                // Input row: Amount and Spend details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = expAmount,
                        onValueChange = { expAmount = it },
                        label = { Text("Amount (₹)", fontSize = 11.sp) },
                        placeholder = { Text("₹0", fontSize = 12.sp, color = grayText) },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textColor
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = cardBorder,
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = grayText,
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg
                        )
                    )

                    OutlinedTextField(
                        value = expTitle,
                        onValueChange = { expTitle = it },
                        label = { Text("Notes / Details", fontSize = 11.sp) },
                        placeholder = { Text("e.g. Chai for staff", fontSize = 12.sp, color = grayText) },
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 13.sp,
                            color = textColor
                        ),
                        modifier = Modifier.weight(1.8f),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = textColor,
                            unfocusedTextColor = textColor,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = cardBorder,
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = grayText,
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg
                        )
                    )
                }

                Button(
                    onClick = {
                        val amt = expAmount.toDoubleOrNull() ?: 0.0
                        if (expTitle.isNotBlank() && amt > 0) {
                            repository.addExpense(
                                Expense(
                                    category = expCategory,
                                    title = expTitle,
                                    amount = amt
                                )
                            )
                            expTitle = ""
                            expAmount = ""
                            Toast.makeText(context, "Cash expense recorded!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonRed, contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Add Cash Spend Entry", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }

        // Spend list
        if (expenses.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorder, RoundedCornerShape(14.dp))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.ReceiptLong,
                        contentDescription = null,
                        tint = grayText.copy(alpha = 0.5f),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No Expenses Recorded Today", fontWeight = FontWeight.SemiBold, color = textColor, fontSize = 13.5.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Record petty cash spends above or use the voice mic to auto-fill.", color = grayText, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                items(expenses) { exp ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, cardBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(accentColor.copy(alpha = 0.15f))
                                        .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(exp.category),
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(accentColor.copy(alpha = 0.12f))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = exp.category,
                                                fontSize = 9.5.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = accentColor
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = exp.date,
                                            fontSize = 10.sp,
                                            color = subText
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(3.dp))
                                    Text(
                                        text = exp.title,
                                        fontWeight = FontWeight.SemiBold,
                                        color = textColor,
                                        fontSize = 13.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(NeonRed.copy(alpha = 0.12f))
                                        .border(1.dp, NeonRed.copy(alpha = 0.35f), RoundedCornerShape(8.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "₹${String.format("%,.0f", exp.amount)}",
                                        color = NeonRed,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 13.5.sp
                                    )
                                }
                                IconButton(
                                    onClick = { repository.deleteExpense(exp.id) },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Delete",
                                        tint = if (isLight) Color(0xFF94A3B8) else Color(0xFF94A3B8),
                                        modifier = Modifier.size(17.dp)
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

// --- MODULE 8: REPORTS SCREEN (ANALYTIC GRAPHS) ---
@Composable
fun ReportsScreen(repository: ReLectRepository, accentColor: Color) {
    val bills by repository.bills.collectAsState()
    val expenses by repository.expenses.collectAsState()

    val totalSales = bills.sumOf { it.totalAmount }
    val totalSpend = expenses.sumOf { it.amount }
    val netProfit = (totalSales - totalSpend).coerceAtLeast(0.0)

    val isLight = MaterialTheme.colorScheme.surface == Color.White
    val textColor = if (isLight) Color(0xFF0F172A) else Color.White
    val grayText = if (isLight) Color(0xFF475569) else Color(0xFF94A3B8)
    val cardBg = if (isLight) Color.White else Color(0xFF131B2E)
    val cardBorder = if (isLight) Color(0xFFE2E8F0) else Color(0x3364748B)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column {
                Text("Analytics Ledger & Reports", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, letterSpacing = (-0.3).sp)
                Text("Enterprise profit, parts turnover, and work progression metrics", fontSize = 12.sp, color = grayText, fontWeight = FontWeight.Medium)
            }
        }

        // Financial Overview Widgets
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Gross Billing", color = grayText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("₹${String.format("%,.0f", totalSales)}", color = NeonGreen, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Total Spend", color = grayText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("₹${String.format("%,.0f", totalSpend)}", color = NeonRed, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    modifier = Modifier
                        .weight(1f)
                        .border(1.dp, cardBorder, RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Net Yield", color = grayText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("₹${String.format("%,.0f", netProfit)}", color = accentColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }

        // Pie chart for Expense Allocation
        item {
            val categoryTotals = remember(expenses) {
                expenses.groupBy { it.category }.mapValues { it.value.sumOf { e -> e.amount } }
            }

            val totalCategorySpend = categoryTotals.values.sum()
            val colors = listOf(NeonRed, CyberPurple, Color(0xFF00ADB5), Color(0xFFFF9F43), Color(0xFF10AC84), Color(0xFF3B82F6))

            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, cardBorder, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Expense Categorization Shares", fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
                        if (totalCategorySpend > 0) {
                            Text(
                                "Total: ₹${String.format("%,.0f", totalCategorySpend)}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (categoryTotals.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.PieChart,
                                contentDescription = null,
                                tint = grayText.copy(alpha = 0.5f),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "No Spends Recorded Yet",
                                fontWeight = FontWeight.SemiBold,
                                color = textColor,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Add cash spend entries in Daily Expenditures to see allocation shares.",
                                color = grayText,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        val chartData = remember(categoryTotals) {
                            val total = totalCategorySpend.coerceAtLeast(1.0)
                            categoryTotals.entries.mapIndexed { idx, entry ->
                                val pct = (entry.value / total) * 100
                                Triple(entry.key, pct, colors[idx % colors.size])
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Drawing dynamic pie slices using calculated angles
                            Canvas(modifier = Modifier.size(96.dp)) {
                                var currentAngle = -90f
                                chartData.forEach { data ->
                                    val sweep = (data.second.toFloat() / 100f) * 360f
                                    drawArc(
                                        color = data.third,
                                        startAngle = currentAngle,
                                        sweepAngle = sweep,
                                        useCenter = true
                                    )
                                    currentAngle += sweep
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(start = 12.dp)) {
                                chartData.forEach { data ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(data.third)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "${data.first}: ${String.format("%.1f", data.second)}%",
                                            color = textColor,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.5.sp
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

// --- MODULE 9: SETTINGS SCREEN ---
@Composable
fun SettingsScreen(
    repository: ReLectRepository,
    accentColor: Color,
    onRoleChange: (String) -> Unit,
    activeRole: String = "admin",
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onAccentColorChange: (Color) -> Unit,
    onNavigateToSection: (String) -> Unit = {}
) {
    val firestoreProject by repository.firestoreProjectName.collectAsState()
    var shopName by remember { mutableStateOf("Rattan Electricals") }
    var gstNo by remember { mutableStateOf("03AABCR1234F1Z5") }

    var isProjectExpanded by remember { mutableStateOf(false) }
    var isAppearanceExpanded by remember { mutableStateOf(false) }
    var isFirebaseExpanded by remember { mutableStateOf(false) }
    var isSmsGatewayExpanded by remember { mutableStateOf(false) }
    var isPermissionsExpanded by remember { mutableStateOf(false) }
    var isDbMaintenanceExpanded by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val isLight = MaterialTheme.colorScheme.surface == Color.White
    val textColor = if (isLight) Color(0xFF0F172A) else Color.White
    val grayText = if (isLight) Color(0xFF64748B) else Color(0xFF94A3B8)
    val cardBg = if (isLight) Color.White else Color(0xFF0F172A)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 0.dp, bottom = 24.dp)
    ) {
        item {
            Column {
                Text("Enterprise System Preferences", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, letterSpacing = (-0.3).sp)
                Text("Configure store profile, branding, app QR code desk & cloud sync", fontSize = 12.sp, color = grayText, fontWeight = FontWeight.Medium)
            }
        }

        // Section 1: Enterprise Profile & GST
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSilver, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isProjectExpanded = !isProjectExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElectricBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Store, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Enterprise Profile & GSTIN", fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
                                Text("Shop legal name and tax registration number", fontSize = 11.sp, color = grayText)
                            }
                        }
                        Icon(if (isProjectExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = grayText)
                    }

                    if (isProjectExpanded) {
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedTextField(
                            value = shopName,
                            onValueChange = { shopName = it },
                            label = { Text("Enterprise Shop Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = BorderSilver
                            )
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedTextField(
                            value = gstNo,
                            onValueChange = { gstNo = it },
                            label = { Text("GSTIN Tax Identification") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = BorderSilver
                            )
                        )
                    }
                }
            }
        }

        // Section 2: Appearance & Theme Customization
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSilver, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isAppearanceExpanded = !isAppearanceExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(accentColor.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Palette, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Appearance & Brand Theme", fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
                                Text("Dark / Light mode and accent primary color", fontSize = 11.sp, color = grayText)
                            }
                        }
                        Icon(if (isAppearanceExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = grayText)
                    }

                    if (isAppearanceExpanded) {
                        Spacer(modifier = Modifier.height(14.dp))

                        // --- 2-COLUMN BRAND IDENTITY & CUSTOM LOGO MANAGER ---
                        val brandTheme by repository.activeBrandTheme.collectAsState()
                        val isRevoltActive = brandTheme == "revolt_crimson"
                        val customBlueLogo by repository.customBlueLogoUri.collectAsState()
                        val customRedLogo by repository.customRedLogoUri.collectAsState()
                        val isAdmin = activeRole.equals("admin", ignoreCase = true)

                        val blueLogoPickerLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.PickVisualMedia()
                        ) { uri: Uri? ->
                            if (uri != null) {
                                val savedPath = repository.saveCustomLogoFromUri(context, uri, isRevolt = false)
                                if (savedPath != null) {
                                    Toast.makeText(context, "Custom Blue ReLec Logo saved & applied everywhere!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }

                        val redLogoPickerLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.PickVisualMedia()
                        ) { uri: Uri? ->
                            if (uri != null) {
                                val savedPath = repository.saveCustomLogoFromUri(context, uri, isRevolt = true)
                                if (savedPath != null) {
                                    Toast.makeText(context, "Custom Red RE-VOLT Logo saved & applied everywhere!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Failed to save image", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }


                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                "Brand Themes & App Icon",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                "Select a theme below to automatically update the visual styling, icons across all screens, and the launcher icon on your phone's home screen.",
                                fontSize = 12.sp,
                                color = grayText,
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // --- 2 COLUMNS: BLUE RELEC & RED REVOLT ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // ===== COLUMN 1: BLUE THEME (ReLec) =====
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(
                                        width = if (!isRevoltActive) 2.dp else 1.dp,
                                        color = if (!isRevoltActive) ElectricBlue else BorderSilver.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        if (isRevoltActive) {
                                            repository.setBrandTheme("cyber_blue")
                                            onAccentColorChange(ElectricBlue)
                                            ReLectRepository.updateLauncherIcon(context, false)
                                            Toast.makeText(context, "ReLec Blue Theme & Home Screen Icon Applied!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (!isRevoltActive) ElectricBlue.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Card Title & Active Pill
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "ReLec Blue",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = textColor
                                        )
                                        if (!isRevoltActive) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(ElectricBlue)
                                                    .padding(horizontal = 6.dp, vertical = 2.5.dp)
                                            ) {
                                                Text(
                                                    "ACTIVE",
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.Black
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        "Cyber Wave",
                                        fontSize = 11.sp,
                                        color = grayText,
                                        modifier = Modifier.fillMaxWidth().padding(top = 1.dp)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Logo Display Box
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.White)
                                            .border(1.5.dp, if (!isRevoltActive) ElectricBlue else BorderSilver.copy(alpha = 0.8f), RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        BrandLogoView(
                                            repository = repository,
                                            isRevolt = false,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(4.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = if (customBlueLogo != null) "✓ Custom Icon" else "Official Icon",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (customBlueLogo != null) Color(0xFF10B981) else grayText
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Theme Activation Button
                                    Button(
                                        onClick = {
                                            repository.setBrandTheme("cyber_blue")
                                            onAccentColorChange(ElectricBlue)
                                            ReLectRepository.updateLauncherIcon(context, false)
                                            Toast.makeText(context, "ReLec Blue Theme & Home Screen Icon Applied!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(36.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (!isRevoltActive) ElectricBlue else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (!isRevoltActive) Color.Black else textColor
                                        ),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            if (!isRevoltActive) Icons.Default.Check else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            if (!isRevoltActive) "Active" else "Select Blue",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (isAdmin) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedButton(
                                            onClick = {
                                                blueLogoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(32.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, if (!isRevoltActive) ElectricBlue.copy(alpha = 0.8f) else grayText.copy(alpha = 0.4f)),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.AddPhotoAlternate,
                                                contentDescription = null,
                                                tint = if (!isRevoltActive) ElectricBlue else grayText,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "Upload Icon",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = textColor
                                            )
                                        }

                                        if (customBlueLogo != null) {
                                            TextButton(
                                                onClick = {
                                                    repository.resetCustomLogo(isRevolt = false)
                                                    Toast.makeText(context, "Reset to default ReLec logo", Toast.LENGTH_SHORT).show()
                                                },
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Text("Reset to Default", fontSize = 9.5.sp, color = Color(0xFFEF4444))
                                            }
                                        }
                                    }
                                }
                            }

                            // ===== COLUMN 2: RED THEME (RE-VOLT) =====
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(
                                        width = if (isRevoltActive) 2.dp else 1.dp,
                                        color = if (isRevoltActive) Color(0xFFEF4444) else BorderSilver.copy(alpha = 0.6f),
                                        shape = RoundedCornerShape(16.dp)
                                    )
                                    .clickable {
                                        if (!isRevoltActive) {
                                            repository.setBrandTheme("revolt_crimson")
                                            onAccentColorChange(Color(0xFFEF4444))
                                            ReLectRepository.updateLauncherIcon(context, true)
                                            Toast.makeText(context, "RE-VOLT Red Theme & Home Screen Icon Applied!", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isRevoltActive) Color(0xFFEF4444).copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Card Title & Active Pill
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "RE-VOLT Red",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = textColor
                                        )
                                        if (isRevoltActive) {
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFEF4444))
                                                    .padding(horizontal = 6.dp, vertical = 2.5.dp)
                                            ) {
                                                Text(
                                                    "ACTIVE",
                                                    fontSize = 8.5.sp,
                                                    fontWeight = FontWeight.Black,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }

                                    Text(
                                        "Power Crimson",
                                        fontSize = 11.sp,
                                        color = grayText,
                                        modifier = Modifier.fillMaxWidth().padding(top = 1.dp)
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Logo Display Box
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(Color.Transparent)
                                            .border(1.5.dp, if (isRevoltActive) Color(0xFFEF4444) else BorderSilver.copy(alpha = 0.8f), RoundedCornerShape(14.dp)),
                                         contentAlignment = Alignment.Center
                                    ) {
                                        BrandLogoView(
                                            repository = repository,
                                            isRevolt = true,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(4.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Text(
                                        text = if (customRedLogo != null) "✓ Custom Icon" else "Official Icon",
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (customRedLogo != null) Color(0xFF10B981) else grayText
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Theme Activation Button
                                    Button(
                                        onClick = {
                                            repository.setBrandTheme("revolt_crimson")
                                            onAccentColorChange(Color(0xFFEF4444))
                                            ReLectRepository.updateLauncherIcon(context, true)
                                            Toast.makeText(context, "RE-VOLT Red Theme & Home Screen Icon Applied!", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(36.dp),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isRevoltActive) Color(0xFFEF4444) else MaterialTheme.colorScheme.surfaceVariant,
                                            contentColor = if (isRevoltActive) Color.White else textColor
                                        ),
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            if (isRevoltActive) Icons.Default.Check else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            if (isRevoltActive) "Active" else "Select Red",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    if (isAdmin) {
                                        Spacer(modifier = Modifier.height(6.dp))
                                        OutlinedButton(
                                            onClick = {
                                                redLogoPickerLauncher.launch(
                                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(32.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            border = BorderStroke(1.dp, if (isRevoltActive) Color(0xFFEF4444).copy(alpha = 0.8f) else grayText.copy(alpha = 0.4f)),
                                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.AddPhotoAlternate,
                                                contentDescription = null,
                                                tint = if (isRevoltActive) Color(0xFFEF4444) else grayText,
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                "Upload Icon",
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = textColor
                                            )
                                        }

                                        if (customRedLogo != null) {
                                            TextButton(
                                                onClick = {
                                                    repository.resetCustomLogo(isRevolt = true)
                                                    Toast.makeText(context, "Reset to default Revolt logo", Toast.LENGTH_SHORT).show()
                                                },
                                                contentPadding = PaddingValues(0.dp),
                                                modifier = Modifier.height(26.dp)
                                            ) {
                                                Text("Reset to Default", fontSize = 9.5.sp, color = Color(0xFFEF4444))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Unified Home Screen & App-wide Sync Status Card
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(accentColor.copy(alpha = 0.08f))
                                .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(accentColor.copy(alpha = 0.18f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sync,
                                        contentDescription = null,
                                        tint = accentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = if (isRevoltActive) "Outside Icon: RE-VOLT Red • App: ReVolt" else "Outside Icon: ReLec Blue • App: ReLec",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textColor
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Theme selection is instantly synced to your Android home screen icon, splash screen, and all ERP headers.",
                                        fontSize = 11.sp,
                                        color = grayText,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Dashboard Banner Text Overlay toggle switch
                        val showBannerText by repository.showBannerText.collectAsState()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable {
                                    val newShow = !showBannerText
                                    repository.setShowBannerText(newShow)
                                    Toast.makeText(
                                        context,
                                        if (newShow) "Banner text overlay enabled" else "Banner text overlay hidden (Artwork mode)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    if (showBannerText) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        "Dashboard Banner Text Overlay",
                                        color = textColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        if (showBannerText) "Show company name & tagline over top banner" else "Hide text (Show clean banner artwork)",
                                        fontSize = 10.5.sp,
                                        color = grayText
                                    )
                                }
                            }
                            Switch(
                                checked = showBannerText,
                                onCheckedChange = {
                                    repository.setShowBannerText(it)
                                    Toast.makeText(
                                        context,
                                        if (it) "Banner text overlay enabled" else "Banner text overlay hidden (Artwork mode)",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = accentColor
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Dark mode toggle tile
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                                .clickable { onThemeToggle() }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        if (isDarkTheme) "Dark Luxury Glass Mode" else "Light Studio Mode",
                                        color = textColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text("Switch interface appearance style", fontSize = 10.sp, color = grayText)
                                }
                            }
                            Switch(
                                checked = isDarkTheme,
                                onCheckedChange = { onThemeToggle() },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = accentColor
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text("Brand Accent Color", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val accents = listOf(
                                "Electric Blue" to ElectricBlue,
                                "Crimson Red" to Color(0xFFEF4444),
                                "Electric Orange" to ElectricOrange,
                                "Cyber Purple" to CyberPurple,
                                "Success Green" to NeonGreen
                            )

                            accents.forEach { (name, color) ->
                                val isSelected = accentColor == color
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) color else color.copy(alpha = 0.15f))
                                        .border(if (isSelected) 2.dp else 1.dp, if (isSelected) textColor else color.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                                        .clickable {
                                            onAccentColorChange(color)
                                            Toast.makeText(context, "$name applied!", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 10.dp, horizontal = 4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        name.split(" ").first(),
                                        fontSize = 11.sp,
                                        color = if (isSelected) Color.White else color,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Local Storage & Cloud Backup
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSilver, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isFirebaseExpanded = !isFirebaseExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(CyberPurple.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = CyberPurple, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Local Persistence & Cloud Sync", fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
                                Text("Offline-first Room DB with optional Firebase sync", fontSize = 11.sp, color = grayText)
                            }
                        }
                        Icon(if (isFirebaseExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = grayText)
                    }

                    if (isFirebaseExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "ReLec ERP operates offline-first via Android Room DB. To mirror records with Firebase Firestore, configure your Project ID below:",
                            fontSize = 11.sp,
                            color = grayText
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        var tempId by remember { mutableStateOf(firestoreProject) }
                        OutlinedTextField(
                            value = tempId,
                            onValueChange = { tempId = it },
                            label = { Text("Firestore Sync Project ID") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = BorderSilver
                            ),
                            trailingIcon = {
                                IconButton(onClick = {
                                    repository.setFirestoreProject(tempId)
                                    Toast.makeText(context, "Linked to $tempId and synced successfully!", Toast.LENGTH_SHORT).show()
                                }) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen)
                                }
                            }
                        )
                    }
                }
            }
        }

        // Section 4.5: SMS Gateway & Real-Time Messaging Backend
        item {
            val currentSmsProvider by repository.smsProvider.collectAsState()
            val currentFast2smsKey by repository.fast2smsApiKey.collectAsState()
            val currentTwilioSid by repository.twilioSid.collectAsState()
            val currentTwilioToken by repository.twilioToken.collectAsState()
            val currentTwilioSender by repository.twilioSender.collectAsState()
            val currentWebhookUrl by repository.customSmsWebhook.collectAsState()

            var selectedProvider by remember(currentSmsProvider) { mutableStateOf(currentSmsProvider) }
            var editFast2smsKey by remember(currentFast2smsKey) { mutableStateOf(currentFast2smsKey) }
            var editTwilioSid by remember(currentTwilioSid) { mutableStateOf(currentTwilioSid) }
            var editTwilioToken by remember(currentTwilioToken) { mutableStateOf(currentTwilioToken) }
            var editTwilioSender by remember(currentTwilioSender) { mutableStateOf(currentTwilioSender) }
            var editWebhookUrl by remember(currentWebhookUrl) { mutableStateOf(currentWebhookUrl) }

            var testPhoneNumber by remember { mutableStateOf("9812045612") }
            var testSendingStatus by remember { mutableStateOf<String?>(null) }
            var isSendingTestSms by remember { mutableStateOf(false) }

            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSilver, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isSmsGatewayExpanded = !isSmsGatewayExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0284C7).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.SendToMobile, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("SMS Gateway & Messaging Backend", fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
                                Text("Fast2SMS India, Twilio, Webhook & Real Phone SMS", fontSize = 11.sp, color = grayText)
                            }
                        }
                        Icon(if (isSmsGatewayExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = grayText)
                    }

                    if (isSmsGatewayExpanded) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            "Select the cloud SMS backend used to deliver OTP verification codes and invoice bills directly to mobile phones:",
                            fontSize = 11.5.sp,
                            color = grayText
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // Provider Selection Pills
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "fast2sms" to "Fast2SMS (India)",
                                "twilio" to "Twilio Cloud",
                                "webhook" to "Custom Webhook",
                                "native" to "Native SMS"
                            ).forEach { (id, label) ->
                                val isSelected = selectedProvider == id
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) accentColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                        .border(1.dp, if (isSelected) accentColor else Color.Transparent, RoundedCornerShape(8.dp))
                                        .clickable { selectedProvider = id }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) accentColor else textColor,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        when (selectedProvider) {
                            "fast2sms" -> {
                                Text(
                                    "Fast2SMS Quick OTP Gateway (Recommended for India +91 mobile numbers):",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = editFast2smsKey,
                                    onValueChange = { editFast2smsKey = it },
                                    label = { Text("Fast2SMS API Key (authorization header)") },
                                    placeholder = { Text("Enter Fast2SMS API Key") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                            }
                            "twilio" -> {
                                Text("Twilio Cloud SMS Account Credentials:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = editTwilioSid,
                                    onValueChange = { editTwilioSid = it },
                                    label = { Text("Twilio Account SID") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = editTwilioToken,
                                    onValueChange = { editTwilioToken = it },
                                    label = { Text("Twilio Auth Token") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                OutlinedTextField(
                                    value = editTwilioSender,
                                    onValueChange = { editTwilioSender = it },
                                    label = { Text("Twilio Sender Phone Number (e.g. +1234567890)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                            }
                            "webhook" -> {
                                Text("Custom HTTP Webhook SMS Endpoint:", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = textColor)
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = editWebhookUrl,
                                    onValueChange = { editWebhookUrl = it },
                                    label = { Text("Webhook URL (e.g. https://api.myserver.com/sms/send)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    singleLine = true
                                )
                            }
                            else -> {
                                Text(
                                    "Native Device SMS: Dispatches SMS using device SIM card via Android SmsManager or system Messages application.",
                                    fontSize = 11.sp,
                                    color = grayText
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Button(
                            onClick = {
                                repository.saveSmsGatewayConfig(
                                    provider = selectedProvider,
                                    fast2smsKey = editFast2smsKey.trim(),
                                    twilioSidVal = editTwilioSid.trim(),
                                    twilioTokenVal = editTwilioToken.trim(),
                                    twilioSenderVal = editTwilioSender.trim(),
                                    webhookUrl = editWebhookUrl.trim()
                                )
                                Toast.makeText(context, "SMS Gateway configuration saved & applied!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(40.dp)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save Gateway Configuration", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        HorizontalDivider(color = BorderSilver)
                        Spacer(modifier = Modifier.height(12.dp))

                        // Real-Time Test SMS Dispatcher
                        Text("Live SMS Dispatch Test:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = testPhoneNumber,
                                onValueChange = { testPhoneNumber = it },
                                label = { Text("Mobile Number") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    isSendingTestSms = true
                                    testSendingStatus = "Dispatching..."
                                    val testOtp = String.format("%06d", (100000..999999).random())
                                    val isCurrentRevolt = repository.activeBrandTheme.value == "revolt_crimson"
                                    com.example.util.VerificationNotificationHelper.sendAutomaticSms(
                                        context = context,
                                        phone = testPhoneNumber.trim(),
                                        otpCode = testOtp,
                                        isRevolt = isCurrentRevolt
                                    ) { result ->
                                        isSendingTestSms = false
                                        testSendingStatus = if (result.success) {
                                            "✓ Sent via ${result.provider}"
                                        } else {
                                            "Error: ${result.errorMessage ?: "Failed"}"
                                        }
                                        Toast.makeText(context, "Test SMS ($testOtp) dispatched to $testPhoneNumber!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isSendingTestSms && testPhoneNumber.trim().length >= 10,
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.height(54.dp)
                            ) {
                                Text("Send Test SMS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        if (!testSendingStatus.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Status: $testSendingStatus",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (testSendingStatus?.startsWith("✓") == true) NeonGreen else MaterialTheme.colorScheme.error
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = {
                                val testOtp = String.format("%06d", (100000..999999).random())
                                val isCurrentRevolt = repository.activeBrandTheme.value == "revolt_crimson"
                                com.example.util.VerificationNotificationHelper.openSmsAppWithPrefilledOtp(
                                    context = context,
                                    phone = testPhoneNumber.trim(),
                                    otpCode = testOtp,
                                    isRevolt = isCurrentRevolt
                                )
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(38.dp)
                        ) {
                            Icon(Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Open in Phone's Messages App (smsto:)", fontSize = 11.5.sp)
                        }
                    }
                }
            }
        }

        // Section 5: Hardware & System Diagnostics
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSilver, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isPermissionsExpanded = !isPermissionsExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(ElectricBlue.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Sensors, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Hardware Diagnostics & Audio", fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
                                Text("Sensor permissions and speech-to-text language", fontSize = 11.sp, color = grayText)
                            }
                        }
                        Icon(if (isPermissionsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = grayText)
                    }

                    if (isPermissionsExpanded) {
                        Spacer(modifier = Modifier.height(14.dp))

                        val cameraGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                            context, android.Manifest.permission.CAMERA
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                        val recordGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                            context, android.Manifest.permission.RECORD_AUDIO
                        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (cameraGranted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (cameraGranted) NeonGreen else NeonRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Camera Hardware (QR Barcode Scanner)", fontSize = 12.sp, color = textColor)
                            }
                            Text(if (cameraGranted) "ONLINE" else "OFFLINE", fontSize = 10.sp, color = if (cameraGranted) NeonGreen else NeonRed, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (recordGranted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = null,
                                    tint = if (recordGranted) NeonGreen else NeonRed,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Microphone Hardware (Voice AI Engine)", fontSize = 12.sp, color = textColor)
                            }
                            Text(if (recordGranted) "ONLINE" else "OFFLINE", fontSize = 10.sp, color = if (recordGranted) NeonGreen else NeonRed, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Speech Recognition Locale:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Spacer(modifier = Modifier.height(8.dp))

                        var selectedLang by remember { mutableStateOf("Hinglish") }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            listOf("Hinglish (Default)", "English", "Punjabi").forEach { lang ->
                                val active = selectedLang == lang
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (active) accentColor else MaterialTheme.colorScheme.surfaceVariant)
                                        .clickable {
                                            selectedLang = lang
                                            Toast.makeText(context, "$lang speech recognition selected", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        lang.split(" ").first(),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (active) Color.White else textColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 6: Database Maintenance & Demo Sandbox
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, BorderSilver, RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isDbMaintenanceExpanded = !isDbMaintenanceExpanded },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(NeonRed.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = NeonRed, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Database Sandbox Maintenance", fontWeight = FontWeight.Bold, color = textColor, fontSize = 14.sp)
                                Text("Purge demo toy entries or restore sample data", fontSize = 11.sp, color = grayText)
                            }
                        }
                        Icon(if (isDbMaintenanceExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, tint = grayText)
                    }

                    if (isDbMaintenanceExpanded) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            "Clear simulated sandbox data to prepare the database for live electrical customer entries, or restore sample records to explore capabilities.",
                            fontSize = 11.sp,
                            color = grayText
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    repository.clearAllToyData()
                                    Toast.makeText(context, "Database cleared. Ready for live inputs!", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonRed, contentColor = Color.White),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Clear Data", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    repository.restoreDemoData()
                                    Toast.makeText(context, "Sample sandbox data restored!", Toast.LENGTH_LONG).show()
                                },
                                border = BorderStroke(1.dp, accentColor),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Restore Demo", fontWeight = FontWeight.Bold, color = textColor, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- MODULE 10: SIMPLIFIED WORKER ASSIGNED JOBS PORTAL WITH ATTENDANCE ---
@Composable
fun WorkerDashboardView(
    repository: ReLectRepository,
    accentColor: Color,
    isDarkTheme: Boolean
) {
    val services by repository.services.collectAsState()
    val attendanceRecords by repository.attendance.collectAsState()
    val activeWorkerJobs = remember(services) {
        services.filter { it.assignedWorkerId == "W1" } // Pre-assigned to our default logged-in tech (Sukhvinder)
    }

    val context = LocalContext.current
    val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfTime = SimpleDateFormat("hh:mm a", Locale.getDefault())
    val todayDate = sdfDate.format(Date())

    // Sukhvinder's attendance
    val workerAttendance = remember(attendanceRecords) {
        attendanceRecords.filter { it.workerId == "W1" }
    }

    val todayRecord = remember(workerAttendance, todayDate) {
        workerAttendance.find { it.date == todayDate }
    }

    val isClockedIn = todayRecord != null && todayRecord.clockOutTime.isNullOrBlank()
    val isClockedOut = todayRecord != null && todayRecord.clockOutTime?.isNotBlank() == true

    var selectedWorkerTab by remember { mutableStateOf(0) } // 0: My Jobs, 1: Available Unassigned
    var jobToComplete by remember { mutableStateOf<ServiceRequest?>(null) }
    var completionRemarks by remember { mutableStateOf("") }

    val unassignedJobs = remember(services) {
        services.filter { (it.assignedWorkerId == null || it.assignedWorkerName == null) && it.status == ServiceStatus.CREATED }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Worker Identity & Interactive Attendance Punch Clock Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.2.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Engineering, contentDescription = null, tint = accentColor, modifier = Modifier.size(24.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    "Sukhvinder Singh", 
                                    fontWeight = FontWeight.Bold, 
                                    color = MaterialTheme.colorScheme.onSurface, 
                                    fontSize = 16.sp
                                )
                                Text(
                                    "Field Technician • W1", 
                                    color = MaterialTheme.colorScheme.onSurfaceVariant, 
                                    fontSize = 12.sp
                                )
                            }
                        }

                        // Shift Status Badge
                        val shiftBg = if (isClockedIn) Color(0xFFDCFCE7) else if (isClockedOut) Color(0xFFF1F5F9) else Color(0xFFFEF3C7)
                        val shiftTextColor = if (isClockedIn) Color(0xFF16A34A) else if (isClockedOut) Color(0xFF475569) else Color(0xFFD97706)
                        val shiftLabel = if (isClockedIn) "ON DUTY" else if (isClockedOut) "SHIFT ENDED" else "OFF DUTY"

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(shiftBg)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                shiftLabel,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = shiftTextColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Punch Clock Action Row
                    if (!isClockedIn && !isClockedOut) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Shift not started today",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Button(
                                onClick = {
                                    val newRecord = AttendanceRecord(
                                        workerId = "W1",
                                        workerName = "Sukhvinder Singh",
                                        clockInTime = sdfTime.format(Date()),
                                        clockOutTime = null,
                                        date = todayDate,
                                        isClockedIn = true
                                    )
                                    repository.addAttendanceRecord(newRecord)
                                    Toast.makeText(context, "Clocked in successfully at ${newRecord.clockInTime}!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A), contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Login, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Punch In (Start Shift)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    } else if (isClockedIn) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    "Clocked in at ${todayRecord?.clockInTime}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF16A34A)
                                )
                                Text("Working on assigned field jobs", fontSize = 10.5.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(
                                onClick = {
                                    if (todayRecord != null) {
                                        repository.updateAttendanceRecord(
                                            todayRecord.copy(
                                                clockOutTime = sdfTime.format(Date()),
                                                isClockedIn = false
                                            )
                                        )
                                        Toast.makeText(context, "Clocked out successfully. Great work!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626), contentColor = Color.White),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Punch Out", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Shift Completed: In ${todayRecord?.clockInTime} • Out ${todayRecord?.clockOutTime}",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }

        // Job Filter Tabs: My Assigned Jobs vs Available Unassigned
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedWorkerTab == 0) accentColor else Color.Transparent)
                        .clickable { selectedWorkerTab = 0 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "My Tasks (${activeWorkerJobs.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedWorkerTab == 0) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (selectedWorkerTab == 1) accentColor else Color.Transparent)
                        .clickable { selectedWorkerTab = 1 }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "Counter Bookings (${unassignedJobs.size})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selectedWorkerTab == 1) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        val displayJobs = if (selectedWorkerTab == 0) activeWorkerJobs else unassignedJobs

        if (displayJobs.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp), 
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (selectedWorkerTab == 0) "No active jobs currently assigned to you." else "No pending counter bookings right now.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        } else {
            items(displayJobs) { req ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(14.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                req.category, 
                                fontWeight = FontWeight.Bold, 
                                color = MaterialTheme.colorScheme.onSurface, 
                                fontSize = 15.sp
                            )
                            val statusColor = when (req.status) {
                                ServiceStatus.CREATED -> Color.LightGray
                                ServiceStatus.ASSIGNED -> Color(0xFFF59E0B)
                                ServiceStatus.IN_PROGRESS -> accentColor
                                ServiceStatus.COMPLETED -> Color(0xFF16A34A)
                                ServiceStatus.CLOSED -> Color.Gray
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(statusColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    req.status.label, 
                                    color = statusColor, 
                                    fontWeight = FontWeight.Bold, 
                                    fontSize = 10.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(req.problemDescription, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                        Spacer(modifier = Modifier.height(6.dp))

                        // Customer Information Box
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Customer: ${req.customerName}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("Site: ${req.customerAddress}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            // Quick Call & WhatsApp Customer Action Buttons
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                IconButton(
                                    onClick = {
                                        try {
                                            val dialIntent = android.content.Intent(
                                                android.content.Intent.ACTION_DIAL,
                                                android.net.Uri.parse("tel:${req.customerPhone}")
                                            )
                                            context.startActivity(dialIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Dialer unavailable", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0284C7).copy(alpha = 0.15f))
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = "Call Customer", tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                                }

                                IconButton(
                                    onClick = {
                                        try {
                                            val cleanPhone = req.customerPhone.filter { it.isDigit() }
                                            val waIntent = android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse("https://api.whatsapp.com/send?phone=91$cleanPhone&text=Hello%20${req.customerName},%20I%20am%20Sukhvinder%20from%20Rattan%20Electricals%20regarding%20your%20service%20request.")
                                            )
                                            context.startActivity(waIntent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "WhatsApp unavailable", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF25D366).copy(alpha = 0.15f))
                                ) {
                                    Icon(Icons.Default.Chat, contentDescription = "WhatsApp Customer", tint = Color(0xFF25D366), modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(vertical = 12.dp))

                        // Workflow Actions Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (selectedWorkerTab == 1) {
                                // Accept / Claim Job
                                Button(
                                    onClick = {
                                        repository.updateServiceRequest(
                                            req.copy(
                                                assignedWorkerId = "W1",
                                                assignedWorkerName = "Sukhvinder Singh",
                                                status = ServiceStatus.ASSIGNED
                                            )
                                        )
                                        Toast.makeText(context, "Job accepted & added to your active queue!", Toast.LENGTH_SHORT).show()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Claim & Accept This Job", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            } else {
                                // Photo upload slot
                                var localPhoto by remember { mutableStateOf<String?>(null) }
                                OutlinedButton(
                                    onClick = {
                                        localPhoto = "uploaded"
                                        Toast.makeText(context, "Site photo attached to ticket!", Toast.LENGTH_SHORT).show()
                                    },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.AddAPhoto, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (localPhoto != null) "Photo Added" else "Add Photo", 
                                        fontSize = 11.sp, 
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (req.status == ServiceStatus.ASSIGNED) {
                                        Button(
                                            onClick = {
                                                repository.updateServiceRequest(req.copy(status = ServiceStatus.IN_PROGRESS))
                                                Toast.makeText(context, "Started work! Customer & Admin alerted.", Toast.LENGTH_SHORT).show()
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Start Work (On Site)", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    } else if (req.status == ServiceStatus.IN_PROGRESS) {
                                        Button(
                                            onClick = {
                                                jobToComplete = req
                                                completionRemarks = ""
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A), contentColor = Color.White),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Complete Job", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                        }
                                    } else if (req.status == ServiceStatus.COMPLETED) {
                                        Text(
                                            "✓ Finished",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF16A34A)
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

    // Work Completion & Remarks Dialog
    if (jobToComplete != null) {
        val target = jobToComplete!!
        AlertDialog(
            onDismissRequest = { jobToComplete = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Complete Service Job", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Please enter the work done remarks. This will be automatically reflected in the Admin portal and sent to the customer:",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = completionRemarks,
                        onValueChange = { completionRemarks = it },
                        placeholder = { Text("e.g., Capacitor replaced, motor checked & running normally.", fontSize = 12.sp) },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val updatedHistory = target.statusHistory + StatusChange(
                            status = ServiceStatus.COMPLETED,
                            timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date()),
                            notes = completionRemarks.ifBlank { "Work completed successfully on site" }
                        )
                        repository.updateServiceRequest(
                            target.copy(
                                status = ServiceStatus.COMPLETED,
                                statusHistory = updatedHistory
                            )
                        )
                        jobToComplete = null
                        Toast.makeText(context, "Job marked as completed! Admin & Customer updated.", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A), contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Submit Completion", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { jobToComplete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// --- HELPER FOR OPENING PDF IN DRIVE / PDF VIEWER ---
fun exportAndOpenInDrive(context: android.content.Context, bill: Bill) {
    val pdfDocument = android.graphics.pdf.PdfDocument()
    val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(595, 842, 1).create()
    val page = pdfDocument.startPage(pageInfo)
    val canvas = page.canvas

    val textPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 11f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
    }
    
    val boldPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.BLACK
        textSize = 11f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    val headerPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(11, 25, 44)
        textSize = 20f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    val accentPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(0, 173, 181)
        textSize = 14f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    var y = 120f

    val bannerPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(11, 25, 44)
    }
    canvas.drawRect(20f, 20f, 575f, 90f, bannerPaint)

    headerPaint.color = android.graphics.Color.WHITE
    canvas.drawText("RATTAN ELECTRICALS", 35f, 55f, headerPaint)
    
    val subtitlePaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(0, 173, 181)
        textSize = 11f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
    }
    canvas.drawText("Premium Repair Retails, Spares & Enterprise ERP Portal", 35f, 75f, subtitlePaint)

    canvas.drawText("TAX INVOICE / BILL OF SUPPLY", 35f, y, accentPaint)
    y += 24f
    
    canvas.drawText("Invoice ID: RE-BILL-${System.currentTimeMillis() % 100000}", 35f, y, boldPaint)
    y += 18f
    canvas.drawText("Date: ${bill.date}", 35f, y, textPaint)
    y += 18f
    canvas.drawText("Customer: ${bill.customerName}", 35f, y, textPaint)
    y += 18f
    canvas.drawText("Phone: +91 ${bill.customerPhone}", 35f, y, textPaint)
    y += 30f

    val tableHeaderPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(240, 240, 240)
    }
    canvas.drawRect(35f, y - 18f, 560f, y + 8f, tableHeaderPaint)
    
    canvas.drawText("Item Name", 45f, y, boldPaint)
    canvas.drawText("Qty", 350f, y, boldPaint)
    canvas.drawText("Price (INR)", 420f, y, boldPaint)
    canvas.drawText("Total (INR)", 490f, y, boldPaint)
    
    y += 24f

    bill.items.forEach { item ->
        canvas.drawText(item.name, 45f, y, textPaint)
        canvas.drawText(item.qty.toString(), 350f, y, textPaint)
        canvas.drawText(String.format("%.2f", item.rate), 420f, y, textPaint)
        canvas.drawText(String.format("%.2f", item.total), 490f, y, textPaint)
        y += 22f
    }

    y += 10f
    canvas.drawLine(35f, y, 560f, y, textPaint)
    y += 20f

    val subtotalParts = bill.items.sumOf { item -> item.total }
    val labor = bill.laborCharges
    val grossTotal = subtotalParts + labor
    val discount = bill.discountAmount
    val taxableAmount = maxOf(0.0, grossTotal - discount)
    val gstTax = taxableAmount * (bill.gstPercent / 100.0)
    val grandTotal = bill.totalAmount

    canvas.drawText("Subtotal Parts:", 330f, y, textPaint)
    canvas.drawText(String.format("₹%.2f", subtotalParts), 490f, y, boldPaint)
    y += 18f

    canvas.drawText("Labor / Service Charges:", 330f, y, textPaint)
    canvas.drawText(String.format("₹%.2f", labor), 490f, y, boldPaint)
    y += 18f

    if (discount > 0.0) {
        canvas.drawText("Discount Applied:", 330f, y, textPaint)
        canvas.drawText(String.format("-₹%.2f", discount), 490f, y, boldPaint)
        y += 18f
    }

    if (bill.gstPercent > 0.0) {
        canvas.drawText("Integrated GST (${String.format("%.1f", bill.gstPercent)}%):", 330f, y, textPaint)
        canvas.drawText(String.format("₹%.2f", gstTax), 490f, y, boldPaint)
    } else {
        canvas.drawText("GST (0% - Tax Exempt):", 330f, y, textPaint)
        canvas.drawText("₹0.00", 490f, y, boldPaint)
    }
    y += 24f

    val totalBannerPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(224, 247, 250)
    }
    canvas.drawRect(320f, y - 18f, 560f, y + 8f, totalBannerPaint)
    
    val grandTotalPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(11, 25, 44)
        textSize = 12f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    canvas.drawText("GRAND TOTAL (INR):", 330f, y, grandTotalPaint)
    canvas.drawText(String.format("₹%.2f", grandTotal), 490f, y, grandTotalPaint)
    
    y += 45f
    
    val footerPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.GRAY
        textSize = 9f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.ITALIC)
    }
    canvas.drawText("Terms & Conditions:", 35f, y, boldPaint)
    y += 14f
    canvas.drawText("1. Goods once sold will not be returned or exchanged.", 35f, y, footerPaint)
    y += 12f
    canvas.drawText("2. All disputes are subject to local jurisdiction only.", 35f, y, footerPaint)
    y += 12f
    canvas.drawText("3. Electrical items carry warranty as per manufacturers policy only.", 35f, y, footerPaint)
    
    y += 40f
    val centerFooterPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.rgb(11, 25, 44)
        textSize = 11f
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    canvas.drawText("Thank you for choosing Rattan Electricals!", 160f, y, centerFooterPaint)

    pdfDocument.finishPage(page)

    val filename = "RE_Invoice_${System.currentTimeMillis()}.pdf"
    var outputStream: java.io.OutputStream? = null
    var uri: android.net.Uri? = null
    
    try {
        val resolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
        }
        val contentUri = if (android.os.Build.VERSION.SDK_INT >= 29) {
            android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            android.net.Uri.parse("content://media/external/file")
        }
        uri = resolver.insert(contentUri, contentValues)
        if (uri != null) {
            outputStream = resolver.openOutputStream(uri)
            if (outputStream != null) {
                pdfDocument.writeTo(outputStream)
                Toast.makeText(context, "PDF Invoice Saved to Downloads!", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "PDF creation failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    } finally {
        try { outputStream?.close() } catch (ex: java.lang.Exception) {}
        pdfDocument.close()
    }

    if (uri != null) {
        try {
            val viewIntent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(android.content.Intent.createChooser(viewIntent, "Open Invoice in Google Drive / PDF Viewer:"))
        } catch (e: Exception) {
            Toast.makeText(context, "No PDF Viewer application found. Excel/PDF is stored in your Downloads directory.", Toast.LENGTH_LONG).show()
        }
    }
}

// --- HELPER FOR EXCEL / CSV EXPORT ---
fun exportAndShareBillExcel(context: android.content.Context, bill: Bill) {
    val csvContent = buildString {
        append("RATTAN ELECTRICALS TAX INVOICE\n")
        append("Invoice Date,${bill.date}\n")
        append("Customer Name,${bill.customerName}\n")
        append("Customer Phone,${bill.customerPhone}\n\n")
        
        append("Item Name,Quantity,Unit Price (INR),Total Price (INR)\n")
        bill.items.forEach { item ->
            append("\"${item.name}\",${item.qty},${item.rate},${item.total}\n")
        }
        
        append("\n")
        val subParts = bill.items.sumOf { it.total }
        val gross = subParts + bill.laborCharges
        val disc = bill.discountAmount
        val taxable = maxOf(0.0, gross - disc)
        val gstTax = taxable * (bill.gstPercent / 100.0)
        append("Subtotal (Parts),,,${subParts}\n")
        append("Labor Charges,,,${bill.laborCharges}\n")
        if (disc > 0.0) {
            append("Discount Applied,,,-${disc}\n")
        }
        append("Taxable Amount,,,${taxable}\n")
        append("GST (${bill.gstPercent}%),,,${gstTax}\n")
        append("GRAND TOTAL (INR),,,${bill.totalAmount}\n")
    }

    val filename = "RE_Invoice_${System.currentTimeMillis()}.csv"
    var outputStream: java.io.OutputStream? = null
    var uri: android.net.Uri? = null

    try {
        val resolver = context.contentResolver
        val contentValues = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, filename)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/comma-separated-values")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
        }
        val contentUri = if (android.os.Build.VERSION.SDK_INT >= 29) {
            android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI
        } else {
            android.net.Uri.parse("content://media/external/file")
        }
        uri = resolver.insert(contentUri, contentValues)
        if (uri != null) {
            outputStream = resolver.openOutputStream(uri)
            if (outputStream != null) {
                outputStream.write(csvContent.toByteArray(Charsets.UTF_8))
                Toast.makeText(context, "Excel CSV Saved to Downloads!", Toast.LENGTH_SHORT).show()
            }
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Excel CSV saving failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
    } finally {
        try { outputStream?.close() } catch (ex: java.lang.Exception) {}
    }

    if (uri != null) {
        val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/comma-separated-values"
            putExtra(android.content.Intent.EXTRA_SUBJECT, "Rattan Electricals Tax Invoice Excel")
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(shareIntent, "Save or Send Invoice Excel CSV via:"))
    }
}

// --- GLOBAL AI SCANNED PRODUCT DIALOG ---
@Composable
fun GlobalScannerDialog(
    repository: ReLectRepository,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val isLight = MaterialTheme.colorScheme.surface == Color.White
    val textColor = if (isLight) Color(0xFF111827) else Color.White
    val grayText = if (isLight) Color(0xFF4B5563) else Color.Gray
    val modalBg = if (isLight) Color.White else Color(0xFF0F172A)
    
    var hasCameraPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            Toast.makeText(context, "Camera permission denied. Simulator mode enabled.", Toast.LENGTH_SHORT).show()
        }
    }
    
    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(android.Manifest.permission.CAMERA)
        }
    }
    
    var isProcessing by remember { mutableStateOf(false) }
    var scannedResult by remember { mutableStateOf<ReLectRepository.ParsedVoiceData?>(null) }
    var inputQuantity by remember { mutableStateOf("10") }
    var barcodeValue by remember { mutableStateOf("") }
    
    var capturedBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            capturedBitmap = bitmap
            isProcessing = true
            scope.launch {
                try {
                    val parsed = GeminiClient.parseBarcodeImage(bitmap)
                    scannedResult = parsed
                    barcodeValue = "BAR-${System.currentTimeMillis() % 100000000}"
                    Toast.makeText(context, "Product Scanned and Parsed!", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Parsing error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isProcessing = false
                }
            }
        }
    }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
        containerColor = modalBg,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.QrCode, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI QR & Barcode Scanner", fontWeight = FontWeight.Bold, color = textColor, fontSize = 18.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = grayText)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Aim camera at an electrical product barcode or QR code on inventory spares to instantly update stock database.",
                    fontSize = 11.sp,
                    color = grayText
                )
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Black)
                        .border(1.dp, Color.Gray.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (hasCameraPermission && capturedBitmap == null) {
                        androidx.compose.ui.viewinterop.AndroidView(
                            factory = { ctx ->
                                val previewView = androidx.camera.view.PreviewView(ctx).apply {
                                    scaleType = androidx.camera.view.PreviewView.ScaleType.FILL_CENTER
                                }
                                val cameraProviderFuture = androidx.camera.lifecycle.ProcessCameraProvider.getInstance(ctx)
                                val executor = androidx.core.content.ContextCompat.getMainExecutor(ctx)
                                cameraProviderFuture.addListener({
                                    try {
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = androidx.camera.core.Preview.Builder().build().also {
                                            it.setSurfaceProvider(previewView.surfaceProvider)
                                        }
                                        val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
                                        cameraProvider.unbindAll()
                                        cameraProvider.bindToLifecycle(
                                            ctx as androidx.lifecycle.LifecycleOwner,
                                            cameraSelector,
                                            preview
                                        )
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }, executor)
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )
                        
                        Box(
                            modifier = Modifier
                                .size(120.dp)
                                .border(2.dp, accentColor, RoundedCornerShape(12.dp))
                        )
                        
                        val infiniteTransition = rememberInfiniteTransition(label = "scan")
                        val yOffset by infiniteTransition.animateFloat(
                            initialValue = -50f,
                            targetValue = 50f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(1500, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ),
                            label = "scanLine"
                        )
                        Box(
                            modifier = Modifier
                                .width(110.dp)
                                .height(2.dp)
                                .offset(y = yOffset.dp)
                                .background(NeonGreen)
                        )
                    } else if (capturedBitmap != null) {
                        Image(
                            bitmap = capturedBitmap!!.asImageBitmap(),
                            contentDescription = "Scanned photo",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = grayText, modifier = Modifier.size(40.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Camera access required for live scanning", fontSize = 11.sp, color = grayText)
                        }
                    }
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            if (hasCameraPermission) {
                                cameraLauncher.launch(null)
                            } else {
                                permissionLauncher.launch(android.Manifest.permission.CAMERA)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Capture Photo", fontWeight = FontWeight.Bold)
                    }
                    
                    if (capturedBitmap != null) {
                        OutlinedButton(
                            onClick = {
                                capturedBitmap = null
                                scannedResult = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color.Gray)
                        ) {
                            Text("Reset", color = textColor)
                        }
                    }
                }
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isLight) Color(0xFFF3F4F6) else Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Barcode Simulator (Select to test scanning):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = textColor)
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        val simulatedProducts = listOf(
                            Triple("Havells Wire Red", "Wires & Cables", "Havells India Ltd"),
                            Triple("Anchor 1Way Switch", "Switches & Sockets", "Anchor Panasonic"),
                            Triple("Syska 12W LED Bulb", "Switches & Sockets", "Syska LED"),
                            Triple("Orient Ceiling Fan 48\"", "Fans & Cooling", "Orient Electric"),
                            Triple("Luminous Single Pole MCB", "Switchgears", "Luminous Power")
                        )
                        
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            simulatedProducts.forEach { (name, cat, brand) ->
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(accentColor.copy(alpha = 0.15f))
                                        .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                                        .clickable {
                                            isProcessing = true
                                            scope.launch {
                                                kotlinx.coroutines.delay(600)
                                                scannedResult = ReLectRepository.ParsedVoiceData(
                                                    item = name,
                                                    quantity = 1,
                                                    amount = when(cat) {
                                                        "Wires & Cables" -> 1450.0
                                                        "Switches & Sockets" -> if(name.contains("LED")) 180.0 else 35.0
                                                        "Fans & Cooling" -> 2100.0
                                                        "Switchgears" -> 160.0
                                                        else -> 120.0
                                                    },
                                                    supplier = brand,
                                                    category = cat
                                                )
                                                barcodeValue = "BAR-${(1000000000 + (Math.random() * 9000000000).toLong())}"
                                                isProcessing = false
                                                Toast.makeText(context, "Simulated scanning of $name successful!", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(name, fontSize = 10.sp, color = accentColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
                
                if (isProcessing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = accentColor)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Decoding QR/Barcode via Gemini AI...", fontSize = 12.sp, color = textColor)
                    }
                }
                
                val result = scannedResult
                if (result != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    ) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = NeonGreen)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Decoded Successfully!", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            }
                            
                            HorizontalDivider(color = Color.DarkGray)
                            
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Item Name:", fontSize = 11.sp, color = Color.Gray)
                                Text(result.item, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Supplier:", fontSize = 11.sp, color = Color.Gray)
                                Text(result.supplier, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Category:", fontSize = 11.sp, color = Color.Gray)
                                Text(result.category, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Rate/Price:", fontSize = 11.sp, color = Color.Gray)
                                Text("₹%,.2f".format(result.amount), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                            }
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Barcode ID:", fontSize = 11.sp, color = Color.Gray)
                                Text(barcodeValue, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = accentColor)
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            
                            OutlinedTextField(
                                value = inputQuantity,
                                onValueChange = { inputQuantity = it },
                                label = { Text("Enter Quantity to Add") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = accentColor,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                modifier = Modifier.fillMaxWidth()
                            )
                            
                            Button(
                                onClick = {
                                    val qty = inputQuantity.toIntOrNull() ?: 1
                                    repository.addPurchaseRecord(
                                        PurchaseRecord(
                                            itemName = result.item,
                                            quantity = qty,
                                            supplierName = result.supplier,
                                            cost = result.amount * qty,
                                            date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                        )
                                    )
                                    Toast.makeText(context, "Saved! Inventory Updated: +$qty pcs of ${result.item}", Toast.LENGTH_LONG).show()
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save Directly to Database", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {}
    )
}

// --- GLOBAL AI SPEECH COMMAND CENTRE DIALOG ---
@Composable
fun AIVoiceEntryDialog(
    repository: ReLectRepository,
    accentColor: Color,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val isLight = MaterialTheme.colorScheme.surface == Color.White
    val textColor = if (isLight) Color(0xFF111827) else Color.White
    val grayText = if (isLight) Color(0xFF4B5563) else Color.Gray
    val modalBg = if (isLight) Color.White else Color(0xFF0F172A)
    
    var spokenText by remember { mutableStateOf("") }
    var isListening by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    
    var editItem by remember { mutableStateOf("") }
    var editQty by remember { mutableStateOf("1") }
    var editCost by remember { mutableStateOf("0.0") }
    var editSupplier by remember { mutableStateOf("Local Distributor") }
    var editCategory by remember { mutableStateOf("General") }
    
    val voiceLauncher = rememberSpeechRecognizerLauncher(
        onResult = { transcript ->
            spokenText = transcript
            if (transcript.isNotBlank()) {
                isProcessing = true
                scope.launch {
                    try {
                        val parsed = GeminiClient.parseVoiceInput(transcript)
                        editItem = parsed.item
                        editQty = parsed.quantity.toString()
                        editCost = parsed.amount.toString()
                        editSupplier = parsed.supplier.ifBlank { "Local Distributor" }
                        editCategory = parsed.category
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    } finally {
                        isProcessing = false
                    }
                }
            }
        },
        onStateChange = { listening, _ ->
            isListening = listening
        }
    )
    
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(24.dp)),
        containerColor = modalBg,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = accentColor, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Speech Command Desk", fontWeight = FontWeight.Bold, color = textColor, fontSize = 18.sp)
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = grayText)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Speak in Hindi, Punjabi, or English to record purchases or stock items (e.g. \"Havells wire ke 10 switches ₹250 me khareede\"). All fields will automatically fill!",
                    fontSize = 11.sp,
                    color = grayText
                )
                
                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isListening) NeonRed.copy(alpha = 0.15f) else accentColor.copy(alpha = 0.08f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            if (isListening) {
                                voiceLauncher.stop()
                            } else {
                                voiceLauncher()
                            }
                        }
                        .border(
                            1.dp,
                            if (isListening) NeonRed else accentColor.copy(alpha = 0.3f),
                            RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.StopCircle else Icons.Default.Mic,
                            contentDescription = if (isListening) "Stop" else "Mic",
                            tint = if (isListening) NeonRed else accentColor,
                            modifier = Modifier
                                .size(48.dp)
                                .then(if (isListening) Modifier.pulsingAnimation() else Modifier)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            if (isListening) "Listening... Tap to Stop / Pause" else "Tap here & Speak",
                            fontWeight = FontWeight.Bold,
                            color = textColor,
                            fontSize = 14.sp
                        )
                    }
                }
                
                if (spokenText.isNotBlank()) {
                    Text("Spoken Text:", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textColor)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            "\"$spokenText\"",
                            fontSize = 12.sp,
                            color = textColor,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
                
                if (isProcessing) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), color = accentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Gemini detecting entities...", fontSize = 11.sp, color = textColor)
                    }
                }
                
                Text("Extracted Database Fields (Verify & Edit):", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = textColor)
                
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editItem,
                        onValueChange = { editItem = it },
                        label = { Text("Product/Item Name") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, focusedTextColor = textColor, unfocusedTextColor = textColor)
                    )
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editQty,
                            onValueChange = { editQty = it },
                            label = { Text("Quantity") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, focusedTextColor = textColor, unfocusedTextColor = textColor)
                        )
                        OutlinedTextField(
                            value = editCost,
                            onValueChange = { editCost = it },
                            label = { Text("Cost / Price") },
                            modifier = Modifier.weight(1.5f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, focusedTextColor = textColor, unfocusedTextColor = textColor)
                        )
                    }
                    
                    OutlinedTextField(
                        value = editSupplier,
                        onValueChange = { editSupplier = it },
                        label = { Text("Supplier/Brand") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, focusedTextColor = textColor, unfocusedTextColor = textColor)
                    )
                    
                    OutlinedTextField(
                        value = editCategory,
                        onValueChange = { editCategory = it },
                        label = { Text("Category") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = accentColor, focusedTextColor = textColor, unfocusedTextColor = textColor)
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Button(
                    onClick = {
                        if (editItem.isNotBlank()) {
                            val qty = editQty.toIntOrNull() ?: 1
                            val cost = editCost.toDoubleOrNull() ?: 0.0
                            repository.addPurchaseRecord(
                                PurchaseRecord(
                                    itemName = editItem,
                                    quantity = qty,
                                    supplierName = editSupplier,
                                    cost = cost,
                                    date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                )
                            )
                            Toast.makeText(context, "Direct Entry Saved: $editItem ($qty pcs)!", Toast.LENGTH_LONG).show()
                            onDismiss()
                        } else {
                            Toast.makeText(context, "Please enter item details first!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonGreen, contentColor = Color.Black),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to SQLite Database", fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {}
    )
}
