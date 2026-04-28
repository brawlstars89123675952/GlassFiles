package com.glassfiles.data.ai

import android.content.Context
import com.glassfiles.data.ai.models.AiProviderId

/**
 * Per-provider API-key storage in `SharedPreferences`.
 *
 * Keys are stored under [AiProviderId.keyPrefsKey], plus a few legacy keys for
 * proxy / region settings already used by [GeminiKeyStore]. New code should use
 * this object; [GeminiKeyStore] is kept as a thin compatibility shim so older
 * callers keep working until they migrate.
 */
object AiKeyStore {
    private const val PREFS = "gemini_prefs" // reuse legacy file so old keys survive an update
    private const val KEY_PROXY_GEMINI = "proxy_url"
    private const val KEY_QWEN_REGION = "qwen_region"

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun getKey(context: Context, provider: AiProviderId): String =
        prefs(context).getString(provider.keyPrefsKey, "").orEmpty().also {
            if (it.isBlank()) {
                // Migrate legacy keys: previous versions stored Gemini under "api_key"
                // and Qwen under "qwen_api_key". Promote them to the new namespace.
                val legacy = legacyLookup(context, provider)
                if (legacy.isNotBlank()) saveKey(context, provider, legacy)
            }
        }.ifBlank { legacyLookup(context, provider) }

    fun saveKey(context: Context, provider: AiProviderId, key: String) {
        prefs(context).edit().putString(provider.keyPrefsKey, key.trim()).apply()
    }

    fun clearKey(context: Context, provider: AiProviderId) {
        prefs(context).edit().remove(provider.keyPrefsKey).apply()
    }

    fun hasKey(context: Context, provider: AiProviderId): Boolean = getKey(context, provider).isNotBlank()

    fun configuredProviders(context: Context): List<AiProviderId> =
        AiProviderId.values().filter { hasKey(context, it) }

    /** Legacy proxy URL for Gemini (used by older builds). Optional. */
    fun getGeminiProxy(context: Context): String = prefs(context).getString(KEY_PROXY_GEMINI, "").orEmpty()
    fun saveGeminiProxy(context: Context, url: String) =
        prefs(context).edit().putString(KEY_PROXY_GEMINI, url.trim().trimEnd('/')).apply()

    /** Qwen region affects which DashScope endpoint is used. Defaults to "intl". */
    fun getQwenRegion(context: Context): String = prefs(context).getString(KEY_QWEN_REGION, "intl") ?: "intl"
    fun saveQwenRegion(context: Context, region: String) =
        prefs(context).edit().putString(KEY_QWEN_REGION, region).apply()

    private fun legacyLookup(context: Context, provider: AiProviderId): String {
        val p = prefs(context)
        return when (provider) {
            AiProviderId.GOOGLE -> p.getString("api_key", "").orEmpty()
            AiProviderId.ALIBABA -> p.getString("qwen_api_key", "").orEmpty()
            else -> ""
        }
    }
}
