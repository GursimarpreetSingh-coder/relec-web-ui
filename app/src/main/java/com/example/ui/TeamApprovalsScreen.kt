package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ReLectRepository
import com.example.data.UserEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val SUPERIOR_ADMIN_EMAIL = "gursimarpsr@gmail.com"

@Composable
fun TeamApprovalsScreen(
    repository: ReLectRepository,
    accentColor: Color,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val users by repository.users.collectAsState()

    var selectedFilter by remember { mutableStateOf("All") } // "All", "Pending", "Admin", "Worker", "Customer"
    var userToDelete by remember { mutableStateOf<UserEntity?>(null) }
    var userToEditRole by remember { mutableStateOf<UserEntity?>(null) }
    var showAddUserDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var isPolicyExpanded by remember { mutableStateOf(false) }

    val isDark = MaterialTheme.colorScheme.surface != Color.White
    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val cardBg = if (isDark) Color(0xFF0F172A) else Color.White
    val borderColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    val pendingCount = remember(users) { users.count { !it.isApproved } }
    val adminCount = remember(users) { users.count { it.role.equals("Admin", ignoreCase = true) || it.role.equals("SuperAdmin", ignoreCase = true) } }
    val workerCount = remember(users) { users.count { it.role.equals("Worker", ignoreCase = true) } }
    val customerCount = remember(users) { users.count { it.role.equals("Customer", ignoreCase = true) } }

    val filteredUsers = remember(users, selectedFilter, searchQuery) {
        users.filter { user ->
            val matchesFilter = when (selectedFilter) {
                "Pending" -> !user.isApproved
                "Admin" -> user.role.equals("Admin", ignoreCase = true) || user.role.equals("SuperAdmin", ignoreCase = true)
                "Worker" -> user.role.equals("Worker", ignoreCase = true)
                "Customer" -> user.role.equals("Customer", ignoreCase = true)
                else -> true
            }
            val query = searchQuery.trim().lowercase()
            val matchesSearch = query.isBlank() ||
                user.displayName.lowercase().contains(query) ||
                (user.email?.lowercase()?.contains(query) == true) ||
                (user.phone?.contains(query) == true)

            matchesFilter && matchesSearch
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 0.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. Screen Header Bar with Back button & proper spacing for Add button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f).padding(end = 12.dp),
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
                        Text("Team & Access", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = textColor, letterSpacing = (-0.3).sp)
                        Text("Enterprise Security & Role Governance", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = subtitleColor)
                    }
                }

                Button(
                    onClick = { showAddUserDialog = true },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.height(36.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 1. Sleek Compact Superior Admin Security Notice (Saves vertical space & prevents huge box)
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = if (pendingCount > 0) {
                        if (isDark) Color(0xFF2D1F0D) else Color(0xFFFFFBEB)
                    } else {
                        if (isDark) Color(0xFF131D31) else Color(0xFFF1F5F9)
                    }
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(
                    1.dp,
                    if (pendingCount > 0) Color(0xFFF59E0B).copy(alpha = 0.6f) else borderColor
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isPolicyExpanded = !isPolicyExpanded }
            ) {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        ) {
                            Icon(
                                Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (pendingCount > 0) Color(0xFFF59E0B) else accentColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Superior Admin Governance",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = textColor
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (pendingCount > 0) Color(0xFFEF4444) else accentColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 1.5.dp)
                            ) {
                                Text(
                                    if (pendingCount > 0) "$pendingCount PENDING" else "ACTIVE",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (pendingCount > 0) Color.White else accentColor
                                )
                            }
                        }

                        Icon(
                            if (isPolicyExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (isPolicyExpanded) "Collapse" else "Expand",
                            tint = subtitleColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    AnimatedVisibility(visible = isPolicyExpanded) {
                        Column {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "Master account (gursimarpsr@gmail.com) has supreme authority. Admin and Worker registrations require approval before access is granted.",
                                fontSize = 11.sp,
                                color = subtitleColor,
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }
        }

        // 2. Metrics Quick Bar - Compact with "TOTAL" label
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Metric 1: Total Users
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TOTAL", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = subtitleColor)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("${users.size}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = textColor)
                    }
                }

                // Metric 2: Pending
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (pendingCount > 0) Color(0xFFFEF3C7).copy(alpha = 0.5f) else cardBg
                    ),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (pendingCount > 0) Color(0xFFF59E0B) else borderColor),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("PENDING", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = if (pendingCount > 0) Color(0xFFD97706) else subtitleColor)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "$pendingCount",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = if (pendingCount > 0) Color(0xFFD97706) else textColor
                        )
                    }
                }

                // Metric 3: Admins
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("ADMINS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = subtitleColor)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("$adminCount", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF8B5CF6))
                    }
                }

                // Metric 4: Workers
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("WORKERS", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = subtitleColor)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("$workerCount", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF0284C7))
                    }
                }
            }
        }

        // 3. Search Bar (Full Width so placeholder and query are completely visible without truncation)
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search by name, email, or phone...", fontSize = 12.sp) },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = subtitleColor)
                },
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = accentColor,
                    unfocusedBorderColor = borderColor
                ),
                singleLine = true
            )
        }

        // 4. Filter Pills (Clean horizontal scroll, strictly prevents vertical word squishing!)
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "All" to "${users.size}",
                    "Pending" to "$pendingCount",
                    "Admin" to "$adminCount",
                    "Worker" to "$workerCount",
                    "Customer" to "$customerCount"
                ).forEach { (filterKey, count) ->
                    val isSelected = selectedFilter == filterKey
                    val pillBg = if (isSelected) {
                        if (filterKey == "Pending" && count.toIntOrNull() ?: 0 > 0) Color(0xFFEF4444) else accentColor
                    } else {
                        if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                    }
                    val pillTextColor = if (isSelected) Color.White else subtitleColor

                    Surface(
                        onClick = { selectedFilter = filterKey },
                        shape = RoundedCornerShape(10.dp),
                        color = pillBg,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color.Transparent else borderColor
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                filterKey,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = pillTextColor,
                                softWrap = false,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) Color.White.copy(alpha = 0.25f)
                                        else if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    count,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = pillTextColor,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. User Cards List
        if (filteredUsers.isEmpty()) {
            item {
                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, borderColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.PersonOff,
                            contentDescription = null,
                            tint = subtitleColor,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            "No members found in '$selectedFilter'",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = textColor
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Try changing the filter or clearing the search query",
                            fontSize = 11.5.sp,
                            color = subtitleColor
                        )
                    }
                }
            }
        } else {
            items(filteredUsers, key = { it.id }) { user ->
                TeamMemberCard(
                    user = user,
                    accentColor = accentColor,
                    isDark = isDark,
                    onApprove = {
                        repository.approveUser(user.id)
                        Toast.makeText(context, "${user.displayName} approved!", Toast.LENGTH_SHORT).show()
                    },
                    onToggleActive = {
                        repository.toggleUserActive(user.id)
                        Toast.makeText(
                            context,
                            if (user.isActive) "${user.displayName} suspended" else "${user.displayName} reactivated",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onEditRole = { userToEditRole = user },
                    onDelete = { userToDelete = user }
                )
            }
        }
    }

    // Delete User Confirmation Dialog
    if (userToDelete != null) {
        val target = userToDelete!!
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            shape = RoundedCornerShape(16.dp),
            title = {
                Text("Revoke Access & Delete User", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Text(
                    "Are you sure you want to completely remove ${target.displayName} (${target.role})? Their login access will be permanently revoked.",
                    fontSize = 12.5.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteUser(target.id)
                        Toast.makeText(context, "User removed from system", Toast.LENGTH_SHORT).show()
                        userToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("Delete Account", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Edit Role Dialog
    if (userToEditRole != null) {
        val target = userToEditRole!!
        var newRole by remember { mutableStateOf(target.role) }

        AlertDialog(
            onDismissRequest = { userToEditRole = null },
            shape = RoundedCornerShape(16.dp),
            title = {
                Text("Change Role: ${target.displayName}", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select new authority level:", fontSize = 12.sp, color = subtitleColor)
                    listOf("Admin", "Worker", "Customer").forEach { roleOption ->
                        Surface(
                            onClick = { newRole = roleOption },
                            shape = RoundedCornerShape(10.dp),
                            color = if (newRole.equals(roleOption, ignoreCase = true)) accentColor.copy(alpha = 0.15f) else Color.Transparent,
                            border = BorderStroke(
                                1.dp,
                                if (newRole.equals(roleOption, ignoreCase = true)) accentColor else borderColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(12.dp)
                            ) {
                                RadioButton(
                                    selected = newRole.equals(roleOption, ignoreCase = true),
                                    onClick = { newRole = roleOption },
                                    colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        roleOption,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = textColor
                                    )
                                    Text(
                                        when (roleOption) {
                                            "Admin" -> "Full management of billing, inventory, workers & reports"
                                            "Worker" -> "View assigned jobs, update repair status & time logs"
                                            else -> "Public portal view for tracking complaints & repairs"
                                        },
                                        fontSize = 10.sp,
                                        color = subtitleColor
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.updateUser(target.copy(role = newRole))
                        Toast.makeText(context, "Role updated to $newRole", Toast.LENGTH_SHORT).show()
                        userToEditRole = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Save Changes", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToEditRole = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add New Member Dialog
    if (showAddUserDialog) {
        var nameInput by remember { mutableStateOf("") }
        var phoneInput by remember { mutableStateOf("") }
        var emailInput by remember { mutableStateOf("") }
        var roleInput by remember { mutableStateOf("Worker") }

        AlertDialog(
            onDismissRequest = { showAddUserDialog = false },
            shape = RoundedCornerShape(16.dp),
            title = {
                Text("Add Team Member", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Full Name *") },
                        placeholder = { Text("e.g. Gurpreet Singh") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Mobile Number *") },
                        placeholder = { Text("e.g. 9812045612") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email (Optional)") },
                        placeholder = { Text("e.g. worker@relect.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Text("Assign Role:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = subtitleColor)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Worker", "Admin", "Customer").forEach { r ->
                            val isSel = roleInput == r
                            FilterChip(
                                selected = isSel,
                                onClick = { roleInput = r },
                                label = { Text(r, fontSize = 11.sp) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cleanName = nameInput.trim()
                        val cleanPhone = phoneInput.trim()
                        if (cleanName.isBlank()) {
                            Toast.makeText(context, "Please enter member name", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (cleanPhone.length < 10) {
                            Toast.makeText(context, "Please enter a valid 10-digit mobile number", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val newUser = UserEntity(
                            id = "USR_${System.currentTimeMillis()}",
                            displayName = cleanName,
                            phone = cleanPhone,
                            email = emailInput.trim().ifEmpty { null },
                            role = roleInput,
                            isActive = true,
                            isApproved = true,
                            createdAt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        )
                        repository.insertUser(newUser)
                        Toast.makeText(context, "$cleanName added and pre-authorized!", Toast.LENGTH_LONG).show()
                        showAddUserDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor)
                ) {
                    Text("Add & Authorize", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddUserDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun TeamMemberCard(
    user: UserEntity,
    accentColor: Color,
    isDark: Boolean,
    onApprove: () -> Unit,
    onToggleActive: () -> Unit,
    onEditRole: () -> Unit,
    onDelete: () -> Unit
) {
    val cleanDisplayName = user.displayName
        .replace(Regex("(?i)\\s*\\(master\\s*admin\\)"), "")
        .replace(Regex("[\\r\\n\\t\\u00A0\\u2028\\u2029]+"), " ")
        .trim()
        .ifBlank { "Team Member" }

    val cleanContact = (user.email ?: user.phone ?: "ID: ${user.id}")
        .replace(Regex("[\\r\\n\\t\\u00A0\\u2028\\u2029]+"), "")
        .trim()

    val isMasterAdmin = cleanContact.equals(SUPERIOR_ADMIN_EMAIL, ignoreCase = true) ||
        cleanContact.contains("gursimarpsr@gmail.com", ignoreCase = true) ||
        user.displayName.contains("Gursimar", ignoreCase = true) ||
        user.displayName.contains("Master Admin", ignoreCase = true) ||
        user.role.equals("SuperAdmin", ignoreCase = true) ||
        user.id == "ADMIN_MASTER_GURSIMAR" ||
        user.id == "USR_ADMIN_1"

    val roleColor = when (user.role.lowercase()) {
        "admin", "superadmin" -> Color(0xFF8B5CF6) // Purple
        "worker" -> Color(0xFF0284C7) // Blue
        "customer" -> Color(0xFF10B981) // Green
        else -> Color.Gray
    }

    // High contrast distinct background for Master Admin card as requested by user
    val cardBg = if (isMasterAdmin) {
        if (isDark) Color(0xFF142038) else Color(0xFFEEF4FF)
    } else {
        if (isDark) Color(0xFF0F172A) else Color.White
    }

    val textColor = if (isDark) Color.White else Color(0xFF0F172A)
    val subtitleColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    val borderColor = if (isMasterAdmin) {
        if (isDark) Color(0xFF6366F1).copy(alpha = 0.65f) else Color(0xFF6366F1).copy(alpha = 0.45f)
    } else {
        if (!user.isApproved) Color(0xFFF59E0B)
        else if (isDark) Color(0xFF334155)
        else Color(0xFFE2E8F0)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(if (isMasterAdmin) 1.2.dp else 1.dp, borderColor),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp)
        ) {
            // Header Row: Avatar, Name, Email/Phone, Role Badge & Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(
                                if (isMasterAdmin) Color(0xFF6366F1).copy(alpha = 0.18f)
                                else roleColor.copy(alpha = 0.15f)
                            )
                            .border(
                                1.dp,
                                if (isMasterAdmin) Color(0xFF6366F1).copy(alpha = 0.45f) else Color.Transparent,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isMasterAdmin) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                                modifier = Modifier.size(17.dp)
                            )
                        } else {
                            Text(
                                cleanDisplayName.take(1).uppercase().ifBlank { "U" },
                                fontWeight = FontWeight.Black,
                                fontSize = 13.5.sp,
                                color = roleColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = cleanDisplayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isMasterAdmin) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isDark) Color(0xFF3B2E15) else Color(0xFFFEF3C7))
                                        .border(0.8.dp, Color(0xFFF59E0B).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        "MASTER",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(1.dp))

                        Text(
                            text = cleanContact,
                            fontSize = 11.sp,
                            color = subtitleColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Role & Status Badge
                Column(horizontalAlignment = Alignment.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                if (isMasterAdmin) Color(0xFF6366F1).copy(alpha = 0.15f)
                                else roleColor.copy(alpha = 0.15f)
                            )
                            .border(
                                0.8.dp,
                                if (isMasterAdmin) Color(0xFF6366F1).copy(alpha = 0.4f) else roleColor.copy(alpha = 0.4f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            user.role.uppercase(),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isMasterAdmin) (if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5)) else roleColor
                        )
                    }

                    Spacer(modifier = Modifier.height(2.5.dp))

                    val statusBg = if (!user.isApproved) Color(0xFFFEF3C7)
                    else if (!user.isActive) Color(0xFFFEE2E2)
                    else Color(0xFFDCFCE7)

                    val statusText = if (!user.isApproved) "Pending"
                    else if (!user.isActive) "Suspended"
                    else "Active"

                    val statusColor = if (!user.isApproved) Color(0xFFD97706)
                    else if (!user.isActive) Color(0xFFDC2626)
                    else Color(0xFF16A34A)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            statusText,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            HorizontalDivider(color = borderColor.copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(5.dp))

            // Action Buttons Row - Fixed compact layout preventing text clipping
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Joined: ${user.createdAt.ifEmpty { "Recent" }.take(10)}",
                    fontSize = 9.5.sp,
                    color = subtitleColor
                )

                if (isMasterAdmin) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        Icon(
                            Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "Superior Admin Owner",
                            fontSize = 10.sp,
                            color = if (isDark) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
                            fontWeight = FontWeight.Bold
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        if (!user.isApproved) {
                            Button(
                                onClick = onApprove,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                shape = RoundedCornerShape(7.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(26.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(3.dp))
                                Text("Approve", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = onEditRole,
                            shape = RoundedCornerShape(7.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(26.dp)
                        ) {
                            Text("Role", fontSize = 10.sp, fontWeight = FontWeight.Medium)
                        }

                        IconButton(
                            onClick = onToggleActive,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                if (user.isActive) Icons.Default.Block else Icons.Default.CheckCircle,
                                contentDescription = if (user.isActive) "Suspend" else "Activate",
                                tint = if (user.isActive) Color(0xFFF59E0B) else Color(0xFF10B981),
                                modifier = Modifier.size(15.dp)
                            )
                        }

                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = "Delete User",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
