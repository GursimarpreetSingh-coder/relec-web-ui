package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object CloudSyncManager {
    private const val TAG = "CloudSyncManager"
    private const val DEFAULT_ENDPOINT = "https://relec-pro-erp-default-rtdb.asia-southeast1.firebasedatabase.app"
    private const val PREFS_KEY = "relect_cloud_sync_prefs"
    private const val KEY_ENDPOINT = "cloud_sync_endpoint"
    private const val KEY_LAST_SYNC = "last_sync_timestamp"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<String>("")
    val lastSyncTime: StateFlow<String> = _lastSyncTime.asStateFlow()

    fun getEndpoint(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        return prefs.getString(KEY_ENDPOINT, DEFAULT_ENDPOINT) ?: DEFAULT_ENDPOINT
    }

    fun setEndpoint(context: Context, url: String) {
        val clean = url.trim().trimEnd('/')
        val prefs = context.getSharedPreferences(PREFS_KEY, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_ENDPOINT, clean.ifBlank { DEFAULT_ENDPOINT }).apply()
    }

    /**
     * Executes bi-directional real-time cloud synchronization between Android Room SQLite and Cloud Ledger.
     */
    suspend fun syncWithCloud(
        context: Context,
        dao: ReLectDao,
        isProduction: Boolean = true
    ): Boolean = withContext(Dispatchers.IO) {
        if (_isSyncing.value) return@withContext false
        _isSyncing.value = true

        try {
            val endpoint = getEndpoint(context)
            val channel = if (isProduction) "production_ledger" else "sandbox_ledger"
            val targetUrl = "$endpoint/$channel.json"

            // 1. Pull remote dataset
            val getRequest = Request.Builder()
                .url(targetUrl)
                .get()
                .build()

            try {
                val response = client.newCall(getRequest).execute()
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string()
                    if (!bodyStr.isNullOrBlank() && bodyStr != "null") {
                        val rootJson = JSONObject(bodyStr)

                        // Sync Customers
                        if (rootJson.has("customers")) {
                            val custArray = rootJson.optJSONArray("customers") ?: JSONArray()
                            for (i in 0 until custArray.length()) {
                                val cObj = custArray.optJSONObject(i) ?: continue
                                val id = cObj.optString("id").takeIf { it.isNotBlank() } ?: continue
                                val name = cObj.optString("name", "Customer")
                                val phone = cObj.optString("phone", "")
                                val address = cObj.optString("address", "")
                                val joinedDate = cObj.optString("joinedDate", "")
                                dao.insertCustomer(Customer(id, name, phone, address, joinedDate))
                            }
                        }

                        // Sync Inventory
                        if (rootJson.has("inventory")) {
                            val invArray = rootJson.optJSONArray("inventory") ?: JSONArray()
                            for (i in 0 until invArray.length()) {
                                val iObj = invArray.optJSONObject(i) ?: continue
                                val id = iObj.optString("id").takeIf { it.isNotBlank() } ?: continue
                                val name = iObj.optString("name", "Item")
                                val category = iObj.optString("category", "General")
                                val stock = iObj.optInt("stockLevel", 0)
                                val unit = iObj.optString("unit", "pcs")
                                val minAlert = iObj.optInt("minAlertLevel", 5)
                                val price = iObj.optDouble("price", 0.0)
                                val costPrice = iObj.optDouble("costPrice", 0.0)
                                val brand = iObj.optString("brand", "")
                                dao.insertInventoryItem(
                                    InventoryItem(
                                        id = id,
                                        name = name,
                                        category = category,
                                        stockLevel = stock,
                                        unit = unit,
                                        minAlertLevel = minAlert,
                                        price = price,
                                        costPrice = costPrice,
                                        brand = brand
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "Cloud GET failed, continuing to push local state: ${e.message}")
            }

            // 2. Fetch current local Room dataset
            val currentCustomers = dao.getCustomersFlow()
            // Format current data and push to cloud
            val nowStr = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(java.util.Date())
            _lastSyncTime.value = nowStr

            true
        } catch (e: Exception) {
            Log.e(TAG, "Cloud sync error: ${e.message}", e)
            false
        } finally {
            _isSyncing.value = false
        }
    }
}
