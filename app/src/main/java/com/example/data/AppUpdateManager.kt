package com.example.data

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class AppVersionManifest(
    val versionCode: Int,
    val versionName: String,
    val apkUrl: String,
    val releaseNotes: String,
    val forceUpdate: Boolean = false,
    val publishedAt: String = ""
)

object AppUpdateManager {
    private const val TAG = "AppUpdateManager"
    private const val UPDATE_MANIFEST_URL = "https://gursimarpreetsingh-coder.github.io/relec-web-ui/version.json"

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(6, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    /**
     * Checks the live production update manifest on GitHub Pages.
     * Returns AppVersionManifest if a newer version is published, or null if up to date.
     */
    suspend fun checkForUpdates(context: Context): AppVersionManifest? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(UPDATE_MANIFEST_URL)
                .header("Cache-Control", "no-cache")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.w(TAG, "Update check HTTP failed with code: ${response.code}")
                    return@withContext null
                }
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@withContext null

                val json = JSONObject(body)
                val remoteVersionCode = json.optInt("versionCode", 0)
                val remoteVersionName = json.optString("versionName", "1.0")
                val apkUrl = json.optString("apkUrl", "")
                val releaseNotes = json.optString("releaseNotes", "Bug fixes and stability enhancements.")
                val forceUpdate = json.optBoolean("forceUpdate", false)
                val publishedAt = json.optString("publishedAt", "")

                val currentVersionCode = getInstalledVersionCode(context)
                Log.d(TAG, "Current installed versionCode=$currentVersionCode, remoteVersionCode=$remoteVersionCode")

                if (remoteVersionCode > currentVersionCode && apkUrl.isNotBlank()) {
                    return@withContext AppVersionManifest(
                        versionCode = remoteVersionCode,
                        versionName = remoteVersionName,
                        apkUrl = apkUrl,
                        releaseNotes = releaseNotes,
                        forceUpdate = forceUpdate,
                        publishedAt = publishedAt
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to check for app updates: ${e.message}")
        }
        return@withContext null
    }

    /**
     * Retrieves the running application's versionCode.
     */
    fun getInstalledVersionCode(context: Context): Int {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pInfo.longVersionCode.toInt()
            } else {
                @Suppress("DEPRECATION")
                pInfo.versionCode
            }
        } catch (e: Exception) {
            1
        }
    }

    /**
     * Retrieves the running application's versionName.
     */
    fun getInstalledVersionName(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
    }

    /**
     * Initiates the update installation.
     * Uses DownloadManager for system notification download and also opens the browser intent.
     */
    fun launchUpdateInstall(context: Context, apkUrl: String) {
        try {
            // 1. Enqueue download via Android DownloadManager
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (dm != null) {
                val request = DownloadManager.Request(Uri.parse(apkUrl))
                    .setTitle("ReLec / RE-VOLT ERP Update")
                    .setDescription("Downloading latest APK installer...")
                    .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "relec-update.apk")
                    .setMimeType("application/vnd.android.package-archive")

                dm.enqueue(request)
            }
        } catch (e: Exception) {
            Log.w(TAG, "DownloadManager enqueue skipped: ${e.message}")
        }

        // 2. Open Direct APK Link in Browser / Package Installer
        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(apkUrl)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(browserIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open APK URL: ${e.message}")
        }
    }
}
