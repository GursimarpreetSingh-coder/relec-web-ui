package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ReLectRepository
import com.example.data.ReLectRepository.StoreSupportInfo
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportDeskManagementScreen(
    repository: ReLectRepository,
    accentColor: Color,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val liveInfo by repository.storeSupportInfo.collectAsState()

    var storeName by remember(liveInfo) { mutableStateOf(liveInfo.storeName) }
    var phone by remember(liveInfo) { mutableStateOf(liveInfo.phone) }
    var whatsapp by remember(liveInfo) { mutableStateOf(liveInfo.whatsapp) }
    var address by remember(liveInfo) { mutableStateOf(liveInfo.address) }
    var workingHours by remember(liveInfo) { mutableStateOf(liveInfo.workingHours) }
    var emergencyHelpline by remember(liveInfo) { mutableStateOf(liveInfo.emergencyHelpline) }
    var safetyGuidelines by remember(liveInfo) { mutableStateOf(liveInfo.safetyGuidelines) }
    var appDownloadUrl by remember(liveInfo) { mutableStateOf(liveInfo.appDownloadUrl) }

    var feedbackBannerMessage by remember { mutableStateOf<String?>(null) }

    val isDark = MaterialTheme.colorScheme.surface != Color.White
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
    val inputBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 0.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. Screen Header Bar with Back button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = textColor, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text("Customer Support Desk", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, letterSpacing = (-0.3).sp)
                    Text("Helpline, WhatsApp & Customer Queries", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = subtitleColor)
                }
            }
        }

        // 1. Compact Status Bar
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(accentColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Live Customer Helpline Settings",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                "Edits reflect instantly on customer portal & bills",
                                fontSize = 10.5.sp,
                                color = subtitleColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.12f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                "LIVE SYNC",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF10B981),
                                softWrap = false,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // 2. Themed In-App Feedback Banner Animation
        item {
            AnimatedVisibility(visible = feedbackBannerMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.14f)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = feedbackBannerMessage ?: "Updated Successfully",
                                fontWeight = FontWeight.Bold,
                                color = accentColor,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Changes have been updated immediately across the system.",
                                fontSize = 11.sp,
                                color = textColor
                            )
                        }
                    }
                }
            }
        }

        // 3. Live Customer Preview Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF030816) else Color(0xFFF1F5F9)),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Visibility, contentDescription = null, tint = accentColor, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("LIVE PREVIEW (HOW CUSTOMERS SEE THIS)", fontSize = 10.sp, fontWeight = FontWeight.Black, color = accentColor)
                        }
                        Text("Instant Sync", fontSize = 10.sp, color = subtitleColor)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(containerColor = cardBg),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(storeName.ifBlank { "Rattan Electricals" }, fontWeight = FontWeight.Black, fontSize = 15.sp, color = textColor)
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Phone, contentDescription = null, tint = accentColor, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(phone.ifBlank { "+91 98120 45612" }, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = textColor)
                                Spacer(modifier = Modifier.width(14.dp))
                                Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF25D366), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp: " + whatsapp.ifBlank { "+91 98120 45612" }, fontSize = 12.sp, color = subtitleColor)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(address.ifBlank { "Main Market, Sector 14, Gurugram" }, fontSize = 11.5.sp, color = subtitleColor, maxLines = 1)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Hours: " + workingHours.ifBlank { "Mon - Sat: 9:00 AM - 8:30 PM" }, fontSize = 11.sp, color = subtitleColor)
                            }
                        }
                    }
                }
            }
        }

        // 4. Editor Form Fields
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Store & Helpdesk Information", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = textColor)

                    // Store Name
                    OutlinedTextField(
                        value = storeName,
                        onValueChange = { storeName = it },
                        label = { Text("Store / Helpdesk Name") },
                        leadingIcon = { Icon(Icons.Default.Storefront, contentDescription = null, tint = accentColor) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg
                        )
                    )

                    // Phone & WhatsApp in balanced layout
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text("Primary Calling Number") },
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = accentColor) },
                        placeholder = { Text("+91 98120 45612") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg
                        )
                    )

                    OutlinedTextField(
                        value = whatsapp,
                        onValueChange = { whatsapp = it },
                        label = { Text("WhatsApp Support Number") },
                        leadingIcon = { Icon(Icons.Default.Chat, contentDescription = null, tint = Color(0xFF25D366)) },
                        placeholder = { Text("+91 98120 45612") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg
                        )
                    )

                    // Emergency 24/7 Helpline
                    OutlinedTextField(
                        value = emergencyHelpline,
                        onValueChange = { emergencyHelpline = it },
                        label = { Text("24/7 Emergency Helpline") },
                        leadingIcon = { Icon(Icons.Default.Emergency, contentDescription = null, tint = Color(0xFFEF4444)) },
                        placeholder = { Text("+91 98000 11223") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg
                        )
                    )

                    // Store Address
                    OutlinedTextField(
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Full Store Address") },
                        leadingIcon = { Icon(Icons.Default.LocationOn, contentDescription = null, tint = accentColor) },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 2,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg
                        )
                    )

                    // Working Hours
                    OutlinedTextField(
                        value = workingHours,
                        onValueChange = { workingHours = it },
                        label = { Text("Business & Working Hours") },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, tint = accentColor) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg
                        )
                    )

                    // Safety Guidelines
                    OutlinedTextField(
                        value = safetyGuidelines,
                        onValueChange = { safetyGuidelines = it },
                        label = { Text("Safety Precautions & Tips for Customers") },
                        leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = Color(0xFFF59E0B)) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        maxLines = 5,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg
                        )
                    )

                    // App QR Code & Download / Portal Link URL
                    OutlinedTextField(
                        value = appDownloadUrl,
                        onValueChange = { appDownloadUrl = it },
                        label = { Text("App Download QR Code & Web Portal Link") },
                        leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, tint = Color(0xFF8B5CF6)) },
                        trailingIcon = {
                            IconButton(onClick = {
                                appDownloadUrl = "https://ais-pre-x5noeypzgmvkg3noct2hto-828738081727.asia-east1.run.app/?view=customer"
                            }) {
                                Icon(Icons.Default.Refresh, contentDescription = "Default URL", tint = subtitleColor)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = inputBg,
                            unfocusedContainerColor = inputBg
                        )
                    )
                    Text(
                        "Updating this link dynamically refreshes the QR code across Settings, Support Desk, and Bill counter standees.",
                        fontSize = 11.sp,
                        color = subtitleColor,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }

        // 5. Actions: Save & Reset
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = {
                        val defaultInfo = StoreSupportInfo()
                        storeName = defaultInfo.storeName
                        phone = defaultInfo.phone
                        whatsapp = defaultInfo.whatsapp
                        address = defaultInfo.address
                        workingHours = defaultInfo.workingHours
                        emergencyHelpline = defaultInfo.emergencyHelpline
                        safetyGuidelines = defaultInfo.safetyGuidelines
                        appDownloadUrl = defaultInfo.appDownloadUrl
                        repository.updateSupportInfo(defaultInfo)
                        scope.launch {
                            feedbackBannerMessage = "Reset to standard defaults successfully!"
                            delay(3000)
                            feedbackBannerMessage = null
                        }
                    },
                    modifier = Modifier.weight(1f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
                ) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Reset Defaults",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                Button(
                    onClick = {
                        val updated = StoreSupportInfo(
                            storeName = storeName.trim(),
                            phone = phone.trim(),
                            whatsapp = whatsapp.trim(),
                            address = address.trim(),
                            workingHours = workingHours.trim(),
                            emergencyHelpline = emergencyHelpline.trim(),
                            safetyGuidelines = safetyGuidelines.trim(),
                            appDownloadUrl = appDownloadUrl.trim()
                        )
                        repository.updateSupportInfo(updated)
                        scope.launch {
                            feedbackBannerMessage = "Support Desk details saved and published live!"
                            delay(3000)
                            feedbackBannerMessage = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = accentColor,
                        contentColor = Color.White
                    ),
                    modifier = Modifier.weight(1.5f).height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save & Publish Live", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, maxLines = 1)
                }
            }
        }
    }
}
