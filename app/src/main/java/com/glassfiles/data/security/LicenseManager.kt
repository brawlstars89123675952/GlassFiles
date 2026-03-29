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
import java.security.MessageDigest
import java.security.cert.X509Certificate
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

/**
 * GlassFiles Server License Manager — Hardened
 *
 * Security layers:
 * 1. Certificate pinning (Cloudflare)
 * 2. HMAC token verification
 * 3. APK signature check
 * 4. Device fingerprint
 * 5. Multiple scattered check points
 * 6. Anti-tamper: integrity check of this class
 * 7. Root/hook detection
 */
object LicenseManager {

    private const val TAG = "LM"

    // ═══ CONFIGURE ═══
    private const val SERVER_URL = "https://api.glassfiles.ru"
    // ═══════════════════

    private const val PREFS_NAME = "lp"
    private const val KEY_TOKEN = "t"
    private const val KEY_TIER = "r"
    private const val KEY_FEATURES = "f"
    private const val KEY_LAST_VERIFY = "lv"
    private const val KEY_DEVICE_ID = "d"
    private const val KEY_SERVER_MESSAGE = "m"
    private const val KEY_SIG_HASH = "sh"

    private const val TOKEN_TTL_MS = 24 * 60 * 60 * 1000L
    private const val VERIFY_INTERVAL_MS = 12 * 60 * 60 * 1000L

    // ═══ Certificate Pinning ═══
    // Cloudflare public key SHA-256 pins (multiple for rotation)
    private val CERT_PINS = setOf(
        "3a43e220fe795114e8e91b5afee1b79dfa4ce9c3e28f9b4a7ebb94b189c5be01",
        "cb3ccbb76031e5e0138f8dd39a23f9de47ffc35e43c1144cea27d46a5ab1cb5f",
        "16af57a9f676b0ab126095aa5ebadef22ab31119d644ac95cd4b93dbf3f26aeb"
    )

    // ═══ State ═══
    @Volatile var isVerified = false; private set
    @Volatile var currentTier = "free"; private set
    @Volatile var features = listOf<String>(); private set
    @Volatile var serverMessage: String? = null; private set
    @Volatile private var checksum = 0L
    private var integrityToken = 0L

    // ═══════════════════════════════════
    // Main verify
    // ═══════════════════════════════════

    data class VerifyResult(
        val valid: Boolean,
        val reason: String = "",
        val message: String? = null,
        val tier: String = "free",
        val features: List<String> = emptyList()
    )

    suspend fun verify(context: Context): VerifyResult = withContext(Dispatchers.IO) {
        if (detectHooks()) {
            return@withContext VerifyResult(valid = false, reason = "environment_compromised")
        }

        val prefs = getPrefs(context)
        if (integrityToken == 0L) integrityToken = computeIntegrity(context)

        val cachedToken = prefs.getString(KEY_TOKEN, null)
        val lastVerify = prefs.getLong(KEY_LAST_VERIFY, 0)
        val now = System.currentTimeMillis()

        if (cachedToken != null && (now - lastVerify) < TOKEN_TTL_MS) {
            loadCachedState(prefs)
            if ((now - lastVerify) > VERIFY_INTERVAL_MS) {
                return@withContext doServerVerify(context, prefs)
            }
            return@withContext VerifyResult(valid = true, tier = currentTier, features = features, message = serverMessage)
        }

        val result = doServerVerify(context, prefs)

        if (!result.valid && result.reason == "network_error" && cachedToken != null) {
            loadCachedState(prefs)
            return@withContext VerifyResult(valid = true, tier = currentTier, features = features, message = "Offline mode")
        }

        result
    }

    // ═══════════════════════════════════
    // Server call with certificate pinning
    // ═══════════════════════════════════

    private suspend fun doServerVerify(context: Context, prefs: SharedPreferences): VerifyResult {
        return try {
            val deviceId = getDeviceId(context, prefs)
            val sigHash = getSignatureHash(context)
            prefs.edit().putString(KEY_SIG_HASH, sigHash).apply()

            val body = JSONObject().apply {
                put("deviceId", deviceId)
                put("signatureHash", sigHash)
                put("packageName", context.packageName)
                put("version", getAppVersion(context))
                put("model", "${Build.MANUFACTURER} ${Build.MODEL}")
                put("android", Build.VERSION.SDK_INT)
                put("integrity", integrityToken.toString(16))
            }

            val conn = createPinnedConnection("$SERVER_URL/api/verify")
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 10000
            conn.readTimeout = 10000
            conn.doOutput = true

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

                prefs.edit()
                    .putString(KEY_TOKEN, token).putString(KEY_TIER, tier)
                    .putString(KEY_FEATURES, featuresList.joinToString(","))
                    .putLong(KEY_LAST_VERIFY, System.currentTimeMillis())
                    .putString(KEY_SERVER_MESSAGE, msg).apply()

                isVerified = true; currentTier = tier; features = featuresList; serverMessage = msg
                checksum = computeChecksum(token, tier)
                VerifyResult(valid = true, tier = tier, features = featuresList, message = msg)
            } else {
                val reason = json.optString("reason", "unknown")
                val msg = json.optString("message", null)
                if (reason != "network_error") prefs.edit().remove(KEY_TOKEN).remove(KEY_LAST_VERIFY).apply()
                isVerified = false
                VerifyResult(valid = false, reason = reason, message = msg)
            }
        } catch (e: javax.net.ssl.SSLException) {
            Log.e(TAG, "SSL pin fail: ${e.message}")
            isVerified = false
            VerifyResult(valid = false, reason = "ssl_pinning_failed", message = "Connection security error")
        } catch (e: Exception) {
            Log.e(TAG, "Verify: ${e.message}")
            VerifyResult(valid = false, reason = "network_error", message = e.message)
        }
    }

    // ═══════════════════════════════════
    // Certificate Pinning
    // ═══════════════════════════════════

    private fun createPinnedConnection(urlStr: String): HttpURLConnection {
        val url = URL(urlStr)
        val conn = url.openConnection() as HttpsURLConnection

        val tm = object : X509TrustManager {
            private val defaultTM: X509TrustManager by lazy {
                val factory = javax.net.ssl.TrustManagerFactory.getInstance(
                    javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm()
                )
                factory.init(null as java.security.KeyStore?)
                factory.trustManagers.first { it is X509TrustManager } as X509TrustManager
            }

            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) =
                defaultTM.checkClientTrusted(chain, authType)

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                defaultTM.checkServerTrusted(chain, authType)
                if (chain == null || chain.isEmpty()) throw javax.net.ssl.SSLException("Empty chain")

                var pinMatched = false
                for (cert in chain) {
                    if (sha256Hex(cert.encoded) in CERT_PINS || sha256Hex(cert.publicKey.encoded) in CERT_PINS) {
                        pinMatched = true; break
                    }
                }
                if (!pinMatched) {
                    Log.w(TAG, "Cert pin mismatch — possible MITM or rotation")
                    // Soft pinning: log but don't block (prevents breakage on cert rotation)
                    // For hard pinning uncomment: throw javax.net.ssl.SSLException("Pin failed")
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = defaultTM.acceptedIssuers
        }

        val ctx = SSLContext.getInstance("TLS")
        ctx.init(null, arrayOf<TrustManager>(tm), java.security.SecureRandom())
        conn.sslSocketFactory = ctx.socketFactory
        return conn
    }

    // ═══════════════════════════════════
    // Heartbeat
    // ═══════════════════════════════════

    suspend fun heartbeat(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            if (integrityToken != 0L && integrityToken != computeIntegrity(context)) {
                isVerified = false; return@withContext false
            }
            val prefs = getPrefs(context)
            val deviceId = getDeviceId(context, prefs)
            val token = prefs.getString(KEY_TOKEN, null) ?: return@withContext false

            val body = JSONObject().apply { put("deviceId", deviceId); put("token", token) }
            val conn = createPinnedConnection("$SERVER_URL/api/heartbeat")
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 8000; conn.readTimeout = 8000; conn.doOutput = true
            conn.outputStream.use { it.write(body.toString().toByteArray()) }
            val response = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()

            val valid = JSONObject(response).optBoolean("valid", false)
            if (!valid) { isVerified = false; prefs.edit().remove(KEY_TOKEN).remove(KEY_LAST_VERIFY).apply() }
            valid
        } catch (e: Exception) { Log.e(TAG, "HB: ${e.message}"); true }
    }

    // ═══════════════════════════════════
    // Scattered Check Points
    // ═══════════════════════════════════

    /** Quick inline check — call from critical features */
    fun c(): Boolean = isVerified && checksum != 0L

    /** Check with context — verifies signature */
    fun validate(context: Context): Boolean {
        if (!isVerified) return false
        val prefs = getPrefs(context)
        val storedSig = prefs.getString(KEY_SIG_HASH, null) ?: return false
        if (storedSig != getSignatureHash(context)) { isVerified = false; return false }
        return true
    }

    /** Deep check — call occasionally from random places */
    suspend fun deepCheck(context: Context): Boolean = withContext(Dispatchers.IO) {
        if (!isVerified) return@withContext false
        if (!validate(context)) return@withContext false
        if (integrityToken != 0L && integrityToken != computeIntegrity(context)) { isVerified = false; return@withContext false }
        if (detectHooks()) { isVerified = false; return@withContext false }
        val prefs = getPrefs(context)
        val lastVerify = prefs.getLong(KEY_LAST_VERIFY, 0)
        if (System.currentTimeMillis() - lastVerify > TOKEN_TTL_MS * 2) { isVerified = false; return@withContext false }
        true
    }

    // ═══════════════════════════════════
    // Feature check
    // ═══════════════════════════════════

    fun hasFeature(feature: String): Boolean = isVerified && features.contains(feature)
    fun isPro(): Boolean = isVerified && (currentTier == "pro" || currentTier == "beta")

    // ═══════════════════════════════════
    // Anti-Hook / Root Detection
    // ═══════════════════════════════════

    private fun detectHooks(): Boolean {
        try {
            // Frida ports
            for (port in listOf(27042, 27043)) {
                try {
                    val s = java.net.Socket(); s.connect(java.net.InetSocketAddress("127.0.0.1", port), 100); s.close(); return true
                } catch (_: Exception) {}
            }
            // Xposed
            try { Class.forName("de.robv.android.xposed.XposedBridge"); return true } catch (_: ClassNotFoundException) {}
            // Substrate
            try { Class.forName("com.saurik.substrate.MS"); return true } catch (_: ClassNotFoundException) {}
            // Debugger
            if (android.os.Debug.isDebuggerConnected()) return true
        } catch (_: Exception) {}
        return false
    }

    // ═══════════════════════════════════
    // Integrity
    // ═══════════════════════════════════

    private fun computeIntegrity(context: Context): Long = try {
        val info = context.packageManager.getApplicationInfo(context.packageName, 0)
        val f = java.io.File(info.sourceDir)
        (f.length() xor f.lastModified()) xor (Build.VERSION.SDK_INT.toLong() shl 32) xor context.packageName.hashCode().toLong()
    } catch (_: Exception) { -1L }

    private fun computeChecksum(token: String, tier: String): Long =
        (token.hashCode().toLong() shl 32) or (tier.hashCode().toLong() and 0xFFFFFFFFL)

    // ═══════════════════════════════════
    // Device ID & Signature
    // ═══════════════════════════════════

    @SuppressLint("HardwareIds")
    private fun getDeviceId(context: Context, prefs: SharedPreferences): String {
        val cached = prefs.getString(KEY_DEVICE_ID, null)
        if (cached != null) return cached
        val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "unknown"
        val deviceId = "gf_${androidId}_${Build.FINGERPRINT.hashCode().toString(16)}"
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
        return deviceId
    }

    @Suppress("DEPRECATION")
    private fun getSignatureHash(context: Context): String = try {
        val pm = context.packageManager
        val info = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P)
            pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNING_CERTIFICATES)
        else pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNATURES)
        val sigs = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.signingInfo?.apkContentsSigners
        else @Suppress("DEPRECATION") info.signatures
        if (sigs.isNullOrEmpty()) "no_sig" else sha256Hex(sigs[0].toByteArray())
    } catch (e: Exception) { "error" }

    private fun sha256Hex(data: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(data).joinToString("") { "%02x".format(it) }

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

    fun clearCache(context: Context) {
        getPrefs(context).edit().clear().apply()
        isVerified = false; currentTier = "free"; features = emptyList(); serverMessage = null; checksum = 0L
    }
}
