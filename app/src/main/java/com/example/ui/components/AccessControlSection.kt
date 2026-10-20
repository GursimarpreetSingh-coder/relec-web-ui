package com.example.ui.components

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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

private const val SUPERIOR_ADMIN_EMAIL = "gursimarpsr@gmail.com"

@Composable
fun AccessControlSection(
    repository: ReLectRepository,
    accentColor: Color,
    isDarkTheme: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val users by repository.users.collectAsState()

    var selectedFilter by remember { mutableStateOf("All") } // "All", "Pending", "Admin", "Worker", "Customer"
    var userToDelete by remember { mutableStateOf<UserEntity?>(null) }
    var searchQuery by remember { mutableStateOf("") }

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

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Superior Admin Security Banner
        Card(
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    1.2.dp,
                    if (pendingCount > 0) Color(0xFFF59E0B) else accentColor.copy(alpha = 0.5f),
                    RoundedCornerShape(16.dp)
                )
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
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (pendingCount > 0) Color(0xFFFEF3C7) else accentColor.copy(alpha = 0.15f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (pendingCount > 0) Icons.Default.AdminPanelSettings else Icons.Default.Security,
                                contentDescription = null,
                                tint = if (pendingCount > 0) Color(0xFFD97706) else accentColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                "Superior Admin Authority",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "1 Master Owner Approval Policy Active",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (pendingCount > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEF4444))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                "$pendingCount PENDING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    "Only you (the Superior Admin) can authorize additional Admin or Worker technician accounts. Any new user registering with an Admin or Worker role is locked until activated below.",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 15.sp
                )
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by name, email, or phone...", fontSize = 12.sp) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
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
                unfocusedBorderColor = if (isDarkTheme) Color(0xFF334155) else Color(0xFFCBD5E1)
            ),
            singleLine = true
        )

        // Filter Pills Row (Horizontally scrollable, prevents uneven word squishing)
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
                    if (filterKey == "Pending") Color(0xFFEF4444) else accentColor
                } else {
                    if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                }
                val textColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(pillBg)
                        .clickable { selectedFilter = filterKey }
                        .padding(horizontal = 14.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$filterKey ($count)",
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = textColor,
                        softWrap = false,
                        maxLines = 1
                    )
                }
            }
        }

        // List of Accounts
        if (filteredUsers.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = if (isDarkTheme) Color(0xFF0F172A) else Color(0xFFF8FAFC)),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFE2E8F0), RoundedCornerShape(14.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (selectedFilter == "Pending") "No accounts currently waiting for approval." else "No user accounts found matching '$searchQuery'.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                filteredUsers.forEach { user ->
                    UserApprovalCard(
                        user = user,
                        accentColor = accentColor,
                        isDarkTheme = isDarkTheme,
                        onApprove = {
                            repository.setUserApproval(user.id, true)
                            Toast.makeText(context, "Access approved for ${user.displayName}!", Toast.LENGTH_SHORT).show()
                        },
                        onRevoke = {
                            repository.setUserApproval(user.id, false)
                            Toast.makeText(context, "Access revoked for ${user.displayName}", Toast.LENGTH_SHORT).show()
                        },
                        onToggleActive = {
                            val newActive = !user.isActive
                            repository.setUserActive(user.id, newActive)
                            val msg = if (newActive) "Account activated" else "Account suspended"
                            Toast.makeText(context, "$msg for ${user.displayName}", Toast.LENGTH_SHORT).show()
                        },
                        onDelete = {
                            userToDelete = user
                        }
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (userToDelete != null) {
        val target = userToDelete!!
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(20.dp),
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFEF4444))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Account?", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Text(
                    "Are you sure you want to permanently delete the profile of '${target.displayName}' (${target.email ?: target.phone ?: target.id})? They will lose all access immediately.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        repository.deleteUser(target.id)
                        userToDelete = null
                        Toast.makeText(context, "Account deleted", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444), contentColor = Color.White),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Delete Account", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun UserApprovalCard(
    user: UserEntity,
    accentColor: Color,
    isDarkTheme: Boolean,
    onApprove: () -> Unit,
    onRevoke: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit
) {
    val cleanDisplayName = user.displayName.replace(Regex("[\\r\\n]+"), " ").trim()
    val cleanContact = (user.email ?: user.phone ?: "ID: ${user.id}").replace(Regex("[\\r\\n]+"), "").trim()

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

    val cardBg = if (isMasterAdmin) {
        if (isDarkTheme) Color(0xFF142038) else Color(0xFFEEF4FF)
    } else {
        if (isDarkTheme) Color(0xFF0F172A) else Color.White
    }

    val cardBorder = if (isMasterAdmin) {
        if (isDarkTheme) Color(0xFF6366F1).copy(alpha = 0.65f) else Color(0xFF6366F1).copy(alpha = 0.45f)
    } else {
        if (!user.isApproved) Color(0xFFF59E0B)
        else if (isDarkTheme) Color(0xFF1E293B)
        else Color(0xFFE2E8F0)
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = cardBg
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .border(
                1.dp,
                cardBorder,
                RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)) {
            // Header Row: Name & Role Badge
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
                                tint = if (isDarkTheme) Color(0xFFA5B4FC) else Color(0xFF4F46E5),
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
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                cleanDisplayName,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (isMasterAdmin) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isDarkTheme) Color(0xFF3B2E15) else Color(0xFFFEF3C7))
                                        .border(0.8.dp, Color(0xFFF59E0B).copy(alpha = 0.6f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                ) {
                                    Text(
                                        "MASTER",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (isDarkTheme) Color(0xFFFBBF24) else Color(0xFFB45309),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }
                        }
                        Text(
                            cleanContact,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Role & Approval Pills
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
                            color = if (isMasterAdmin) (if (isDarkTheme) Color(0xFFA5B4FC) else Color(0xFF4F46E5)) else roleColor
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
                            color = statusColor,
                            softWrap = false,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(
                color = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF1F5F9)
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Action Buttons
            if (isMasterAdmin) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "🔒 Permanent Super Admin Account",
                        fontSize = 11.sp,
                        color = Color(0xFF16A34A),
                        fontWeight = FontWeight.SemiBold
                    )
                    Text("Created: ${user.createdAt.take(10)}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (!user.isApproved) {
                            Button(
                                onClick = onApprove,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF16A34A),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Approve Access", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = onRevoke,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFFD97706).copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text("Revoke", fontSize = 11.sp, color = Color(0xFFD97706), fontWeight = FontWeight.Bold)
                            }

                            // Active / Suspended toggle
                            OutlinedButton(
                                onClick = onToggleActive,
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, if (user.isActive) Color(0xFFDC2626).copy(alpha = 0.5f) else Color(0xFF16A34A).copy(alpha = 0.5f)),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    if (user.isActive) "Suspend" else "Activate",
                                    fontSize = 11.sp,
                                    color = if (user.isActive) Color(0xFFDC2626) else Color(0xFF16A34A),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Delete Account Button
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = "Delete Account",
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
