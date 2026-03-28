package com.glassfiles.data.security

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * GlassFiles Server License Manager
 *
 * Handles: device verification, token caching, kill-switch,
 * feature gating, and periodic heartbeat.
 *
 * Usage:
 *   // In Application.onCreate() or MainActivity:
 *   val result = LicenseManager.verify(context)
 *   if (!result.valid) { showBlockedScreen(result.reason, result.message) }
 *
 *   // Check features:
 *   if (LicenseManager.hasFeature("github")) { ... }
 *
 *   // Periodic heartbeat (call every 4-6 hours):
 *   LicenseManager.heartbeat(context)
 */
object LicenseManager {

    private const val TAG = "LicenseManager"

    // ═══ CONFIGURE THIS ═══
    private const val SERVER_URL = "https://glassfiles-license.brawlstars89123675952.workers.dev"
    // ═══════════════════════

    private const val PREFS_NAME = "license_prefs"
    private const val KEY_TOKEN = "cached_token"
    private const val KEY_TIER = "cached_tier"
    private const val KEY_FEATURES = "cached_features"
    private const val KEY_LAST_VERIFY = "last_verify_time"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_SERVER_MESSAGE = "server_message"

    private const val TOKEN_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours
    private const val VERIFY_INTERVAL_MS = 12 * 60 * 60 * 1000L // Re-verify every 12h

    // Cached state (in-memory)
    var isVerified = false
        private set
    var currentTier = "free"
        private set
    var features = listOf<String>()
        private set
    var serverMessage: String? = null
        private set

    // ═══════════════════════════════════
    // Main verify — call on app start
    // ═══════════════════════════════════

    data class VerifyResult(
        val valid: Boolean,
        val reason: String = "",
        val message: String? = null,
        val tier: String = "free",
        val features: List<String> = emptyList()
    )

    suspend fun verify(context: Context): VerifyResult = withContext(Dispatchers.IO) {
        val prefs = getPrefs(context)

        // 1. Check if we have a valid cached token
        val cachedToken = prefs.getString(KEY_TOKEN, null)
        val lastVerify = prefs.getLong(KEY_LAST_VERIFY, 0)
        val now = System.currentTimeMillis()

        // If cached token exists and not too old, try offline
        if (cachedToken != null && (now - lastVerify) < TOKEN_TTL_MS) {
            loadCachedState(prefs)
            Log.d(TAG, "Using cached token, tier=$currentTier")

            // If enough time passed, re-verify in background (non-blocking)
            if ((now - lastVerify) > VERIFY_INTERVAL_MS) {
                Log.d(TAG, "Cache valid but stale, re-verifying...")
                return@withContext doServerVerify(context, prefs)
            }

            return@withContext VerifyResult(
                valid = true, tier = currentTier,
                features = features, message = serverMessage
            )
        }

        // 2. No valid cache — must verify with server
        val result = doServerVerify(context, prefs)

        // 3. If server unreachable but we have an old token, allow gracefully
        if (!result.valid && result.reason == "network_error" && cachedToken != null) {
            loadCachedState(prefs)
            Log.w(TAG, "Server unreachable, using expired cache (grace period)")
            return@withContext VerifyResult(
                valid = true, tier = currentTier,
                features = features, message = "Offline mode"
            )
        }

        result
    }

    // ═══════════════════════════════════
    // Server verification
    // ═══════════════════════════════════

    private suspend fun doServerVerify(context: Context, prefs: SharedPreferences): VerifyResult {
        return try {
            val deviceId = getDeviceId(context, prefs)
            val sigHash = getSignatureHash(context)

            val body = JSONObject().apply {
                put("deviceId", deviceId)
                put("signatureHash", sigHash)
                put("packageName", context.packageName)
                put("version", getAppVersion(context))
                put("model", "${Build.MANUFACTURER} ${Build.MODEL}")
                put("android", Build.VERSION.SDK_INT)
            }

            val conn = (URL("$SERVER_URL/api/verify").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 10000
                readTimeout = 10000
                doOutput = true
            }

            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val code = conn.responseCode
            val response = (if (code in 200..299) conn.inputStream else conn.errorStream)
                .bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(response)
            val valid = json.optBoolean("valid", false)

            if (valid) {
                val token = json.optString("token", "")
                val tier = json.optString("tier", "free")
                val featuresArr = json.optJSONArray("features") ?: JSONArray()
                val featuresList = (0 until featuresArr.length()).map { featuresArr.getString(it) }
                val msg = json.optString("message", null)

                // Cache
                prefs.edit()
                    .putString(KEY_TOKEN, token)
                    .putString(KEY_TIER, tier)
                    .putString(KEY_FEATURES, featuresList.joinToString(","))
                    .putLong(KEY_LAST_VERIFY, System.currentTimeMillis())
                    .putString(KEY_SERVER_MESSAGE, msg)
                    .apply()

                isVerified = true
                currentTier = tier
                features = featuresList
                serverMessage = msg

                VerifyResult(valid = true, tier = tier, features = featuresList, message = msg)
            } else {
                val reason = json.optString("reason", "unknown")
                val msg = json.optString("message", null)

                // Clear cache on explicit rejection
                if (reason != "network_error") {
                    prefs.edit().remove(KEY_TOKEN).remove(KEY_LAST_VERIFY).apply()
                }

                isVerified = false
                VerifyResult(valid = false, reason = reason, message = msg)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Verify failed: ${e.message}")
            VerifyResult(valid = false, reason = "network_error", message = e.message)
        }
    }

    // ═══════════════════════════════════
    // Heartbeat — lightweight periodic check
    // ═══════════════════════════════════

    suspend fun heartbeat(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val prefs = getPrefs(context)
            val deviceId = getDeviceId(context, prefs)
            val token = prefs.getString(KEY_TOKEN, null) ?: return@withContext false

            val body = JSONObject().apply {
                put("deviceId", deviceId)
                put("token", token)
            }

            val conn = (URL("$SERVER_URL/api/heartbeat").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                setRequestProperty("Content-Type", "application/json")
                connectTimeout = 8000; readTimeout = 8000; doOutput = true
            }
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val json = JSONObject(response)
            val valid = json.optBoolean("valid", false)

            if (!valid) {
                isVerified = false
                prefs.edit().remove(KEY_TOKEN).remove(KEY_LAST_VERIFY).apply()
            }

            valid
        } catch (e: Exception) {
            Log.e(TAG, "Heartbeat failed: ${e.message}")
            true // Don't kill app on network errors
        }
    }

    // ═══════════════════════════════════
    // Feature check
    // ═══════════════════════════════════

    fun hasFeature(feature: String): Boolean = features.contains(feature)

    fun isPro(): Boolean = currentTier == "pro" || currentTier == "beta"

    // ═══════════════════════════════════
    // Device ID — persistent, unique
    // ═══════════════════════════════════

    @SuppressLint("HardwareIds")
    private fun getDeviceId(context: Context, prefs: SharedPreferences): String {
        // Try cached first
        val cached = prefs.getString(KEY_DEVICE_ID, null)
        if (cached != null) return cached

        // Generate from Android ID + some entropy
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        val deviceId = "gf_${androidId}_${Build.FINGERPRINT.hashCode().toString(16)}"

        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        return deviceId
    }

    // ═══════════════════════════════════
    // APK Signature Hash
    // ═══════════════════════════════════

    @Suppress("DEPRECATION")
    private fun getSignatureHash(context: Context): String {
        return try {
            val pm = context.packageManager
            val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
            } else {
                pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNATURES)
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                info.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION") info.signatures
            }

            if (signatures.isNullOrEmpty()) return "no_sig"

            val md = java.security.MessageDigest.getInstance("SHA-256")
            val hash = md.digest(signatures[0].toByteArray())
            hash.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "Sig hash error: ${e.message}")
            "error"
        }
    }

    // ═══════════════════════════════════
    // Helpers
    // ═══════════════════════════════════

    private fun getPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private fun getAppVersion(context: Context): String = try {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
    } catch (_: Exception) { "unknown" }

    private fun loadCachedState(prefs: SharedPreferences) {
        isVerified = true
        currentTier = prefs.getString(KEY_TIER, "free") ?: "free"
        features = (prefs.getString(KEY_FEATURES, "") ?: "").split(",").filter { it.isNotBlank() }
        serverMessage = prefs.getString(KEY_SERVER_MESSAGE, null)
    }

    /**
     * Clear all cached data — use on logout or for testing
     */
    fun clearCache(context: Context) {
        getPrefs(context).edit().clear().apply()
        isVerified = false
        currentTier = "free"
        features = emptyList()
        serverMessage = null
    }
}
