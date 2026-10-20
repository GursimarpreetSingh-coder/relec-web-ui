package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "customers")
data class Customer(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val address: String,
    val joinedDate: String = "2026-01-15"
)

enum class ServiceStatus(val label: String) {
    CREATED("Created"),
    ASSIGNED("Assigned"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    CLOSED("Closed")
}

enum class Priority(val label: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    EMERGENCY("Emergency")
}

@Entity(tableName = "services")
data class ServiceRequest(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerName: String,
    val customerPhone: String,
    val customerAddress: String,
    val problemDescription: String,
    val category: String,
    val priority: Priority = Priority.MEDIUM,
    val assignedWorkerId: String? = null,
    val assignedWorkerName: String? = null,
    val status: ServiceStatus = ServiceStatus.CREATED,
    val preferredDateTime: String = "2026-07-02 10:00 AM",
    val photoUri: String? = null,
    val dateCreated: String = "2026-07-01",
    val statusHistory: List<StatusChange> = listOf(StatusChange(ServiceStatus.CREATED, "2026-07-01"))
)

data class StatusChange(
    val status: ServiceStatus,
    val timestamp: String,
    val notes: String = "Status updated"
)

enum class WorkerStatus(val label: String) {
    WORKING("Currently Working"),         // Green
    ASSIGNED_NOT_STARTED("Assigned Not Started"), // Yellow
    IDLE("Available / Idle")              // Red
}

@Entity(tableName = "workers")
data class Worker(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val phone: String,
    val salary: Double,
    val designation: String, // Electrician, Helper, Senior Technician
    val status: WorkerStatus = WorkerStatus.IDLE,
    val joiningDate: String = "2025-05-10",
    val currentJobId: String? = null,
    val isVerified: Boolean = false,
    val isApproved: Boolean = false,
    val isActive: Boolean = true,
    val email: String? = null
)

@Entity(tableName = "inventory")
data class InventoryItem(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val category: String,
    val stockLevel: Int,
    val minAlertLevel: Int,
    val supplierName: String,
    val price: Double
)

@Entity(tableName = "purchases")
data class PurchaseRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val itemName: String,
    val quantity: Int,
    val supplierName: String,
    val cost: Double,
    val date: String = "2026-07-01",
    val photoUri: String? = null
)

data class BillItem(
    val name: String,
    val qty: Int,
    val rate: Double,
    val total: Double
)

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val customerName: String,
    val customerPhone: String,
    val items: List<BillItem>,
    val laborCharges: Double = 0.0,
    val gstPercent: Double = 18.0,
    val discountAmount: Double = 0.0,
    val totalAmount: Double,
    val date: String = "2026-07-01",
    val isDownloaded: Boolean = false,
    val storeName: String? = "Rattan Electricals",
    val invoiceNumber: String? = null,
    val paymentMethod: String? = "Cash",
    val isMathematicallyValid: Boolean = true,
    val discrepancyNote: String? = null,
    val sentToCustomerAt: String? = null,
    val sentVia: String? = null,
    val paymentStatus: String = "Paid",
    val balanceDue: Double = 0.0
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val category: String,
    val title: String,
    val amount: Double,
    val quantity: Int = 1,
    val date: String = "2026-07-01",
    val voiceSummary: String? = null
)

@Entity(tableName = "attendance")
data class AttendanceRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val workerId: String,
    val workerName: String,
    val clockInTime: String,
    val clockOutTime: String? = null,
    val date: String = "2026-07-01",
    val isClockedIn: Boolean = true
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val email: String? = null,
    val phone: String? = null,
    val displayName: String,
    val passwordHash: String = "",
    val salt: String = "",
    val role: String, // "Admin", "Worker", "Customer"
    val isApproved: Boolean = true,
    val isActive: Boolean = true,
    val failedAttempts: Int = 0,
    val lockUntilTimestamp: Long = 0L,
    val createdAt: String
)

@Entity(tableName = "otp_records")
data class OtpRecord(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val target: String, // phone or email
    val otpCode: String,
    val purpose: String, // "LOGIN", "SIGNUP", "RESET_PASSWORD"
    val expiresAt: Long,
    val attempts: Int = 0,
    val isUsed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "audit_logs")
data class AuditLogEntry(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val userId: String?,
    val userName: String,
    val action: String,
    val details: String,
    val timestamp: String
)

data class ScannedInvoiceItem(
    val name: String,
    val quantity: Int,
    val unitPrice: Double,
    val lineTotal: Double
)

data class ScannedInvoiceResult(
    val storeName: String,
    val invoiceNumber: String?,
    val date: String,
    val time: String?,
    val items: List<ScannedInvoiceItem>,
    val subtotal: Double,
    val taxPercent: Double,
    val taxAmount: Double,
    val discountAmount: Double,
    val grandTotal: Double,
    val paymentMethod: String?,
    val isMathematicallyValid: Boolean,
    val discrepancyNote: String?,
    val confidenceScore: Float
)

data class VisualIdentificationResult(
    val isInvoice: Boolean,
    val title: String,
    val brand: String,
    val category: String,
    val quantity: Int = 1,
    val unit: String = "pcs",
    val estimatedUnitPrice: Double = 0.0,
    val estimatedTotalPrice: Double = 0.0,
    val specifications: String = "",
    val invoiceDetails: ScannedInvoiceResult? = null,
    val detectedTags: List<String> = emptyList(),
    val rawDescription: String = "",
    val confidence: Float = 0.95f
)

