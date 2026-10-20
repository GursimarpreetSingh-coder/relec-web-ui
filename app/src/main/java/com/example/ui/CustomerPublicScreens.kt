package com.example.ui

import com.example.data.auth.UserSession
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.*
import com.example.ui.theme.BorderSilver
import com.example.ui.theme.DarkGlassSurface
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonYellow
import android.widget.Toast
import kotlinx.coroutines.launch
import java.util.UUID
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.AsyncImage
import androidx.compose.ui.layout.ContentScale
import com.example.ui.components.BrandLogoView

@Composable
fun PremiumSegmentedControl(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    accentColor: Color
) {
    val isDark = MaterialTheme.colorScheme.surface != Color.White
    val containerBg = if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(containerBg)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        val tabs = listOf(
            "My Jobs" to Icons.Default.Handyman,
            "Book Repair" to Icons.Default.AddCircleOutline,
            "My Bills" to Icons.AutoMirrored.Filled.ReceiptLong,
            "Support" to Icons.Default.SupportAgent
        )
        tabs.forEachIndexed { index, (label, icon) ->
            val isSelected = selectedTab == index
            val bgSelectedColor = if (isSelected) accentColor else Color.Transparent
            val textSelectedColor = if (isSelected) {
                Color.White
            } else {
                if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
            }
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(13.dp))
                    .background(bgSelectedColor)
                    .clickable { onTabSelected(index) }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = textSelectedColor,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = label,
                        color = textSelectedColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 11.sp,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomerPublicPortal(
    repository: ReLectRepository,
    session: UserSession? = null,
    onNavigateBack: () -> Unit,
    accentColor: Color,
    isDarkTheme: Boolean,
    onThemeToggle: () -> Unit
) {
    val brandTheme by repository.activeBrandTheme.collectAsState()
    val isRevolt = brandTheme == "revolt_crimson"
    val brandName = if (isRevolt) "RE-VOLT ENTERPRISE" else "Rattan Electricals"

    // Default to the customer's phone if logged in
    var trackingPhoneSearch by remember { mutableStateOf(session?.phone ?: "9876543210") }
    var showTrackedList by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(0) } // 0: My Jobs, 1: Book Repair, 2: My Bills, 3: Support
    val bills by repository.bills.collectAsState()
    val services by repository.services.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(accentColor.copy(alpha = 0.12f))
                                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            BrandLogoView(
                                repository = repository,
                                isRevolt = isRevolt,
                                modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                brandName,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.5.sp,
                                letterSpacing = 0.5.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Customer Self-Service Portal",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    // Sign out button with confirmation
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = "Sign Out",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onThemeToggle,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = if (isDarkTheme) Icons.Default.WbSunny else Icons.Default.NightsStay,
                            contentDescription = "Toggle Theme",
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color.Transparent
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
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Customer verified banner - Compact, sleek layout with no vertical text stretching
            val customerName = if (!session?.displayName.isNullOrBlank()) session!!.displayName else "Valued Customer"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isDarkTheme) Color(0xFF1E293B).copy(alpha = 0.75f) else Color(0xFFF1F5F9)
                    )
                    .border(
                        1.dp,
                        if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(accentColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                customerName.take(1).uppercase(),
                                fontWeight = FontWeight.Black,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Welcome, $customerName",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "Phone: ${trackingPhoneSearch.ifBlank { "Not Set" }}",
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF10B981).copy(alpha = 0.15f))
                            .border(1.dp, Color(0xFF10B981).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            "CUSTOMER",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF10B981),
                            maxLines = 1,
                            softWrap = false,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Segmented 4-tab control
            PremiumSegmentedControl(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                accentColor = accentColor
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                AnimatedContent(
                    targetState = selectedTab, 
                    transitionSpec = {
                        fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200))
                    },
                    label = "customerTabChange",
                    modifier = Modifier.fillMaxSize()
                ) { tabIndex ->
                when (tabIndex) {
                    0 -> {
                        // TAB 0: My Service Requests Tracker
                        RequestTrackerView(
                            repository = repository,
                            phoneSearch = trackingPhoneSearch,
                            onPhoneChange = { trackingPhoneSearch = it },
                            showList = showTrackedList,
                            onShowListChange = { showTrackedList = it },
                            accentColor = accentColor
                        )
                    }
                    1 -> {
                        // TAB 1: Book New Service
                        RequestFormView(
                            repository = repository,
                            accentColor = accentColor,
                            defaultName = session?.displayName.orEmpty(),
                            defaultPhone = trackingPhoneSearch,
                            onSubmissionSuccess = { phone ->
                                trackingPhoneSearch = phone
                                selectedTab = 0
                                showTrackedList = true
                            }
                        )
                    }
                    2 -> {
                        // TAB 2: Customer Bills & Invoices
                        val myBills = remember(bills, trackingPhoneSearch) {
                            bills.filter { b ->
                                b.customerPhone.trim() == trackingPhoneSearch.trim() ||
                                (customerName.isNotBlank() && b.customerName.contains(customerName, ignoreCase = true))
                            }
                        }
                        
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(accentColor.copy(alpha = 0.1f))
                                        .border(1.dp, accentColor.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(
                                                "My Invoices & Receipts",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                "Digital invoices linked to ${trackingPhoneSearch.ifBlank { "your account" }}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            "${myBills.size} Invoices",
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 12.sp,
                                            color = accentColor
                                        )
                                    }
                                }
                            }

                            if (myBills.isEmpty()) {
                                item {
                                    Card(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF8FAFC)
                                        ),
                                        border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0))
                                    ) {
                                        Column(
                                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(
                                                Icons.AutoMirrored.Filled.ReceiptLong,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(40.dp)
                                            )
                                            Spacer(modifier = Modifier.height(10.dp))
                                            Text(
                                                "No Invoices Found",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "No bills registered under phone $trackingPhoneSearch yet.",
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(myBills) { bill ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(16.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White
                                        ),
                                        border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0))
                                    ) {
                                        Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        "Invoice #${bill.id.take(8).uppercase()}",
                                                        fontWeight = FontWeight.Black,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        bill.date,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFF10B981).copy(alpha = 0.15f))
                                                        .border(0.8.dp, Color(0xFF10B981).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                                        .padding(horizontal = 8.dp, vertical = 3.dp)
                                                ) {
                                                    Text(
                                                        "PAID",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF10B981)
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))
                                            HorizontalDivider(color = if (isDarkTheme) Color(0xFF334155) else Color(0xFFF1F5F9))
                                            Spacer(modifier = Modifier.height(10.dp))

                                            bill.items.forEach { itm ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        "${itm.name} (${itm.qty}x)",
                                                        fontSize = 11.5.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                    Text(
                                                        "₹${String.format("%,.0f", itm.total)}",
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    "Total Billed",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    "₹${String.format("%,.2f", bill.totalAmount)}",
                                                    fontWeight = FontWeight.Black,
                                                    fontSize = 16.sp,
                                                    color = accentColor
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    3 -> {
                        // TAB 3: Dynamic Store Support & Electrical Safety Guide (Admin Editable)
                        val supportInfo by repository.storeSupportInfo.collectAsState()
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 32.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White
                                    ),
                                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0))
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                        Text(
                                            supportInfo.storeName,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Reach our certified electrical technicians directly for urgent repairs or inquiries.",
                                            fontSize = 11.5.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(14.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    try {
                                                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                                                            data = android.net.Uri.parse("tel:${supportInfo.phone}")
                                                        }
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "Dialer: ${supportInfo.phone}", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Default.Phone, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Call Store", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }

                                            Button(
                                                onClick = {
                                                    try {
                                                        val cleanNum = supportInfo.whatsapp.replace("+", "").replace(" ", "").replace("-", "")
                                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                                                            data = android.net.Uri.parse("https://wa.me/$cleanNum")
                                                        }
                                                        context.startActivity(intent)
                                                    } catch (e: Exception) {
                                                        Toast.makeText(context, "WhatsApp: ${supportInfo.whatsapp}", Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                                                shape = RoundedCornerShape(12.dp)
                                            ) {
                                                Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("WhatsApp", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(14.dp))
                                        HorizontalDivider(color = if (isDarkTheme) Color(0xFF334155) else Color(0xFFF1F5F9))
                                        Spacer(modifier = Modifier.height(10.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                supportInfo.address,
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Schedule, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                supportInfo.workingHours,
                                                fontSize = 11.5.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        if (supportInfo.emergencyHelpline.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Emergency, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    "24/7 Helpline: ${supportInfo.emergencyHelpline}",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFFEF4444)
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            item {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isDarkTheme) Color(0xFF1E293B) else Color.White
                                    ),
                                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF334155) else Color(0xFFE2E8F0))
                                ) {
                                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Home Electrical Safety Precautions",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.5.sp,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(10.dp))
                                        val safetyPoints = remember(supportInfo.safetyGuidelines) {
                                            supportInfo.safetyGuidelines.lines().map { it.trim() }.filter { it.isNotBlank() }
                                        }
                                        safetyPoints.forEach { pt ->
                                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)) {
                                                Text("⚡", fontSize = 11.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(pt, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
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
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RequestFormView(
    repository: ReLectRepository,
    accentColor: Color,
    defaultName: String = "",
    defaultPhone: String = "",
    onSubmissionSuccess: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var name by remember(defaultName) { mutableStateOf(defaultName) }
    var phone by remember(defaultPhone) { mutableStateOf(defaultPhone) }
    var address by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Wiring Check") }
    var priority by remember { mutableStateOf(Priority.MEDIUM) }
    var problemDescription by remember { mutableStateOf("") }
    var preferredDate by remember { mutableStateOf("2026-07-02 10:00 AM") }
    var selectedPhoto by remember { mutableStateOf<String?>(null) }
    var showDialog by remember { mutableStateOf(false) }
    var showPhotoSourceDialog by remember { mutableStateOf(false) }

    // AI Voice Booking States
    var isListening by remember { mutableStateOf(false) }
    var speechError by remember { mutableStateOf<String?>(null) }
    var voiceTranscript by remember { mutableStateOf("") }
    var isProcessingVoice by remember { mutableStateOf(false) }

    val voiceLauncher = rememberSpeechRecognizerLauncher(
        onResult = { text: String ->
            voiceTranscript = text
            if (text.isNotBlank()) {
                isProcessingVoice = true
                scope.launch {
                    try {
                        val result = GeminiClient.parseServiceRequestVoice(text)
                        if (result.name.isNotBlank() && result.name != "Walk-in Customer") name = result.name
                        if (result.phone.isNotBlank() && result.phone != "9872134590") phone = result.phone
                        if (result.address.isNotBlank() && result.address != "Shop Address") address = result.address
                        problemDescription = if (result.problem.isNotBlank()) result.problem else text
                        if (result.category.isNotBlank()) category = result.category
                        Toast.makeText(context, "Voice input fed: ${result.category}!", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        problemDescription = text
                        Toast.makeText(context, "Voice inserted into description", Toast.LENGTH_SHORT).show()
                    } finally {
                        isProcessingVoice = false
                    }
                }
            }
        },
        onStateChange = { listening: Boolean, errorMsg: String? ->
            isListening = listening
            if (errorMsg != null) {
                speechError = errorMsg
            }
        }
    )

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            selectedPhoto = uri.toString()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: android.graphics.Bitmap? ->
        if (bitmap != null) {
            try {
                val cacheFile = java.io.File(context.cacheDir, "fault_${UUID.randomUUID()}.jpg")
                val out = java.io.FileOutputStream(cacheFile)
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, out)
                out.flush()
                out.close()
                selectedPhoto = android.net.Uri.fromFile(cacheFile).toString()
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
        }
    }

    val categories = listOf("Wiring Check", "Fan Installation", "Motor Repair", "DB Board Repair", "Lighting Fittings", "Inverter Setup")

    val isDark = MaterialTheme.colorScheme.surface != Color.White
    val inputContainerBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.5f) else Color.White
    val borderStrokeColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    val inputTextColor = if (isDark) Color.White else Color(0xFF0F172A)
    val inputContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val inputLabelColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.06f)),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, accentColor.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    Text(
                        "Submit Electric Service Request",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) Color.White else Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "No registration or login required. Fill brief details below to instantly route an on-duty technician to your doorstep.",
                        fontSize = 12.5.sp,
                        lineHeight = 17.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                    )
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (isListening) Color(0xFF020617) else accentColor.copy(alpha = 0.05f)
                ),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        BorderStroke(
                            1.dp,
                            if (isListening) NeonGreen else accentColor.copy(alpha = 0.2f)
                        ),
                        RoundedCornerShape(24.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isListening) NeonGreen.copy(alpha = 0.2f) else accentColor.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.Psychology,
                                    contentDescription = null,
                                    tint = if (isListening) NeonGreen else accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    "Accurate AI Voice Booking",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Speak in Punjabi, Hindi, or English",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isListening) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFEF4444).copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "LISTENING",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    if (isProcessingVoice) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = accentColor, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Gemini AI extracting name, phone, address, and category...",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        if (voiceTranscript.isNotBlank()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.Gray.copy(alpha = 0.1f))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    "\"$voiceTranscript\"",
                                    fontSize = 12.sp,
                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    if (!isListening) {
                                        voiceTranscript = ""
                                        speechError = null
                                        voiceLauncher()
                                    } else {
                                        voiceLauncher.stop()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isListening) Color(0xFFEF4444) else accentColor,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(14.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    if (isListening) "Tap to Stop Mic" else "Tap & Speak Problem",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }

                            if (voiceTranscript.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        voiceTranscript = ""
                                    },
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Text("Clear", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = inputContainerBg),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderStrokeColor, RoundedCornerShape(24.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        "SERVICE DETAILS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = accentColor
                    )

                    // Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Your Full Name") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = accentColor) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cust_name_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = inputTextColor,
                            unfocusedTextColor = inputTextColor,
                            cursorColor = accentColor,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = borderStrokeColor,
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = inputLabelColor,
                            focusedContainerColor = inputContainerColor,
                            unfocusedContainerColor = inputContainerColor
                        )
                    )

                    // Phone
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { if (it.length <= 10) phone = it },
                        label = { Text("10-Digit Mobile Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = accentColor) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("cust_phone_input"),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = inputTextColor,
                            unfocusedTextColor = inputTextColor,
                            cursorColor = accentColor,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = borderStrokeColor,
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = inputLabelColor,
                            focusedContainerColor = inputContainerColor,
                            unfocusedContainerColor = inputContainerColor
                        )
                    )

                    // Address
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Full Service Address") },
                        leadingIcon = { Icon(Icons.Default.Home, contentDescription = null, tint = accentColor) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = inputTextColor,
                            unfocusedTextColor = inputTextColor,
                            cursorColor = accentColor,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = borderStrokeColor,
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = inputLabelColor,
                            focusedContainerColor = inputContainerColor,
                            unfocusedContainerColor = inputContainerColor
                        )
                    )

                    // Category Dropdown
                    var categoryExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Service Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable, true)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = inputTextColor,
                                unfocusedTextColor = inputTextColor,
                                cursorColor = accentColor,
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = borderStrokeColor,
                                focusedLabelColor = accentColor,
                                unfocusedLabelColor = inputLabelColor,
                                focusedContainerColor = inputContainerColor,
                                unfocusedContainerColor = inputContainerColor
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat, fontWeight = FontWeight.Medium) },
                                    onClick = {
                                        category = cat
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    // Problem Description with simulated Voice Input
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = problemDescription,
                            onValueChange = { problemDescription = it },
                            label = { Text("Describe the electrical problem") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(110.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = inputTextColor,
                                unfocusedTextColor = inputTextColor,
                                cursorColor = accentColor,
                                focusedBorderColor = accentColor,
                                unfocusedBorderColor = borderStrokeColor,
                                focusedLabelColor = accentColor,
                                unfocusedLabelColor = inputLabelColor,
                                focusedContainerColor = inputContainerColor,
                                unfocusedContainerColor = inputContainerColor
                            )
                        )

                        // Beautiful premium voice button inside descriptor
                        IconButton(
                            onClick = { showDialog = true },
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(bottom = 10.dp, end = 10.dp)
                                .background(accentColor, RoundedCornerShape(12.dp))
                                .size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic, 
                                contentDescription = "Voice Input Assistant", 
                                tint = if (accentColor == Color(0xFF4F46E5) || accentColor == Color(0xFF6366F1)) Color.White else Color.Black, 
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    // Real Photo Upload Block
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC))
                                .border(1.dp, borderStrokeColor, RoundedCornerShape(16.dp))
                                .clickable {
                                    showPhotoSourceDialog = true
                                }
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(accentColor.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (selectedPhoto != null) Icons.Default.CheckCircle else Icons.Default.AddAPhoto,
                                    contentDescription = null,
                                    tint = if (selectedPhoto != null) NeonGreen else accentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    if (selectedPhoto != null) "Fault Photo Connected" else "Upload Issue Photo (Optional)",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                                Text(
                                    if (selectedPhoto != null) "Tap to change image" else "Take a live snap of the defect to assist our crew",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (selectedPhoto != null) {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .border(1.dp, borderStrokeColor, RoundedCornerShape(16.dp))
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = selectedPhoto,
                                        contentDescription = "Selected Fault Photo",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                                    )
                                    IconButton(
                                        onClick = { selectedPhoto = null },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(10.dp)
                                            .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                            .size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Remove Photo", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Submit Button with premium style
                    Button(
                        onClick = {
                            if (name.isNotBlank() && phone.length == 10 && address.isNotBlank()) {
                                val newReq = ServiceRequest(
                                    customerName = name,
                                    customerPhone = phone,
                                    customerAddress = address,
                                    problemDescription = problemDescription.ifBlank { "Unspecified electrical issue" },
                                    category = category,
                                    priority = priority,
                                    preferredDateTime = preferredDate,
                                    photoUri = selectedPhoto
                                )
                                repository.addServiceRequest(newReq)
                                // Also add to customer list if not existing
                                if (repository.customers.value.none { it.phone == phone }) {
                                    repository.addCustomer(
                                        Customer(name = name, phone = phone, address = address)
                                    )
                                }
                                onSubmissionSuccess(phone)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .height(54.dp)
                            .testTag("submit_request_btn"),
                        enabled = name.isNotBlank() && phone.length == 10 && address.isNotBlank(),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor, 
                            contentColor = if (accentColor == Color(0xFF4F46E5) || accentColor == Color(0xFF6366F1)) Color.White else Color.Black,
                            disabledContainerColor = accentColor.copy(alpha = 0.35f),
                            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            "Submit Electric Request", 
                            fontWeight = FontWeight.ExtraBold, 
                            fontSize = 15.sp,
                            letterSpacing = 0.3.sp
                        )
                    }
                }
            }
        }
    }

    // Photo Source Choice Dialog
    if (showPhotoSourceDialog) {
        AlertDialog(
            onDismissRequest = { showPhotoSourceDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text("Upload Fault Photo", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text("Capture a real-time photo of the defect or select an existing one from your gallery.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            },
            confirmButton = {
                Button(
                    onClick = {
                        showPhotoSourceDialog = false
                        cameraLauncher.launch(null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.Black)
                ) {
                    Icon(Icons.Default.PhotoCamera, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Take Photo")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPhotoSourceDialog = false
                        galleryLauncher.launch("image/*")
                    }
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Choose Gallery", color = accentColor)
                }
            }
        )
    }

    // Voice Templates Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { 
                Text(
                    "Punjabi, Hindi, and English Voice Assistant", 
                    color = MaterialTheme.colorScheme.onSurface, 
                    fontSize = 16.sp, 
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Speak in your local language or tap a preset script to automatically extract and fill all fields instantly:", 
                        fontSize = 12.sp, 
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    val voiceScripts = listOf(
                        Triple(
                            "Punjabi (ਸਿੰਘ - Fan Spark)", 
                            "ਮੇਰਾ ਨਾਮ ਗੁਰਪ੍ਰੀਤ ਸਿੰਘ ਹੈ ਮੋਬਾਈਲ 9872134590 ਘਰ ਦਾ ਪਤਾ ਗ੍ਰੀਨ ਐਵੇਨਿਊ ਅੰਮ੍ਰਿਤਸਰ ਹੈ, ਮੇਰਾ ceiling fan ਚੱਲ ਨਹੀਂ ਰਿਹਾ ਅਤੇ ਬੋਰਡ ਚੋਂ ਸਪਾਰਕ ਹੋ ਰਿਹਾ ਹੈ",
                            "ਗੁਰਪ੍ਰੀਤ ਸਿੰਘ, 9872134590, ਅੰਮ੍ਰਿਤਸਰ"
                        ),
                        Triple(
                            "Punjabi (ਸਿੰਘ - Fuse Burn)", 
                            "ਮੇਰਾ ਨਾਮ ਹਰਜੀਤ ਸਿੰਘ ਹੈ ਮੇਰਾ ਮੋਬਾਈਲ ਨੰਬਰ 9417255831 ਹੈ ਮੇਰਾ ਪਤਾ ਸੈਕਟਰ 15 ਚੰਡੀਗੜ੍ਹ ਹੈ ਮੇਰਾ ਮੇਨ ਪਾਵਰ fuse ਸੜ ਗਿਆ ਹੈ ਅਤੇ ਧੂੰਆਂ ਨਿਕਲ ਰਿਹਾ ਹੈ",
                            "ਹਰਜੀਤ ਸਿੰਘ, 9417255831, ਚੰਡੀਗੜ੍ਹ"
                        ),
                        Triple(
                            "Hindi (कुमार - Motor Fault)", 
                            "मेरा नाम राजेश कुमार है मोबाइल नंबर 9911223344 है सर्विस एड्रेस सेक्टर 22 चंडीगढ़ वाटर मोटर पानी नहीं दे रही और शॉर्ट हो रही है",
                            "राजेश कुमार, 9911223344, चंडीगढ़"
                        ),
                        Triple(
                            "English (Singh - MCB Trip)", 
                            "My name is Gursimar Singh, my phone is 9812345678, living in Phase 3B2 Mohali, AC switch is short circuited and tripping MCB",
                            "Gursimar Singh, 9812345678, Mohali"
                        )
                    )

                    voiceScripts.forEach { (lang, script, summary) ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val parsed = repository.parseServiceRequestVoice(script)
                                    name = parsed.name
                                    phone = parsed.phone
                                    address = parsed.address
                                    problemDescription = parsed.problem
                                    
                                    // Match category intelligently
                                    if (script.lowercase().contains("fan") || script.contains("ਪੱਖਾ")) {
                                        category = "Fan Installation"
                                    } else if (script.lowercase().contains("motor") || script.contains("ਮੋਟਰ")) {
                                        category = "Motor Repair"
                                    } else if (script.lowercase().contains("fuse") || script.lowercase().contains("mcb") || script.contains("ਫਿਊਜ਼")) {
                                        category = "DB Board Repair"
                                    } else if (script.lowercase().contains("wire") || script.contains("ਤਾਰ")) {
                                        category = "Wiring Check"
                                    }
                                    
                                    showDialog = false
                                    Toast.makeText(context, "Punjabi Voice parsed! All fields populated.", Toast.LENGTH_LONG).show()
                                }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(lang, fontWeight = FontWeight.Bold, color = accentColor, fontSize = 12.sp)
                                    Text(summary, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(script, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel", color = accentColor)
                }
            }
        )
    }
}

@Composable
fun RequestTrackerView(
    repository: ReLectRepository,
    phoneSearch: String,
    onPhoneChange: (String) -> Unit,
    showList: Boolean,
    onShowListChange: (Boolean) -> Unit,
    accentColor: Color
) {
    val allRequests by repository.services.collectAsState()
    val matchingRequests = remember(allRequests, phoneSearch) {
        allRequests.filter { it.customerPhone == phoneSearch }
    }
    val isDark = MaterialTheme.colorScheme.surface != Color.White
    val cardBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.6f) else Color.White
    val borderCol = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val inputTextColor = if (isDark) Color.White else Color(0xFF0F172A)
    val inputContainerColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    val inputBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
    val inputLabelColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Search Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 0.dp else 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderCol, RoundedCornerShape(24.dp))
            ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(accentColor.copy(alpha = 0.12f))
                            .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Track Your Service Status",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.5).sp,
                            color = if (isDark) Color.White else Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            "Enter the 10-digit mobile number used during booking to view real-time scheduling updates.",
                            fontSize = 12.sp,
                            lineHeight = 16.sp,
                            color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = phoneSearch,
                        onValueChange = { if (it.length <= 10) onPhoneChange(it) },
                        label = { Text("Mobile Number", fontSize = 12.sp) },
                        placeholder = { Text("10-digit phone number", fontSize = 13.sp, color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Phone,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            if (phoneSearch.isNotEmpty()) {
                                IconButton(onClick = { onPhoneChange("") }, modifier = Modifier.size(28.dp)) {
                                    Icon(
                                        Icons.Default.Close,
                                        contentDescription = "Clear",
                                        tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        maxLines = 1,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = inputTextColor
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("track_phone_input"),
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = inputTextColor,
                            unfocusedTextColor = inputTextColor,
                            cursorColor = accentColor,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = inputBorderColor,
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = inputLabelColor,
                            focusedContainerColor = inputContainerColor,
                            unfocusedContainerColor = inputContainerColor
                        )
                    )

                    Button(
                        onClick = { onShowListChange(true) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                        modifier = Modifier.height(52.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "Search Requests", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Search", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }

                // Sample numbers quick chips if available
                val samplePhones = remember(allRequests) {
                    allRequests.map { it.customerPhone }.filter { it.isNotBlank() }.distinct().take(3)
                }
                if (samplePhones.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Quick Suggestions:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        samplePhones.forEach { num ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = accentColor.copy(alpha = if (isDark) 0.2f else 0.1f),
                                border = BorderStroke(0.8.dp, accentColor.copy(alpha = 0.35f)),
                                modifier = Modifier.clickable {
                                    onPhoneChange(num)
                                    onShowListChange(true)
                                }
                            ) {
                                Text(
                                    num,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

        if (showList) {
            if (matchingRequests.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp, horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(borderCol.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.HourglassEmpty,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No Service Requests Found",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Check your input number or submit a new service request.",
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(matchingRequests, key = { it.id }) { req ->
                    TrackingJobCard(req = req, accentColor = accentColor)
                }
            }
        }
    }
}

@Composable
fun TrackingJobCard(req: ServiceRequest, accentColor: Color) {
    val isDark = MaterialTheme.colorScheme.surface != Color.White
    val cardBg = if (isDark) Color(0xFF1E293B).copy(alpha = 0.3f) else Color.White
    val borderCol = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, borderCol, RoundedCornerShape(24.dp))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        req.category,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        "TRACKING ID: #${req.id.take(8).uppercase()}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }

                // Status chip
                val statusColor = when (req.status) {
                    ServiceStatus.CREATED -> Color(0xFF94A3B8)
                    ServiceStatus.ASSIGNED -> NeonYellow
                    ServiceStatus.IN_PROGRESS -> accentColor
                    ServiceStatus.COMPLETED -> NeonGreen
                    ServiceStatus.CLOSED -> Color(0xFF64748B)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        req.status.label.uppercase(),
                        color = statusColor,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Details Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (isDark) Color(0xFF0F172A).copy(alpha = 0.5f) else Color(0xFFF8FAFC))
                    .border(1.dp, borderCol, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Column {
                    Text(
                        "PROBLEM REPORTED",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        req.problemDescription,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Live Timeline indicator
            Text(
                "LIVE PROGRESS TRACKING",
                fontWeight = FontWeight.Bold,
                color = accentColor,
                fontSize = 11.sp,
                letterSpacing = 1.2.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            val statuses = ServiceStatus.values()
            val currentStatusIdx = statuses.indexOf(req.status)

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                statuses.forEachIndexed { idx, status ->
                    val historyMatch = req.statusHistory.find { it.status == status }
                    val isPastOrCurrent = idx <= currentStatusIdx || historyMatch != null
                    val isCurrent = idx == currentStatusIdx
                    
                    val noteText = historyMatch?.notes ?: when(status) {
                        ServiceStatus.CREATED -> "Request registered. Awaiting engineer assignment."
                        ServiceStatus.ASSIGNED -> "Technician has been scheduled."
                        ServiceStatus.IN_PROGRESS -> "On-site check, testing, and repair is underway."
                        ServiceStatus.COMPLETED -> "Electrical repair is complete and ready!"
                        ServiceStatus.CLOSED -> "Request closed and archived."
                    }
                    val timestamp = historyMatch?.timestamp ?: ""

                    Row(
                        verticalAlignment = Alignment.Top,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Visual Timeline Dot
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(24.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isCurrent) accentColor.copy(alpha = 0.25f)
                                        else if (isPastOrCurrent) NeonGreen.copy(alpha = 0.2f)
                                        else borderCol.copy(alpha = 0.5f)
                                    )
                                    .border(
                                        1.5.dp,
                                        if (isCurrent) accentColor
                                        else if (isPastOrCurrent) NeonGreen
                                        else borderCol,
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isPastOrCurrent) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(if (isCurrent) accentColor else NeonGreen)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Timeline Description
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    status.label,
                                    color = if (isCurrent) MaterialTheme.colorScheme.onSurface 
                                            else if (isPastOrCurrent) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f) 
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                if (timestamp.isNotBlank()) {
                                    Text(
                                        timestamp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                            Text(
                                noteText,
                                color = if (isPastOrCurrent) MaterialTheme.colorScheme.onSurfaceVariant 
                                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                                fontSize = 11.sp,
                                lineHeight = 15.sp,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                }
            }

            if (req.assignedWorkerName != null) {
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(accentColor.copy(alpha = 0.08f))
                        .border(1.dp, accentColor.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Engineering, 
                            contentDescription = null, 
                            tint = accentColor, 
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            "ASSIGNED ELECTRICIAN",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = accentColor
                        )
                        Text(
                            req.assignedWorkerName,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}
