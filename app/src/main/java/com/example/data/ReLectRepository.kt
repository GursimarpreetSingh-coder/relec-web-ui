package com.example.data

import android.content.ComponentName
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ReLectRepository private constructor(context: Context) {

    private val appContext = context.applicationContext
    private val database = AppDatabase.getDatabase(context)
    private val dao = database.reLectDao()
    private val repositoryScope = CoroutineScope(Dispatchers.IO)
    private val prefs: SharedPreferences = context.getSharedPreferences("relect_prefs", Context.MODE_PRIVATE)

    // Real-time synchronization state simulation
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _firestoreProjectName = MutableStateFlow("rattan-electricals-erp-prod")
    val firestoreProjectName: StateFlow<String> = _firestoreProjectName.asStateFlow()

    // Brand Visual Identity: "cyber_blue" (ReLect Default) vs "revolt_crimson" (RE-VOLT Power Red)
    private val _activeBrandTheme = MutableStateFlow(prefs.getString("active_brand_theme", "cyber_blue") ?: "cyber_blue")
    val activeBrandTheme: StateFlow<String> = _activeBrandTheme.asStateFlow()

    // Custom Logo File / URI paths for Blue (ReLec) and Red (ReVolt) themes
    private val _customBlueLogoUri = MutableStateFlow<String?>(
        prefs.getString("custom_blue_logo_uri", null)?.takeIf { path ->
            try {
                if (path.startsWith("content://") || path.startsWith("http")) true
                else File(path).exists()
            } catch (e: Exception) { false }
        }
    )
    val customBlueLogoUri: StateFlow<String?> = _customBlueLogoUri.asStateFlow()

    private val _customRedLogoUri = MutableStateFlow<String?>(
        prefs.getString("custom_red_logo_uri", null)?.takeIf { path ->
            try {
                if (path.startsWith("content://") || path.startsWith("http")) true
                else File(path).exists()
            } catch (e: Exception) { false }
        }
    )
    val customRedLogoUri: StateFlow<String?> = _customRedLogoUri.asStateFlow()

    fun setCustomBlueLogo(pathOrUri: String?) {
        val clean = pathOrUri?.trim()?.ifBlank { null }
        if (clean != null) {
            prefs.edit().putString("custom_blue_logo_uri", clean).apply()
        } else {
            prefs.edit().remove("custom_blue_logo_uri").apply()
        }
        _customBlueLogoUri.value = clean
    }

    fun setCustomRedLogo(pathOrUri: String?) {
        val clean = pathOrUri?.trim()?.ifBlank { null }
        if (clean != null) {
            prefs.edit().putString("custom_red_logo_uri", clean).apply()
        } else {
            prefs.edit().remove("custom_red_logo_uri").apply()
        }
        _customRedLogoUri.value = clean
    }

    fun saveCustomLogoFromUri(context: Context, uri: Uri, isRevolt: Boolean): String? {
        return try {
            val fileName = if (isRevolt) "custom_revolt_logo.png" else "custom_relec_logo.png"
            val file = File(context.filesDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            val path = file.absolutePath
            if (isRevolt) {
                setCustomRedLogo(path)
            } else {
                setCustomBlueLogo(path)
            }
            path
        } catch (e: Exception) {
            Log.e("ReLectRepository", "Failed to save custom logo: ${e.message}", e)
            null
        }
    }

    fun resetCustomLogo(isRevolt: Boolean) {
        if (isRevolt) {
            try {
                val file = File(appContext.filesDir, "custom_revolt_logo.png")
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
            setCustomRedLogo(null)
        } else {
            try {
                val file = File(appContext.filesDir, "custom_relec_logo.png")
                if (file.exists()) file.delete()
            } catch (_: Exception) {}
            setCustomBlueLogo(null)
        }
    }

    // Dashboard Banner Text Overlay toggle: controls whether text is shown over the header banner image (default false for clean artwork)
    private val _showBannerText = MutableStateFlow(prefs.getBoolean("show_banner_text", false))
    val showBannerText: StateFlow<Boolean> = _showBannerText.asStateFlow()

    fun setShowBannerText(show: Boolean) {
        prefs.edit().putBoolean("show_banner_text", show).apply()
        _showBannerText.value = show
    }

    // SMS Gateway Backend Configuration (Fast2SMS, Twilio, Webhook, Native Telephony)
    private val _smsProvider = MutableStateFlow(prefs.getString("sms_provider", "fast2sms") ?: "fast2sms")
    val smsProvider: StateFlow<String> = _smsProvider.asStateFlow()

    private val _fast2smsApiKey = MutableStateFlow(prefs.getString("fast2sms_api_key", "") ?: "")
    val fast2smsApiKey: StateFlow<String> = _fast2smsApiKey.asStateFlow()

    private val _twilioSid = MutableStateFlow(prefs.getString("twilio_account_sid", "") ?: "")
    val twilioSid: StateFlow<String> = _twilioSid.asStateFlow()

    private val _twilioToken = MutableStateFlow(prefs.getString("twilio_auth_token", "") ?: "")
    val twilioToken: StateFlow<String> = _twilioToken.asStateFlow()

    private val _twilioSender = MutableStateFlow(prefs.getString("twilio_phone_number", "") ?: "")
    val twilioSender: StateFlow<String> = _twilioSender.asStateFlow()

    private val _customSmsWebhook = MutableStateFlow(prefs.getString("custom_sms_webhook", "") ?: "")
    val customSmsWebhook: StateFlow<String> = _customSmsWebhook.asStateFlow()

    data class DispatchedOtpInfo(val code: String, val target: String, val timestamp: Long)
    private val _lastDispatchedOtp = MutableStateFlow<DispatchedOtpInfo?>(
        if (prefs.contains("last_sms_otp_code")) {
            DispatchedOtpInfo(
                code = prefs.getString("last_sms_otp_code", "") ?: "",
                target = prefs.getString("last_sms_otp_target", "") ?: "",
                timestamp = prefs.getLong("last_sms_otp_time", 0L)
            )
        } else null
    )
    val lastDispatchedOtp: StateFlow<DispatchedOtpInfo?> = _lastDispatchedOtp.asStateFlow()

    fun setLastDispatchedOtp(code: String, target: String) {
        _lastDispatchedOtp.value = DispatchedOtpInfo(code, target, System.currentTimeMillis())
        prefs.edit()
            .putString("last_sms_otp_code", code)
            .putString("last_sms_otp_target", target)
            .putLong("last_sms_otp_time", System.currentTimeMillis())
            .apply()
    }

    fun saveSmsGatewayConfig(
        provider: String,
        fast2smsKey: String,
        twilioSidVal: String,
        twilioTokenVal: String,
        twilioSenderVal: String,
        webhookUrl: String
    ) {
        prefs.edit()
            .putString("sms_provider", provider)
            .putString("fast2sms_api_key", fast2smsKey)
            .putString("twilio_account_sid", twilioSidVal)
            .putString("twilio_auth_token", twilioTokenVal)
            .putString("twilio_phone_number", twilioSenderVal)
            .putString("custom_sms_webhook", webhookUrl)
            .apply()
        _smsProvider.value = provider
        _fast2smsApiKey.value = fast2smsKey
        _twilioSid.value = twilioSidVal
        _twilioToken.value = twilioTokenVal
        _twilioSender.value = twilioSenderVal
        _customSmsWebhook.value = webhookUrl
    }

    // Custom Brand Accent Color (Electric Blue, Crimson Red, Electric Orange, Cyber Purple, Success Green)
    private val _customAccentColor = MutableStateFlow<Long?>(
        if (prefs.contains("custom_accent_color")) prefs.getLong("custom_accent_color", 0L) else null
    )
    val customAccentColor: StateFlow<Long?> = _customAccentColor.asStateFlow()

    fun setCustomAccentColor(colorVal: Long?) {
        if (colorVal != null) {
            prefs.edit().putLong("custom_accent_color", colorVal).apply()
        } else {
            prefs.edit().remove("custom_accent_color").apply()
        }
        _customAccentColor.value = colorVal
    }

    // Dynamic Admin-Controlled Customer Support Desk Information
    private val _storeSupportInfo = MutableStateFlow(
        StoreSupportInfo(
            storeName = prefs.getString("support_store_name", "Rattan Electricals Helpdesk") ?: "Rattan Electricals Helpdesk",
            phone = prefs.getString("support_phone", "9876543210") ?: "9876543210",
            whatsapp = prefs.getString("support_whatsapp", "9876543210") ?: "9876543210",
            address = prefs.getString("support_address", "Shop #12, Main Electrical Market, Near Clock Tower") ?: "Shop #12, Main Electrical Market, Near Clock Tower",
            workingHours = prefs.getString("support_hours", "Mon - Sat (9:00 AM – 8:30 PM)") ?: "Mon - Sat (9:00 AM – 8:30 PM)",
            emergencyHelpline = prefs.getString("support_emergency", "+91 98765 43210") ?: "+91 98765 43210",
            safetyGuidelines = prefs.getString("support_safety", "In case of smoke or burning smell, instantly switch off the Main MCB.\nNever touch appliance switches with wet hands or barefoot on wet floors.\nDo not overload multi-plug strips with heavy loads like geysers or heaters.\nAlways ensure copper earthing wire is connected to your power socket.") ?: "",
            appDownloadUrl = prefs.getString("custom_app_qr_url", "https://ais-pre-x5noeypzgmvkg3noct2hto-828738081727.asia-east1.run.app/?view=customer") ?: "https://ais-pre-x5noeypzgmvkg3noct2hto-828738081727.asia-east1.run.app/?view=customer"
        )
    )
    val storeSupportInfo: StateFlow<StoreSupportInfo> = _storeSupportInfo.asStateFlow()
    val supportInfo: StateFlow<StoreSupportInfo> = storeSupportInfo

    private val _customAppQrUrl = MutableStateFlow(
        prefs.getString("custom_app_qr_url", "https://ais-pre-x5noeypzgmvkg3noct2hto-828738081727.asia-east1.run.app/?view=customer")
            ?: "https://ais-pre-x5noeypzgmvkg3noct2hto-828738081727.asia-east1.run.app/?view=customer"
    )
    val customAppQrUrl: StateFlow<String> = _customAppQrUrl.asStateFlow()

    fun updateCustomAppQrUrl(url: String) {
        val cleanUrl = url.trim().ifBlank {
            "https://ais-pre-x5noeypzgmvkg3noct2hto-828738081727.asia-east1.run.app/?view=customer"
        }
        prefs.edit().putString("custom_app_qr_url", cleanUrl).apply()
        _customAppQrUrl.value = cleanUrl
        val current = _storeSupportInfo.value
        _storeSupportInfo.value = current.copy(appDownloadUrl = cleanUrl)
    }

    fun updateStoreSupportInfo(info: StoreSupportInfo) {
        prefs.edit()
            .putString("support_store_name", info.storeName)
            .putString("support_phone", info.phone)
            .putString("support_whatsapp", info.whatsapp)
            .putString("support_address", info.address)
            .putString("support_hours", info.workingHours)
            .putString("support_emergency", info.emergencyHelpline)
            .putString("support_safety", info.safetyGuidelines)
            .putString("custom_app_qr_url", info.appDownloadUrl)
            .apply()
        _storeSupportInfo.value = info
        _customAppQrUrl.value = info.appDownloadUrl
    }

    fun updateSupportInfo(info: StoreSupportInfo) = updateStoreSupportInfo(info)

    init {
        // Automatically wipe simulated/dummy legacy data once so actual customer entries can be recorded
        val hasPurgedLegacyMockData = prefs.getBoolean("has_purged_legacy_mock_data_v2", false)
        if (!hasPurgedLegacyMockData) {
            prefs.edit().putBoolean("has_purged_legacy_mock_data_v2", true).apply()
            clearAllToyData()
        }
    }

    fun setBrandTheme(themeId: String) {
        if (_activeBrandTheme.value == themeId) return
        prefs.edit().putString("active_brand_theme", themeId).apply()
        _activeBrandTheme.value = themeId
        setCustomAccentColor(null) // Reset custom accent to follow new brand default
        updateLauncherIcon(appContext, themeId == "revolt_crimson")
    }

    // State Flows for each module (using Room Flow under the hood)
    val customers: StateFlow<List<Customer>> = dao.getCustomersFlow()
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    val services: StateFlow<List<ServiceRequest>> = dao.getServicesFlow()
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    val workers: StateFlow<List<Worker>> = dao.getWorkersFlow()
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    val inventory: StateFlow<List<InventoryItem>> = dao.getInventoryFlow()
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    val purchases: StateFlow<List<PurchaseRecord>> = dao.getPurchasesFlow()
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    val bills: StateFlow<List<Bill>> = dao.getBillsFlow()
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    val expenses: StateFlow<List<Expense>> = dao.getExpensesFlow()
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    val attendance: StateFlow<List<AttendanceRecord>> = dao.getAttendanceFlow()
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    val users: StateFlow<List<UserEntity>> = dao.getUsersFlow()
        .stateIn(repositoryScope, SharingStarted.Eagerly, emptyList())

    fun setUserApproval(userId: String, isApproved: Boolean) {
        repositoryScope.launch {
            val user = dao.getUserById(userId)
            if (user != null) {
                dao.updateUser(user.copy(isApproved = isApproved))
                if (user.role.equals("Worker", ignoreCase = true)) {
                    val matchedWorker = workers.value.find { it.phone == user.phone || it.email == user.email || it.id == userId }
                    if (matchedWorker != null) {
                        dao.updateWorker(matchedWorker.copy(isApproved = isApproved, isVerified = isApproved))
                    }
                }
            }
        }
    }

    fun setUserActive(userId: String, isActive: Boolean) {
        repositoryScope.launch {
            val user = dao.getUserById(userId)
            if (user != null) {
                dao.updateUser(user.copy(isActive = isActive))
                if (user.role.equals("Worker", ignoreCase = true)) {
                    val matchedWorker = workers.value.find { it.phone == user.phone || it.email == user.email || it.id == userId }
                    if (matchedWorker != null) {
                        dao.updateWorker(matchedWorker.copy(isActive = isActive))
                    }
                }
            }
        }
    }

    fun deleteUser(userId: String) {
        repositoryScope.launch {
            dao.deleteUser(userId)
        }
    }

    fun approveUser(userId: String) {
        setUserApproval(userId, true)
    }

    fun toggleUserActive(userId: String) {
        repositoryScope.launch {
            val user = dao.getUserById(userId)
            if (user != null) {
                setUserActive(userId, !user.isActive)
            }
        }
    }

    fun updateUser(user: UserEntity) {
        repositoryScope.launch {
            dao.updateUser(user)
        }
    }

    fun insertUser(user: UserEntity) {
        repositoryScope.launch {
            dao.insertUser(user)
        }
    }

    init {
        // In production mode, do not auto-inject mock records on startup.
        // The database remains pristine, populated only by authentic user activities.
    }

    fun clearAllToyData() {
        prefs.edit().putBoolean("is_toy_data_cleared", true).apply()
        repositoryScope.launch {
            try {
                dao.clearCustomers()
                dao.clearServiceRequests()
                dao.clearWorkers()
                dao.clearInventory()
                dao.clearPurchases()
                dao.clearBills()
                dao.clearExpenses()
                dao.clearAttendance()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        triggerSync()
    }

    fun restoreDemoData() {
        prefs.edit().putBoolean("is_toy_data_cleared", false).apply()
        repositoryScope.launch {
            try {
                // Clear first to prevent duplicates
                dao.clearCustomers()
                dao.clearServiceRequests()
                dao.clearWorkers()
                dao.clearInventory()
                dao.clearPurchases()
                dao.clearBills()
                dao.clearExpenses()
                dao.clearAttendance()

                // Insert seeds
                getSeedCustomers().forEach { dao.insertCustomer(it) }
                getSeedServices().forEach { dao.insertServiceRequest(it) }
                getSeedWorkers().forEach { dao.insertWorker(it) }
                getSeedInventory().forEach { dao.insertInventoryItem(it) }
                getSeedPurchases().forEach { dao.insertPurchaseRecord(it) }
                getSeedBills().forEach { dao.insertBill(it) }
                getSeedExpenses().forEach { dao.insertExpense(it) }
                getSeedAttendance().forEach { dao.insertAttendanceRecord(it) }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        triggerSync()
    }

    private fun triggerSync() {
        _isSyncing.value = true
        // Quick background reset
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            _isSyncing.value = false
        }, 1200)
    }

    fun setFirestoreProject(name: String) {
        _firestoreProjectName.value = name
        triggerSync()
    }

    // --- Core Operations ---

    // Customers
    fun addCustomer(customer: Customer) {
        repositoryScope.launch {
            dao.insertCustomer(customer)
        }
        triggerSync()
    }

    fun updateCustomer(updated: Customer) {
        repositoryScope.launch {
            dao.updateCustomer(updated)
        }
        triggerSync()
    }

    fun deleteCustomer(id: String) {
        repositoryScope.launch {
            dao.deleteCustomer(id)
        }
        triggerSync()
    }

    // Service Requests
    fun addServiceRequest(request: ServiceRequest) {
        repositoryScope.launch {
            dao.insertServiceRequest(request)
        }
        triggerSync()
    }

    fun updateServiceRequest(updated: ServiceRequest) {
        repositoryScope.launch {
            dao.updateServiceRequest(updated)
        }
        triggerSync()
    }

    fun deleteServiceRequest(id: String) {
        repositoryScope.launch {
            dao.deleteServiceRequest(id)
        }
        triggerSync()
    }

    // Workers
    fun addWorker(worker: Worker) {
        repositoryScope.launch {
            dao.insertWorker(worker)
        }
        triggerSync()
    }

    fun updateWorker(updated: Worker) {
        repositoryScope.launch {
            dao.updateWorker(updated)
        }
        triggerSync()
    }

    fun deleteWorker(id: String) {
        repositoryScope.launch {
            dao.deleteWorker(id)
        }
        triggerSync()
    }

    // Inventory
    fun addInventoryItem(item: InventoryItem) {
        repositoryScope.launch {
            dao.insertInventoryItem(item)
        }
        triggerSync()
    }

    fun updateInventoryItem(updated: InventoryItem) {
        repositoryScope.launch {
            dao.updateInventoryItem(updated)
        }
        triggerSync()
    }

    fun deleteInventoryItem(id: String) {
        repositoryScope.launch {
            dao.deleteInventoryItem(id)
        }
        triggerSync()
    }

    // Purchase Records
    fun addPurchaseRecord(record: PurchaseRecord) {
        repositoryScope.launch {
            dao.insertPurchaseRecord(record)

            // Sync with inventory stock level
            val inventoryList = dao.getInventoryFlow().first()
            val matchedInv = inventoryList.find { it.name.equals(record.itemName, ignoreCase = true) }
            if (matchedInv != null) {
                dao.updateInventoryItem(matchedInv.copy(stockLevel = matchedInv.stockLevel + record.quantity))
            } else {
                // Create a new item if not exists
                dao.insertInventoryItem(InventoryItem(
                    name = record.itemName,
                    category = "General",
                    stockLevel = record.quantity,
                    minAlertLevel = 5,
                    supplierName = record.supplierName,
                    price = record.cost / record.quantity
                ))
            }
        }
        triggerSync()
    }

    // Bills
    fun addBill(bill: Bill) {
        repositoryScope.launch {
            dao.insertBill(bill)

            // Decrement inventory stock
            val inventoryList = dao.getInventoryFlow().first()
            bill.items.forEach { item ->
                val matchedInv = inventoryList.find { it.name.equals(item.name, ignoreCase = true) }
                if (matchedInv != null) {
                    val newStock = (matchedInv.stockLevel - item.qty).coerceAtLeast(0)
                    dao.updateInventoryItem(matchedInv.copy(stockLevel = newStock))
                }
            }
        }
        triggerSync()
    }

    fun updateBill(bill: Bill) {
        repositoryScope.launch {
            dao.updateBill(bill)
        }
        triggerSync()
    }

    fun deleteBill(id: String) {
        repositoryScope.launch {
            dao.deleteBill(id)
        }
        triggerSync()
    }

    // Expenses
    fun addExpense(expense: Expense) {
        repositoryScope.launch {
            dao.insertExpense(expense)
        }
        triggerSync()
    }

    fun deleteExpense(id: String) {
        repositoryScope.launch {
            dao.deleteExpense(id)
        }
        triggerSync()
    }

    // --- Worker Attendance Operations ---
    fun addAttendanceRecord(record: AttendanceRecord) {
        repositoryScope.launch {
            dao.insertAttendanceRecord(record)
        }
        triggerSync()
    }

    fun updateAttendanceRecord(updated: AttendanceRecord) {
        repositoryScope.launch {
            dao.updateAttendanceRecord(updated)
        }
        triggerSync()
    }

    // --- Voice NLP Natural Speech Parser ---
    fun parseNaturalVoiceInput(transcript: String): ParsedVoiceData {
        val text = transcript.lowercase(Locale.ROOT)
        var item = "General Electricals"
        var qty = 1
        var amount = 150.0
        var supplier = "Local Distributor"

        try {
            val numbers = "\\d+".toRegex().findAll(text).map { it.value.toInt() }.toList()

            // 1. Expense parsing
            if (text.contains("tea") || text.contains("chai") || text.contains("chah")) {
                item = "Chai / Tea Refreshments"
                amount = if (numbers.isNotEmpty()) numbers.first().toDouble() else 40.0
                return ParsedVoiceData(item, 1, amount, "Worker Expense")
            }
            if (text.contains("petrol") || text.contains("conveyance") || text.contains("fuel")) {
                item = "Petrol / Conveyance"
                amount = if (numbers.isNotEmpty()) numbers.first().toDouble() else 150.0
                return ParsedVoiceData(item, 1, amount, "Travel")
            }

            // 2. Identify items
            if (text.contains("fan") || text.contains("pankha") || text.contains("pankhe")) {
                item = "Orient Ceiling Fan"
            } else if (text.contains("switch") || text.contains("board") || text.contains("button")) {
                item = "Anchor Modular Switch"
            } else if (text.contains("wire") || text.contains("taar") || text.contains("cable")) {
                item = "Havells 1.5 Sqmm Wire"
            } else if (text.contains("mcb") || text.contains("fuse")) {
                item = "Luminous 16A MCB"
            } else if (text.contains("motor") || text.contains("tullu")) {
                item = "Crompton 1HP Motor"
            } else if (text.contains("capacitor") || text.contains("condenser")) {
                item = "Keltron 2.5uF Capacitor"
            }

            // 3. Suppliers
            if (text.contains("havells")) supplier = "Havells India Ltd"
            else if (text.contains("anchor")) supplier = "Anchor Panasonic"
            else if (text.contains("luminous")) supplier = "Luminous Power"
            else if (text.contains("crompton")) supplier = "Crompton Greaves"

            // 4. Extract Qty & Cost intelligently
            if (numbers.size >= 2) {
                val sorted = numbers.sorted()
                qty = sorted[0]
                amount = sorted[1].toDouble()
            } else if (numbers.size == 1) {
                amount = numbers[0].toDouble()
                qty = 1
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ParsedVoiceData(item, qty, amount, supplier)
    }

    fun parseServiceRequestVoice(transcript: String): ParsedServiceRequestData {
        val text = transcript.lowercase(Locale.ROOT)
        
        var phone = ""
        var name = "Walk-in Customer"
        var address = "Shop Address"
        var problem = transcript

        try {
            val phoneRegex = "\\b\\d{10}\\b".toRegex()
            val phoneMatch = phoneRegex.find(text)
            if (phoneMatch != null) {
                phone = phoneMatch.value
            }

            val namePatterns = listOf(
                "मेरा नाम ([a-z\\s]+) है",
                "ਮੇਰਾ ਨਾਮ ([a-z\\s]+) ਹੈ",
                "my name is ([a-z\\s]+)",
                "naam hai ([a-z\\s]+)",
                "naam ([a-z\\s]+)"
            )
            for (pattern in namePatterns) {
                val regex = pattern.toRegex()
                val match = regex.find(text)
                if (match != null && match.groupValues.size > 1) {
                    val extracted = match.groupValues[1].trim()
                    if (extracted.isNotBlank() && !extracted.contains("phone") && !extracted.contains("address") && !extracted.contains("mobi")) {
                        name = extracted.split(" ").take(3).joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                        break
                    }
                }
            }

            val addressPatterns = listOf(
                "रहता हूँ ([a-z0-9\\s,]+)",
                "ਰਹਿੰਦਾ ਹਾਂ ([a-z0-9\\s,]+)",
                "address is ([a-z0-9\\s,]+)",
                "address ([a-z0-9\\s,]+)"
            )
            for (pattern in addressPatterns) {
                val regex = pattern.toRegex()
                val match = regex.find(text)
                if (match != null && match.groupValues.size > 1) {
                    val extracted = match.groupValues[1].trim()
                    if (extracted.isNotBlank()) {
                        address = extracted.replaceFirstChar { it.uppercase() }
                        break
                    }
                }
            }
            if (address == "Shop Address" || address.isBlank()) {
                val commonKeywords = listOf("sector", "road", "gali", "lines", "nagar", "avenue", "colony", "house", "h.no")
                for (kw in commonKeywords) {
                    if (text.contains(kw)) {
                        val idx = text.indexOf(kw)
                        address = text.substring(idx).split("and", "for", "please", "te", "aur").first().trim().replaceFirstChar { it.uppercase() }
                        break
                    }
                }
            }

            val problemKeywords = listOf("spark", "tripping", "short", "fan", "motor", "wiring", "mcb", "fuse", "lighting", "pump", "inverter")
            for (kw in problemKeywords) {
                if (text.contains(kw)) {
                    val idx = text.indexOf(kw)
                    problem = transcript.substring(idx).trim().replaceFirstChar { it.uppercase() }
                    break
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ParsedServiceRequestData(
            name = name,
            phone = phone.ifBlank { "9872134590" },
            address = address,
            problem = problem
        )
    }

    fun parseWorkerVoice(transcript: String): ParsedWorkerData {
        val text = transcript.lowercase(Locale.ROOT)
        var name = "New Electrician"
        var phone = ""
        var salary = 15000.0
        var designation = "Electrician"

        try {
            val phoneRegex = "\\b\\d{10}\\b".toRegex()
            val phoneMatch = phoneRegex.find(text)
            if (phoneMatch != null) {
                phone = phoneMatch.value
            }

            val salaryRegex = "\\b\\d{4,6}\\b".toRegex()
            val salaryMatch = salaryRegex.find(text)
            if (salaryMatch != null) {
                salary = salaryMatch.value.toDouble()
            } else if (text.contains("bees") || text.contains("veeh")) {
                salary = 20000.0
            } else if (text.contains("pandra") || text.contains("panjra")) {
                salary = 15000.0
            } else if (text.contains("bara") || text.contains("barah")) {
                salary = 12000.0
            } else if (text.contains("pachis") || text.contains("panji")) {
                salary = 25000.0
            }

            val namePatterns = listOf(
                "worker ([a-z\\s]+) phone",
                "worker ([a-z\\s]+) mobile",
                "worker ([a-z\\s]+) salary",
                "naam ([a-z\\s]+) hai",
                "name is ([a-z\\s]+)"
            )
            for (pattern in namePatterns) {
                val regex = pattern.toRegex()
                val match = regex.find(text)
                if (match != null && match.groupValues.size > 1) {
                    name = match.groupValues[1].trim().split(" ").take(2).joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
                    break
                }
            }

            if (text.contains("helper") || text.contains("chotta") || text.contains("madadgar")) {
                designation = "Helper Technician"
            } else if (text.contains("senior") || text.contains("bada") || text.contains("vadda") || text.contains("head")) {
                designation = "Senior Electrician"
            } else if (text.contains("electrician") || text.contains("bijli")) {
                designation = "Electrician"
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return ParsedWorkerData(name, phone.ifBlank { "9501569922" }, salary, designation)
    }

    data class ParsedServiceRequestData(
        val name: String,
        val phone: String,
        val address: String,
        val problem: String,
        val category: String = "Wiring Check"
    )

    data class ParsedWorkerData(
        val name: String,
        val phone: String,
        val salary: Double,
        val designation: String
    )

    data class ParsedVoiceData(
        val item: String,
        val quantity: Int,
        val amount: Double,
        val supplier: String,
        val category: String = "General"
    )

    data class StoreSupportInfo(
        val storeName: String = "Rattan Electricals Helpdesk",
        val phone: String = "9876543210",
        val whatsapp: String = "9876543210",
        val address: String = "Shop #12, Main Electrical Market, Near Clock Tower",
        val workingHours: String = "Mon - Sat (9:00 AM – 8:30 PM)",
        val emergencyHelpline: String = "+91 98765 43210",
        val safetyGuidelines: String = "In case of smoke or burning smell, instantly switch off the Main MCB.\nNever touch appliance switches with wet hands or barefoot on wet floors.\nDo not overload multi-plug strips with heavy loads like geysers or heaters.\nAlways ensure copper earthing wire is connected to your power socket.",
        val appDownloadUrl: String = "https://ais-pre-x5noeypzgmvkg3noct2hto-828738081727.asia-east1.run.app/?view=customer"
    )

    // --- Seed Data Suppliers ---

    private fun getSeedCustomers() = listOf(
        Customer(name = "Rajesh Kumar", phone = "9812045612", address = "H.No 142, Sector 15, Chandigarh", joinedDate = "2026-03-10"),
        Customer(name = "Anita Sharma", phone = "9417255831", address = "Sco 42, Phase 3B2, Mohali", joinedDate = "2026-04-22"),
        Customer(name = "Gurpreet Singh", phone = "9872134590", address = "Green Avenue, Airport Road, Amritsar", joinedDate = "2026-01-15"),
        Customer(name = "Amit Patel", phone = "8146312456", address = "Sector 22-C, Chandigarh", joinedDate = "2026-05-18"),
        Customer(name = "Priya Bansal", phone = "9501567234", address = "Street No. 3, Civil Lines, Ludhiana", joinedDate = "2026-06-05")
    )

    private fun getSeedServices() = listOf(
        ServiceRequest(
            customerName = "Anita Sharma",
            customerPhone = "9417255831",
            customerAddress = "Sco 42, Phase 3B2, Mohali",
            problemDescription = "Main MCB tripping repeatedly when AC is switched on. Burning smell from DB board.",
            category = "DB Board Repair",
            priority = Priority.HIGH,
            assignedWorkerId = "W1",
            assignedWorkerName = "Sukhvinder Singh",
            status = ServiceStatus.IN_PROGRESS,
            preferredDateTime = "2026-07-01 11:30 AM",
            dateCreated = "2026-07-01"
        ),
        ServiceRequest(
            customerName = "Rajesh Kumar",
            customerPhone = "9812045612",
            customerAddress = "H.No 142, Sector 15, Chandigarh",
            problemDescription = "Fitting 3 new Orient ceiling fans in bedrooms and drawing room.",
            category = "Fan Installation",
            priority = Priority.MEDIUM,
            assignedWorkerId = "W2",
            assignedWorkerName = "Baljinder Singh",
            status = ServiceStatus.ASSIGNED,
            preferredDateTime = "2026-07-02 02:00 PM",
            dateCreated = "2026-07-01"
        ),
        ServiceRequest(
            customerName = "Gurpreet Singh",
            customerPhone = "9872134590",
            customerAddress = "Green Avenue, Airport Road, Amritsar",
            problemDescription = "Water pump motor capacitor has burst. Motor humming but not pumping water.",
            category = "Motor Repair",
            priority = Priority.EMERGENCY,
            status = ServiceStatus.CREATED,
            preferredDateTime = "2026-07-01 09:00 AM",
            dateCreated = "2026-07-01"
        ),
        ServiceRequest(
            customerName = "Amit Patel",
            customerPhone = "8146312456",
            customerAddress = "Sector 22-C, Chandigarh",
            problemDescription = "Inverter backup line connection wiring check.",
            category = "Wiring Check",
            priority = Priority.LOW,
            assignedWorkerId = "W1",
            assignedWorkerName = "Sukhvinder Singh",
            status = ServiceStatus.COMPLETED,
            preferredDateTime = "2026-06-29 04:00 PM",
            dateCreated = "2026-06-29"
        )
    )

    private fun getSeedWorkers() = listOf(
        Worker(id = "W1", name = "Sukhvinder Singh", phone = "9821034455", salary = 24000.0, designation = "Senior Electrician", status = WorkerStatus.WORKING, joiningDate = "2024-04-01"),
        Worker(id = "W2", name = "Baljinder Singh", phone = "9501562211", salary = 18000.0, designation = "Electrician", status = WorkerStatus.ASSIGNED_NOT_STARTED, joiningDate = "2025-01-15"),
        Worker(id = "W3", name = "Harpreet Singh", phone = "8146399002", salary = 12000.0, designation = "Helper Technician", status = WorkerStatus.IDLE, joiningDate = "2025-10-10")
    )

    private fun getSeedInventory() = listOf(
        InventoryItem(id = "I1", name = "Havells 1.5 Sqmm Wire (Red)", category = "Wires & Cables", stockLevel = 4, minAlertLevel = 10, supplierName = "Havells India Ltd", price = 1450.0),
        InventoryItem(id = "I2", name = "Anchor Modular Switch 6A", category = "Switches & Sockets", stockLevel = 85, minAlertLevel = 20, supplierName = "Anchor Panasonic", price = 22.0),
        InventoryItem(id = "I3", name = "Luminous 16A Single Pole MCB", category = "Switchgears", stockLevel = 12, minAlertLevel = 15, supplierName = "Luminous Power", price = 160.0),
        InventoryItem(id = "I4", name = "Orient 48\" Ceiling Fan Black", category = "Fans & Cooling", stockLevel = 3, minAlertLevel = 5, supplierName = "Orient Electric", price = 2100.0),
        InventoryItem(id = "I5", name = "Crompton 1HP Tullu Water Pump", category = "Motors & Pumps", stockLevel = 15, minAlertLevel = 3, supplierName = "Crompton Greaves", price = 4800.0),
        InventoryItem(id = "I6", name = "Keltron 2.5uF Fan Capacitor", category = "Capacitors", stockLevel = 150, minAlertLevel = 50, supplierName = "Keltron Ltd", price = 35.0)
    )

    private fun getSeedPurchases() = listOf(
        PurchaseRecord(itemName = "Anchor Modular Switch 6A", quantity = 100, supplierName = "Anchor Panasonic", cost = 1800.0, date = "2026-06-28"),
        PurchaseRecord(itemName = "Luminous 16A Single Pole MCB", quantity = 30, supplierName = "Luminous Power", cost = 3900.0, date = "2026-06-25"),
        PurchaseRecord(itemName = "Orient 48\" Ceiling Fan Black", quantity = 10, supplierName = "Orient Electric", cost = 17500.0, date = "2026-06-20")
    )

    private fun getSeedBills() = listOf(
        Bill(
            customerName = "Amit Patel",
            customerPhone = "8146312456",
            items = listOf(
                BillItem("Anchor Modular Switch 6A", 5, 25.0, 125.0),
                BillItem("Keltron 2.5uF Fan Capacitor", 1, 45.0, 45.0)
            ),
            laborCharges = 350.0,
            gstPercent = 18.0,
            totalAmount = 613.6,
            date = "2026-06-29",
            isDownloaded = true
        )
    )

    private fun getSeedExpenses() = listOf(
        Expense(category = "Tea & Refreshments", title = "Evening tea & snacks for staff", amount = 85.0, quantity = 1, date = "2026-07-01"),
        Expense(category = "Travel & Fuel", title = "Site check-in Phase 3B2 Mohali", amount = 150.0, quantity = 1, date = "2026-07-01"),
        Expense(category = "Shop Maintenance", title = "Catalog rack cleaning supplies", amount = 120.0, quantity = 1, date = "2026-06-30")
    )

    private fun getSeedAttendance() = listOf(
        AttendanceRecord(workerId = "W1", workerName = "Sukhvinder Singh", clockInTime = "09:00 AM", clockOutTime = "06:00 PM", date = "2026-06-30", isClockedIn = false),
        AttendanceRecord(workerId = "W2", workerName = "Baljinder Singh", clockInTime = "09:15 AM", clockOutTime = "06:15 PM", date = "2026-06-30", isClockedIn = false),
        AttendanceRecord(workerId = "W1", workerName = "Sukhvinder Singh", clockInTime = "08:55 AM", clockOutTime = null, date = "2026-07-01", isClockedIn = true)
    )

    companion object {
        @Volatile
        private var INSTANCE: ReLectRepository? = null

        fun getInstance(context: Context): ReLectRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ReLectRepository(context.applicationContext).also { INSTANCE = it }
            }
        }

        fun updateLauncherIcon(context: Context, isRevolt: Boolean) {
            Log.d("ReLectRepository", "Active brand theme registered: isRevolt=$isRevolt")
        }
    }
}
